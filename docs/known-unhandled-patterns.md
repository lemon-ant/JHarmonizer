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
initializes in return.

### Illustrative example

The code below was stripped from a real-world multi-module Java project to show only the
affected declarations. Original class names, constant names, and package structure are preserved
so the pattern can be traced back to its source.

**`OptionsPanel.java`** (simplified):
```java
package com.example.filebrowser.ui;

class OptionsPanel {
    // These two labels are read by FilterMode during its own static initialization.
    static final String RECURSE_LABEL = new String("Recurse Subdirectories");
    static final String FILTER_LABEL  = new String("File Filter");

    // This field triggers FilterMode's <clinit>, which reads FILTER_LABEL and RECURSE_LABEL.
    static final String MODE_VALUE = FilterMode.FILTER_ALL.getValue();
}
```

**`FilterMode.java`** (in a separate file):
```java
package com.example.filebrowser.ui;

enum FilterMode {
    // The enum constant initializer reads back into OptionsPanel.
    FILTER_ALL(
        OptionsPanel.FILTER_LABEL + "/" + OptionsPanel.RECURSE_LABEL
    );

    private final String description;

    FilterMode(String description) { this.description = description; }

    String getValue() { return description; }
}
```

**What happens at runtime** depends entirely on which class the JVM loads first:

- If `OptionsPanel` is loaded first, its `<clinit>` assigns `RECURSE_LABEL` and `FILTER_LABEL`,
  then tries to initialize `MODE_VALUE`, which triggers `FilterMode.<clinit>`. The enum constant
  reads `FILTER_LABEL` and `RECURSE_LABEL` — both already set — so `MODE_VALUE` gets the correct
  concatenated string. Works by accident.
- If `FilterMode` is loaded first (e.g. via a different call site), its `<clinit>` initializes
  `FILTER_ALL`, which reads `OptionsPanel.FILTER_LABEL` and `RECURSE_LABEL`. Because
  `OptionsPanel.<clinit>` has not run yet, both fields are `null`. `FILTER_ALL` is initialized
  with `"null/null"`, and all downstream consumers see the wrong value permanently.

### Why JHarmonizer does not support this

Handling cross-file circular initializer cycles would require JHarmonizer to:

1. Detect dependency cycles that span separate compilation units (files the tool may or may not
   be processing together).
2. Decide how to order types across files — a problem with no safe general solution when a true
   cycle exists.
3. Add complexity to the dependency graph analysis that would only benefit code that should not
   exist in the first place.

There is no motivation to implement this. The only correct fix is to **break the circular
dependency** in the source code — for instance, by extracting the shared constants into a
dedicated `OptionsPanelConstants` class that neither `OptionsPanel` nor `FilterMode` reads back
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
