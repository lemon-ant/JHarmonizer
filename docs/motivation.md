<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Why JHarmonizer?

Java teams spend real time arguing about class member layout in code reviews: should constants come before
fields, should public methods be grouped at the top, should getters and setters be kept together?
The answer changes per project, per team, and per reviewer. JHarmonizer eliminates that argument by
making the layout **automatic and enforced consistently in CI**.

## Why existing tools are not enough

### IntelliJ IDEA / Eclipse arrangement rules

Both IDEs can reorder class members. The problem: they work only when a developer manually triggers the
action inside the IDE. CI cannot run an IDE. A developer who forgets to reorder before pushing, or who
uses VS Code or another editor, produces non-conforming code that slips through undetected. Configuration
is also locked inside the IDE workspace and is not portable.

### Checkstyle / PMD member-order checks

These tools can report that a class violates a defined member order, but they **do not fix it**. They flag
the problem; a developer still has to manually reorder members and re-run the check. In large classes this
is tedious and error-prone.

### Spotless / Google Java Format / Palantir Java Format

Excellent at whitespace formatting — indentation, blank lines, import ordering. They do **not** reorder
class members by kind or visibility. Running Spotless still leaves constants below methods, public fields
below private ones, and getters scattered across the class.

### Qodana

Can check member ordering in the paid Ultimate tier. Does not auto-correct. Requires Docker, token
management, and a separate pipeline job. Overkill for the specific problem of deterministic member layout.

## What JHarmonizer does differently

JHarmonizer is the only open-source, CI-embeddable tool that:

1. **Reorders class members automatically** according to configurable rules (grouping by kind, visibility,
   name order, accessor bundling, etc.).
2. **Keeps the code safe after reordering.** Before moving anything, JHarmonizer builds a
   *declaration-order dependency graph* that tracks field initializer references, static/instance block
   sequencing, enum constant initializers, and blank-final assignment rules — the exact JLS constraints
   that make some member orderings unsafe. The sorter respects those constraints so the reordered source
   **compiles and runs correctly**.
3. **Runs in CI** as a Maven plugin goal (`reorder`, `check-all`, `check-fast`) or as a standalone fat
   JAR — no IDE, no Docker, no manual step required.
4. **Is open-source and free** under the Apache-2.0 license.

The key distinction from a plain formatter is point 2: sorting without safety is a refactoring hazard.
JHarmonizer was built specifically to close that gap.
