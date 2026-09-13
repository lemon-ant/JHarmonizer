<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer end-to-end coverage

Targets: **85% line coverage** and **80% reachable branch coverage** per class listed below.
Scope: the printer classes listed below in `io.github.lemon_ant.jharmonizer.core.translator.spoon`
and the shared `core.spoon.SpoonGroupSeparatorUtils` and `core.utilities.SrcCodeUtils` helpers.
Run only `SrcPrinterE2ETest` for this measurement. It uses the shared source-processor fixture
runner with formatting and import cleanup disabled. Each case compiles its input and output
with JDK 21, compares the complete output, and checks the processing fixed point.
Unit tests and the general E2E suites are excluded from these counters and run during module verification.

## Measured coverage

JDK 21, Spoon 11.5.0, JaCoCo 0.8.14: 30 fixture pairs and 44 source variants,
plus scenario-directory validation (75 tests).
These cover empty preambles, declaration kinds, combined separator requests, group headers,
LF/CRLF/CR endings, space/tab indentation, preserved opt-out fragments, repeated processing,
footer comments, and JavaDoc tags. Footer cases also verify repeated processing and preservation
after top-level type reordering.

## Fixture structure

Printer scenarios live under
[`core/src/test/resources/test-cases/core/e2e/printer/scenarios/`](../core/src/test/resources/test-cases/core/e2e/printer/scenarios/).
Each numbered directory contains `input/`, `expected/`, and `config.yml`. The shared runner
discovers inputs and matches expectations by filename; adding a scenario requires no Java method.
`CHECK_FAIL_FAST` processes the rendered output through sorting, serialization and formatting
and requires an identical result. It therefore verifies idempotence, including footer preservation.

| Contract | Coverage |
| --- | --- |
| Group spacing, comments, declaration kinds and enum constant bodies | Scenarios `01`–`04` |
| Combined separators and preserved opt-out text | Scenario `05`, plus line-ending and indentation variants |
| Comment whitespace and dominant line separators | Scenarios `06`–`07`, plus whitespace and line-ending variants |
| Footer kinds, JavaDoc tags and reordered top-level declarations | Scenarios `08`–`10` |
| Raw blank-line contracts previously checked by 36 separate assertions | Six full-output fixture pairs in scenarios `10`–`15` |
| First nested type with header spacing enabled or disabled | Additional inputs in scenarios `11` and `13` |
| Comments inside and after type ranges, indentation and semicolons after reordered types | Scenario `16`, plus trailing-whitespace and terminal SUB variants |
| Enum lambda member boundary | Existing regression scenario `02-enum-lambda-body-member-boundary` |

The six blank-line expectations preserve the outputs verified by the former assertions;
formatting stays disabled so it cannot hide extra separators. The existing enum-lambda
regression uses the general regression runner and is outside the isolated coverage measurement.

Dedicated Java tests remain for virtual sources, exact skipped-range offsets, result immutability,
independent serialization across sources and configurations, and fragment-boundary utilities. End-of-file and
Unicode whitespace variants transform the resource fixtures and reuse the common processing checks.

## Coverage counters

| Class | Lines (full total) | Raw branches | Excluded outcomes | Reachable branches |
| --- | --- | --- | --- | --- |
| `SpoonSrcPrinter` | 5/5 = 100% | 4/8 | 4 | 4/4 = 100% |
| `SpoonSrcPrinter.Serialization` | 87/87 = 100% | 58/58 | 0 | 58/58 = 100% |
| `SrcPrinterOutput` | 50/54 = 92.59% | 31/34 | 3 | 31/31 = 100% |
| `SpoonTypeMemberUtils` | 28/28 = 100% | 16/24 | 6 | 16/18 = 88.89% |
| `SpoonGroupSeparatorUtils` | 18/18 = 100% | 10/14 | 4 | 10/10 = 100% |
| `SrcCodeUtils` | 25/25 = 100% | 22/26 | 4 | 22/22 = 100% |
| **Total** | **213/217 = 98.16%** | **141/164** | **21** | **141/143 = 98.60%** |

JaCoCo supplies the raw counters. Reachable-branch percentages are reviewed calculations
using the exclusions below; JaCoCo reports retain all branch outcomes.
The former six buffer tests covered text-writing and line-termination methods used only by the
removed comment renderer. Those methods and tests are gone; source-tail output and end-of-file
preservation use the shared E2E assertions. Footer expectations in scenarios `08`–`10` now retain
the original comment spelling and spacing after one blank line. Expected files were adjusted for
this contract change; declaration output, compilation and fixed-point checks remain intact.

`SpoonGroupSeparatorUtils` includes separator assignment and resolution into kind and header text; its full
implementation is measured, including the assignment methods called by the sorter.

## Reproduce

Run from the repository root with Maven and JDK 21:

```text
mvn -B -ntp -pl core clean test jacoco:report -Dci=true "-Dtest=SrcPrinterE2ETest"
```

`clean` removes previous execution data because the JaCoCo agent appends coverage.
If prerequisite artifacts are unavailable locally, build those modules first with their tests skipped.
Open `core/target/site/jacoco/index.html`; XML and CSV reports are in the same directory.
Read the results before other builds append coverage or overwrite the report.

[JaCoCo check](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html) supports raw coverage
thresholds, including per-class limits, but cannot subtract individual unreachable outcomes.
The adjusted targets here require manual review. Run `mvn -B -ntp -pl core verify` for the
module build gates, which enforce the raw thresholds in its POM.

## Unreachable branch outcomes

Reachable branch coverage: `covered / (covered + missed - unreachable)`.
Subtract only documented missed outcomes. Recheck source locations and exclusions after
printer, Lombok, or Spoon changes; no automated exclusion check is installed.

| Exclusion | Reason |
| --- | --- |
| Lombok `@NonNull` parameter failures | The pipeline supplies non-null arguments. Null API arguments are outside the E2E input contract. |
| `SrcPrinterOutput.detectDominantLineSeparator`, empty source | Input without declared types bypasses output construction. |
| `SrcCodeUtils.findFragmentEndExclusive`, scan reaches the range start | `SrcPrinterOutput.printFragment` skips whitespace-only fragments before finding their end. Direct utility tests cover these ranges. |

Per-class exclusions include respectively 4, 0, 2, 6, 4, and 3 Lombok null-argument outcomes
in table order, plus the other exclusions listed above. Uncovered implicit-member filtering
and leading-comment placement outcomes remain counted as reachable.
Unicode whitespace retains its existing behavior.
Line totals remain unadjusted. No classes or methods are manually excluded with `@Generated`.

## Footer regression coverage

The printer preserves literal footer text and final line terminators, including their absence or
repetition, across LF/CRLF/CR endings. The cutoff follows the last original top-level type, even after
declarations are reordered. `SpoonSrcPrinterTest` separately verifies virtual sources using the
same footer cases; those unit tests do not contribute to this table.
