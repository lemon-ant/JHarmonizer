<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer end-to-end coverage

Targets: **85% line coverage** and **80% reachable branch coverage** per class listed below.
Scope: the printer classes listed below in `io.github.lemon_ant.jharmonizer.core.translator.spoon`
and their shared `core.utilities.SrcCodeUtils` helper.
Run only `SrcPrinterE2ETest` for this measurement. It uses `SrcProcessor`, disables Palantir
and import cleanup, checks printed fragments, and compiles output with JDK 21.
Unit tests and the general E2E suites are excluded from these counters and run during full verification.

## Measured coverage

JDK 21, Spoon 11.5.0, JaCoCo 0.8.14: 29 printer E2E scenarios.
These cover empty preambles, declaration kinds, combined separator requests, group headers,
LF/CRLF/CR endings, space/tab indentation, preserved opt-out fragments, and repeated processing.

| Class | Lines (full total) | Raw branches | Excluded outcomes | Reachable branches |
| --- | --- | --- | --- | --- |
| `SpoonCustomSrcPrinter` | 49/51 = 96.08% | 18/30 | 12 | 18/18 = 100% |
| `SpoonTypeStructurePrinter` | 95/100 = 95% | 46/52 | 6 | 46/46 = 100% |
| `SpoonPrinterHelper` | 6/7 = 85.71% | 5/8 | 3 | 5/5 = 100% |
| `SpoonSrcPrinterUtils` | 36/37 = 97.30% | 43/48 | 4 | 43/44 = 97.73% |
| `SpoonTypeMemberUtils` | 33/33 = 100% | 19/28 | 7 | 19/21 = 90.48% |
| `EnumMemberStartCorrectionResolver` | 46/49 = 93.88% | 25/32 | 4 | 25/28 = 89.29% |
| `SrcCodeUtils` | 25/25 = 100% | 22/26 | 4 | 22/22 = 100% |
| **Total** | **290/302 = 96.03%** | **178/224** | **40** | **178/184 = 96.74%** |

JaCoCo supplies the raw counters. Reachable-branch percentages are reviewed calculations
using the exclusions below; JaCoCo reports retain all branch outcomes.
`SpoonPrinterHelperTest` separately checks six buffer cases: empty output, unterminated lines
with LF/CRLF settings, and existing LF/CRLF/CR endings. Each case terminates the line twice
to verify that it adds at most one terminator. Those unit tests do not contribute to this table.

## Reproduce

Run from the repository root with Maven and JDK 21:

```text
mvn -B -ntp -pl core -am clean test jacoco:report -Dci=true "-Dtest=SrcPrinterE2ETest" "-Dsurefire.failIfNoSpecifiedTests=false"
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
| `SpoonCustomSrcPrinter:63`, no declared types | `SpoonParser.buildSpoonAstModel` returns the original source before constructing the printer. |
| `SpoonPrinterHelper:36`, empty buffer or unterminated final line (two outcomes) | The E2E pipeline prints at least one type, and printing each fragment terminates its line before this call. These cases are covered directly by `SpoonPrinterHelperTest`. |
| `SpoonTypeStructurePrinter:263`, ranges already finalized | Each serialization creates a fresh printer and collects its ranges once. |
| `SpoonSrcPrinterUtils:90`, empty source | Input without declared types bypasses printer construction. |
| `SrcCodeUtils:27`, trailing-whitespace scan reaches the range start | `SpoonTypeStructurePrinter.printOriginalFragment` skips empty and whitespace-only fragments before finding their end. Direct utility tests cover these ranges. |
| `EnumMemberStartCorrectionResolver:94`, preceding whitespace | The scanner consumes a complete whitespace run; the preceding character is non-whitespace. |

Reachable enum correction, regex failures, comment filtering, and formatting outcomes stay counted.
This includes Unicode whitespace that `String.trim()` retains but `Character.isWhitespace` recognizes.
Line totals remain unadjusted. No classes or methods are excluded with `@Generated`.

## Discovered defect

The printer can [drop a comment after the last top-level type](known-limitations.md#comments-after-the-last-top-level-type).
This reachable defect remains outside the exclusions.
