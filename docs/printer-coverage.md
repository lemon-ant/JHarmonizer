<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer end-to-end coverage

Targets: **85% line coverage** and **80% reachable branch coverage** per class listed below.
Scope: `io.github.lemon_ant.jharmonizer.core.translator.spoon`.
Run only `SrcProcessorE2EFixtureTest`, `SrcProcessorRegressionTest`, and `SrcPrinterE2ETest`.
These suites use `SrcProcessor`; unit-test coverage is excluded. `SrcPrinterE2ETest` disables
Palantir and import cleanup, checks printed fragments, and compiles output with JDK 21.

## Measured coverage

JDK 21, Spoon 11.5.0, JaCoCo 0.8.14: 99 existing E2E tests and 15 printer scenarios.

| Class | Lines (full total) | Raw branches | Excluded outcomes | Reachable branches |
| --- | --- | --- | --- | --- |
| `SpoonCustomSrcPrinter` | 45/48 = 93.75% | 15/28 | 13 | 15/15 = 100% |
| `SpoonTypePrinter` | 93/99 = 93.94% | 42/48 | 6 | 42/42 = 100% |
| `SpoonSrcPrinterUtils` | 46/47 = 97.87% | 46/50 | 4 | 46/46 = 100% |
| `SpoonTypeMemberUtils` | 33/33 = 100% | 20/28 | 7 | 20/21 = 95.24% |
| `EnumMemberStartCorrectionResolver` | 46/49 = 93.88% | 25/32 | 4 | 25/28 = 89.29% |
| **Total** | **263/276 = 95.29%** | **148/186** | **34** | **148/152 = 97.37%** |

JaCoCo supplies the raw counters. Reachable-branch percentages are reviewed calculations
using the exclusions below; JaCoCo reports retain all branch outcomes.

## Reproduce

Run from the repository root with Maven and JDK 21:

```text
mvn -B -ntp -pl core -am clean test jacoco:report -Dci=true "-Dtest=SrcProcessorE2EFixtureTest,SrcProcessorRegressionTest,SrcPrinterE2ETest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

`clean` removes previous execution data because the JaCoCo agent appends coverage.
`surefire.failIfNoSpecifiedTests=false` permits prerequisite modules without the selected tests.
Open `core/target/site/jacoco/index.html`; XML and CSV reports are in the same directory.
Read the results before other builds append coverage or overwrite the report.

[JaCoCo check](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html) supports raw coverage
thresholds, including per-class limits, but cannot subtract individual unreachable outcomes.
The adjusted targets here require manual review. Run `mvn -B -ntp verify` for the existing
build gates, which enforce the raw module thresholds in the POMs.

## Unreachable branch outcomes

Reachable branch coverage: `covered / (covered + missed - unreachable)`.
Subtract only documented missed outcomes. Recheck source locations and exclusions after
printer, Lombok, or Spoon changes; no automated exclusion check is installed.

| Exclusion | Reason |
| --- | --- |
| Lombok `@NonNull` parameter failures | The pipeline supplies non-null arguments. Null API arguments are outside the E2E input contract. |
| `SpoonCustomSrcPrinter:59`, no declared types | `SpoonParser.buildSpoonAstModel` returns the original source before constructing the printer. |
| `SpoonCustomSrcPrinter:85`, missing final newline | Printing each type or comment already terminates its line. |
| `SpoonTypePrinter:247`, ranges already finalized | Each serialization creates a fresh printer and collects its ranges once. |
| `SpoonSrcPrinterUtils:90`, empty source | Input without declared types bypasses printer construction. |
| `EnumMemberStartCorrectionResolver:94`, preceding whitespace | The scanner consumes a complete whitespace run; the preceding character is non-whitespace. |

Reachable enum correction, regex failures, comment filtering, and formatting outcomes stay counted.
This includes Unicode whitespace that `String.trim()` retains but `Character.isWhitespace` recognizes.
Line totals remain unadjusted. No classes or methods are excluded with `@Generated`.

## Discovered defect

The printer can [drop a comment after the last top-level type](known-limitations.md#comments-after-the-last-top-level-type).
This reachable defect remains outside the exclusions.
