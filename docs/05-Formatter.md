<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Formatter Wrapper

## Purpose

The formatter wrapper runs after sorting and serialization. It delegates to Palantir Java Format and optionally fixes imports.

## Configuration

The YAML `formatting` section controls formatter behavior:

| Key | Current behavior |
|---|---|
| `fix-imports` | Runs Palantir import fixing when `true`. |
| `formatter-style` | `PALANTIR`, `GOOGLE`, `NONE`, or `AOP`; `AOP` maps to Palantir's AOSP style in code. |
| `blank-line-after-type-header` | Printer option applied before formatting. |
| `blank-line-before-comment` | Printer option applied before formatting. |
| `blank-line-between-fields` | Printer option applied before formatting. |

When `formatter-style` is `NONE`, source formatting is skipped. If `fix-imports` is still `true`, import fixing is applied without full source formatting.

## Opt-out interaction

Type-level `@jharmonizer:fully-off` regions are excluded from partial formatting. File-level `@jharmonizer:fully-off` skips the whole file. File/type-level `@jharmonizer:sort-off` disables sorting while still allowing formatting of the remaining eligible ranges.

## Known limitation: non-deterministic wrapping and reflow

Palantir Java Format has edge cases where repeated formatting can produce non-idempotent output for long or heavily wrapped constructs, including long trailing `//` comments, string concatenations, annotations, pointcuts, and fluent chains. This behavior originates upstream in the formatter engine and is not caused by JHarmonizer sorting.

Practical guidance:

- move long trailing `//` comments to standalone comments above the statement;
- extract long annotation/string values into named constants or helper methods where practical;
- keep fluent/pointcut expressions shorter per line when possible;
- use `// @jharmonizer:sort-off` or `// @jharmonizer:fully-off` as a temporary mitigation for persistent oscillation.

## Template placeholders

Template files containing placeholders such as `package ${package};` are not valid Java compilation units for Palantir formatter. If processing such a file fails, JHarmonizer logs an `ERROR` file result and continues with the rest of the run. Add a file-level `// @jharmonizer:fully-off` directive to skip those files cleanly.
