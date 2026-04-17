# Known Unsupported Patterns

This document lists code patterns that JHarmonizer intentionally does not handle.
These are not implementation gaps to be closed — they represent design problems in the
source code itself that must be fixed by the developer, not worked around by a tool.

---

## Cross-file circular static initializer dependencies

### What it is

Two or more Java types spread across **separate source files** form a cycle through their
static-field initializers: type `A` (in `A.java`) has a static field whose initializer reads a
static field of type `B` (in `B.java`), and `B`'s field initializer in turn reads a static field
of `A`. The cycle may span more than two types.

### Why it is broken code

The Java Language Specification (JLS §12.4.1–12.4.2) defines class initialization order based on
the order of class loading. When two types in different files depend on each other's static fields
during initialization, the JVM's re-entrant `<clinit>` guard kicks in: whichever type is loaded
second will receive zero/`null` values for any of the first type's fields that have not yet been
assigned. The result is silent data corruption, an `ExceptionInInitializerError`, or
`NullPointerException` — all highly dependent on class-loading order, which is not
guaranteed to be stable across JVM runs or build systems.

Code like this **must not go to production**. The circular dependency needs to be broken by
extracting the shared constants into a dedicated third class that neither cycle participant
initializes in turn.

### Illustrative example

The code below was stripped from the Apache NiFi project (HDFS processors module) to show only
the affected declarations. Original class names, field names, and package structure are preserved
exactly so the pattern can be traced back to its source.

**`ListHDFS.java`** (simplified, package `org.apache.nifi.processors.hadoop`):
```java
package org.apache.nifi.processors.hadoop;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processors.hadoop.util.FilterMode;

import static org.apache.nifi.processors.hadoop.util.FilterMode.FILTER_DIRECTORIES_AND_FILES;

public class ListHDFS extends AbstractHadoopProcessor {

    // RECURSE_SUBDIRS.getDisplayName() and FILE_FILTER.getDisplayName() are read back
    // by FilterMode's enum constant initializers in FilterMode.java (a separate file).
    public static final PropertyDescriptor RECURSE_SUBDIRS = new PropertyDescriptor.Builder()
            .name("Recurse Subdirectories")
            .description("Indicates whether to list files from subdirectories of the HDFS directory")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("true")
            .build();

    public static final PropertyDescriptor FILE_FILTER = new PropertyDescriptor.Builder()
            .name("File Filter")
            .description("Only files whose names match the given regular expression will be picked up")
            .required(true)
            .defaultValue("[^\\.].*")
            .build();

    // This field triggers FilterMode.<clinit> via allowableValues(FilterMode.class).
    // FilterMode's enum constants read back RECURSE_SUBDIRS and FILE_FILTER above.
    public static final PropertyDescriptor FILE_FILTER_MODE = new PropertyDescriptor.Builder()
            .name("File Filter Mode")
            .description("Determines how the regular expression in "
                    + FILE_FILTER.getDisplayName() + " will be used when retrieving listings.")
            .required(true)
            .allowableValues(FilterMode.class)
            .defaultValue(FILTER_DIRECTORIES_AND_FILES.getValue())
            .build();
}
```

**`FilterMode.java`** (separate file, package `org.apache.nifi.processors.hadoop.util`):
```java
package org.apache.nifi.processors.hadoop.util;

import org.apache.nifi.components.DescribedValue;

import static org.apache.nifi.processors.hadoop.ListHDFS.FILE_FILTER;
import static org.apache.nifi.processors.hadoop.ListHDFS.RECURSE_SUBDIRS;

public enum FilterMode implements DescribedValue {

    // Each enum constant initializer reads back into ListHDFS via the static imports above.
    FILTER_DIRECTORIES_AND_FILES(
            "filter-mode-directories-and-files",
            "Directories and Files",
            "Filtering will be applied to the names of directories and files. If "
                    + RECURSE_SUBDIRS.getDisplayName()
                    + " is set to true, only subdirectories with a matching name will be searched "
                    + "for files that match the regular expression defined in "
                    + FILE_FILTER.getDisplayName() + "."
    ),
    FILTER_MODE_FILES_ONLY(
            "filter-mode-files-only",
            "Files Only",
            "Filtering will only be applied to the names of files. If "
                    + RECURSE_SUBDIRS.getDisplayName()
                    + " is set to true, the entire subdirectory tree will be searched for files "
                    + "that match the regular expression defined in "
                    + FILE_FILTER.getDisplayName() + "."
    ),
    FILTER_MODE_FULL_PATH(
            "filter-mode-full-path",
            "Full Path",
            "Filtering will be applied by evaluating the regular expression defined in "
                    + FILE_FILTER.getDisplayName()
                    + " against the full path of files. If " + RECURSE_SUBDIRS.getDisplayName()
                    + " is set to true, the entire subdirectory tree will be searched."
    );

    private final String value;
    private final String displayName;
    private final String description;

    FilterMode(final String value, final String displayName, final String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    @Override public String getValue() { return value; }
    @Override public String getDisplayName() { return displayName; }
    @Override public String getDescription() { return description; }
}
```

