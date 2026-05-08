<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Why JHarmonizer?

Developing a coding convention is the easy part. The real challenge is making the whole team follow it,
every commit, every day, without exception.

Manual enforcement does not scale. Sorting and reformatting members by hand is tedious, error-prone, and
simply does not happen consistently. Even when a developer tries to comply, there is no guarantee the
result is correct. Human responsibility alone is not enough.

The only reliable approach is two-fold:

1. **Full automation with deterministic output.** The tool must produce exactly the same result regardless
   of whether the source was already partially ordered or untouched. The output must be absolutely
   predictable — no options that could let the result vary from machine to machine or from run to run.

2. **Hard enforcement in CI.** Nothing must be mergeable into protected branches when the code has not
   been processed by the tool. Quality gates in the pipeline are the only guarantee that standards are
   actually observed across the entire team.

### Why not just ask an AI assistant?

AI models are, by definition, non-deterministic. They can produce a correct result one time, forget the
rule the next, or silently deviate when the context is slightly different. Strict, deterministic algorithms
are the only 100% guarantee for maintaining a coding standard at scale. A dedicated tool like JHarmonizer
is also orders of magnitude faster and cheaper to run than an LLM inference call.

### Does this matter in an AI-generated-code world?

It matters more, not less. AI-generated code still needs human review: humans must quickly understand what
changed, catch over-engineered or under-specified logic, and make strategic architectural decisions. Code
that follows a deterministic, enforced layout is dramatically easier to scan and reason about. AI models
constrained by quality gates can themselves read CI output, understand failures, and produce conformant
code on the next attempt — but they need those gates to exist in the first place.

JHarmonizer provides the automated, deterministic, CI-embeddable quality gate that makes consistent member
ordering enforceable in practice.

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
