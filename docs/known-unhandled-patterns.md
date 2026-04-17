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

```java
package org.apache.nifi.processors.hadoop;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processors.hadoop.util.FilterMode;

import static org.apache.nifi.processors.hadoop.util.FilterMode.FILTER_DIRECTORIES_AND_FILES;

public class ListHDFS extends AbstractHadoopProcessor {

    // Read back by FilterMode enum constant initializers (FilterMode.java, a separate file).
    public static final PropertyDescriptor RECURSE_SUBDIRS = new PropertyDescriptor.Builder()
            .name("Recurse Subdirectories").build();

    // Read back by FilterMode enum constant initializers (FilterMode.java, a separate file).
    public static final PropertyDescriptor FILE_FILTER = new PropertyDescriptor.Builder()
            .name("File Filter").build();

    // Triggers FilterMode.<clinit> via allowableValues(FilterMode.class).
    // FilterMode's constants call RECURSE_SUBDIRS.getDisplayName() and FILE_FILTER.getDisplayName().
    public static final PropertyDescriptor FILE_FILTER_MODE = new PropertyDescriptor.Builder()
            .name("File Filter Mode")
            .allowableValues(FilterMode.class)
            .defaultValue(FILTER_DIRECTORIES_AND_FILES.getValue())
            .build();
}
```

```java
package org.apache.nifi.processors.hadoop.util;

import org.apache.nifi.components.DescribedValue;

import static org.apache.nifi.processors.hadoop.ListHDFS.FILE_FILTER;
import static org.apache.nifi.processors.hadoop.ListHDFS.RECURSE_SUBDIRS;

public enum FilterMode implements DescribedValue {

    // Each constant reads back into ListHDFS.RECURSE_SUBDIRS and ListHDFS.FILE_FILTER.
    FILTER_DIRECTORIES_AND_FILES("filter-mode-directories-and-files", "Directories and Files",
            "If " + RECURSE_SUBDIRS.getDisplayName() + " is true, search dirs matching "
                    + FILE_FILTER.getDisplayName() + "."),
    FILTER_MODE_FILES_ONLY("filter-mode-files-only", "Files Only",
            "If " + RECURSE_SUBDIRS.getDisplayName() + " is true, search all dirs for files matching "
                    + FILE_FILTER.getDisplayName() + "."),
    FILTER_MODE_FULL_PATH("filter-mode-full-path", "Full Path",
            "Match " + FILE_FILTER.getDisplayName() + " against the full path. If "
                    + RECURSE_SUBDIRS.getDisplayName() + " is true, search entire tree.");

    private final String value;
    private final String displayName;
    private final String description;

    FilterMode(String value, String displayName, String description) {
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

## Hidden static-field dependency via method body

### What it is

A static field `F` is initialized by calling a private static method `M()` that reads another
static field `G` of the same class in its body. From the perspective of the field declaration,
`F`'s initializer is just `M()` — `G` never appears in the initializer expression itself, so the
`F → G` ordering dependency is invisible to any tool that analyzes initializer expressions at the
source level.

### Why it is broken code

The Java Language Specification (JLS §12.4.2) specifies that static-field initializers are
processed strictly in textual order. If `F` is declared before `G` and `M()` reads `G`, the JVM
assigns `F` before `G` has been set — causing `M()` to read `G`'s default value (`null` for
objects, `0` for primitives). The bug is silent: swapping the declaration order, or inserting a
new field between them, can break initialization without any compile-time warning.

The dependency exists; it is simply hidden. Code structured this way is misleading — readers
scanning field declarations see `F = M()` with no indication that the ordering of `F` and `G`
matters at all. The correct fix is to pass `G` as an explicit argument to `M`, making the
dependency visible in the initializer expression and allowing both humans and tools to reason about
ordering correctly.

### Illustrative example

The code below was stripped from the Apache NiFi project (cluster protocol module) to show only the
affected declarations.

```java
// Broken: the dependency on JAXB_CONTEXT_PATH is hidden inside the method body.
public final class JaxbProtocolUtils {

    public static final String JAXB_CONTEXT_PATH = ObjectFactory.class.getPackage().getName();

    public static final JAXBContext JAXB_CONTEXT = initializeJaxbContext();

    private static JAXBContext initializeJaxbContext() {
        try {
            return JAXBContext.newInstance(JAXB_CONTEXT_PATH); // reads JAXB_CONTEXT_PATH invisibly
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.", e);
        }
    }
}
```

**Correct form** — dependency made explicit as a parameter:

```java
// Fixed: JAXB_CONTEXT_PATH appears directly in the initializer expression.
public final class JaxbProtocolUtils {

    public static final String JAXB_CONTEXT_PATH = ObjectFactory.class.getPackage().getName();

    public static final JAXBContext JAXB_CONTEXT = initializeJaxbContext(JAXB_CONTEXT_PATH);

    private static JAXBContext initializeJaxbContext(String jaxbContextPath) {
        try {
            return JAXBContext.newInstance(jaxbContextPath);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXBContext.", e);
        }
    }
}
```

### Why JHarmonizer does not support this

Resolving hidden method-body dependencies would require JHarmonizer to:

1. Follow every static-initializer method call into its body and discover which fields are read
   there.
2. Repeat this recursively: `M()` may call `N()`, which may call `P()`, potentially crossing class
   and file boundaries.
3. Handle arbitrary call graphs that may include conditional branches, loops, or mutually recursive
   calls — all without classpath resolution, since the tool operates in `noClasspath` mode.

This analysis is complex, fragile, and would only benefit code that should not exist in the first
place. There is no motivation to implement it. The only correct fix is to **make the dependency
explicit** — pass the required field as a method parameter so that the initializer expression
contains all the information needed to determine ordering.

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