**What happens at runtime** depends entirely on which class the JVM loads first:

- If `ListHDFS` is loaded first, its `<clinit>` assigns `RECURSE_SUBDIRS` and `FILE_FILTER`,
  then initializes `FILE_FILTER_MODE`, which triggers `FilterMode.<clinit>`. The enum constants
  read `RECURSE_SUBDIRS.getDisplayName()` and `FILE_FILTER.getDisplayName()` — both already
  set — so all descriptions are built correctly. Works by accident.
- If `FilterMode` is loaded first (e.g. from a different call site), its `<clinit>` starts
  initializing the enum constants, which read `ListHDFS.RECURSE_SUBDIRS` and `ListHDFS.FILE_FILTER`.
  This triggers `ListHDFS.<clinit>`. During `ListHDFS` initialization, `FILE_FILTER_MODE` is
  built with `allowableValues(FilterMode.class)`, which tries to call `FilterMode.values()`.
  Because `FilterMode.<clinit>` is already running (re-entrant guard), the JVM returns the
  partially initialized `FilterMode` class — enum constants not yet assigned. The result depends
  on JVM internals but may be `NullPointerException`, `ExceptionInInitializerError`, or silently
  wrong enum descriptions that are baked in permanently.

### Why JHarmonizer does not support this

Handling cross-file circular initializer cycles would require JHarmonizer to:

1. Detect dependency cycles that span separate compilation units (files the tool may or may not
   be processing together).
2. Decide how to order types across files — a problem with no safe general solution when a true
   cycle exists.
3. Add complexity to the dependency graph analysis that would only benefit code that should not
   exist in the first place.

There is no motivation to implement this. The only correct fix is to **break the circular
dependency** in the source code — for instance, by extracting the shared display-name strings
into a dedicated `ListHDFSConstants` class that neither `ListHDFS` nor `FilterMode` reads back
from.

---

## What JHarmonizer does handle: single-file multi-type cycles

When all participating types live in the **same `.java` file** (same compilation unit),
JHarmonizer's `CrossTypeConstantBackRefDependencyProvider` detects these cross-type back-reference
chains via a depth-first traversal of static-field initializer expressions, and adds the necessary
dependency edges to the ordering graph.

This includes:

- A direct two-type back-reference between a class and a sibling enum constant in the same file.
- A chain of arbitrary depth: `T.F → E1.SF1 → E2.SF2 → … → T.G`, as long as all intermediate
  types appear in the same compilation unit.
- Nested types: an outer class depending on a nested enum whose constant initializer reads a nested
  class that back-references the outer class (a three-node cycle entirely within one file).

**Example — three-node nested cycle handled correctly** (see regression fixture
`08-enum-constant-cross-type-back-reference/input/NestedMultiNodeCycleRegressionSample.java`):

```java
class NestedMultiNodeCycleRegressionSample {
    // Input has B_VALUE before A_VALUE (wrong alphabetical order).
    // JHarmonizer reorders to A_VALUE, B_VALUE, Z_RESULT.
    static final String B_VALUE = new String("B");
    static final String A_VALUE = new String("A");
    static final String Z_RESULT = Mode.ENTRY.getValue();
    // ...

    // Intermediate node 1: nested enum reads nested class.
    enum Mode {
        ENTRY(Constants.COMBINED);
        // ...
    }

    // Intermediate node 2: nested class reads back outer class fields.
    static class Constants {
        static final String COMBINED = new String(
                NestedMultiNodeCycleRegressionSample.A_VALUE + "+"
                        + NestedMultiNodeCycleRegressionSample.B_VALUE);
    }
}
```

The cycle `Z_RESULT → Mode.ENTRY → Constants.COMBINED → A_VALUE, B_VALUE` is detected entirely
within the single file, and the reordering to `A_VALUE, B_VALUE, Z_RESULT` is applied safely.

> **Note:** even though JHarmonizer handles this single-file variant, the underlying code pattern
> is still a design smell. Circular static-initializer dependencies — even within one file — signal
> that types are more tightly coupled than they should be. Refactoring to eliminate the cycle is
> always preferable to relying on the tool to order around it.
