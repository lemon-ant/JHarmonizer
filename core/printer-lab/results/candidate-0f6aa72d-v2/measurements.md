<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer measurements

Revision: `0f6aa72de70606b241a3e2bc11898c7fb8580199`. Protocol: `printer-lab-v2-invocation-state`. Diagnostic only: **false**.

JDK 21.0.8+9-LTS; 8 logical processors; Windows 11.

## Source size and structure

| Metric | Value |
| --- | ---: |
| sourceFiles | 10 |
| declaredTypesInWholeFiles | 11 |
| declaredOperations | 34 |
| physicalLinesSelected | 701 |
| ncssSelected | 210 |
| sumCyclo | 81 |
| sumCognitive | 59 |
| shortCircuitOperators | 21 |
| cycloOver10 | 0 |
| cognitiveOver15 | 0 |

| Per-operation metric | Mean | p50 | p95 | Max |
| --- | ---: | ---: | ---: | ---: |
| loc | 13.206 | 11.000 | 29.000 | 32.000 |
| ncss | 5.265 | 3.000 | 16.000 | 19.000 |
| cyclo | 2.382 | 1.000 | 7.000 | 9.000 |
| cognitive | 1.735 | 0.000 | 9.000 | 11.000 |
| npath | 3.971 | 1.000 | 16.000 | 42.000 |
| fanOut | 4.147 | 3.000 | 14.000 | 16.000 |
| arity | 1.735 | 2.000 | 4.000 | 5.000 |

| Source construct | Occurrences |
| --- | ---: |
| IfStatement | 20 |
| ConditionalExpression | 6 |
| ForStatement | 2 |
| ForeachStatement | 2 |
| WhileStatement | 2 |
| DoStatement | 0 |
| SwitchStatement | 0 |
| SwitchExpression | 0 |
| CatchClause | 1 |
| LambdaExpression | 14 |

## Complexity hotspots

| Method | Lines | NCSS | Cyclo | Cognitive | NPath |
| --- | ---: | ---: | ---: | ---: | ---: |
| `translator.spoon.SrcPrinterOutput#detectDominantLineSeparator` | 32 | 19 | 7 | 11 | 7 |
| `utilities.SrcCodeUtils#findFragmentStartWithIndentation` | 26 | 10 | 6 | 9 | 6 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printMemberSeparator` | 21 | 8 | 9 | 6 | 42 |
| `translator.spoon.SrcPrinterOutput#selectDominantLineSeparator` | 15 | 8 | 7 | 5 | 16 |
| `translator.spoon.SpoonSrcPrinter$Serialization#serializeCompilationUnit` | 25 | 15 | 4 | 4 | 6 |
| `utilities.SrcCodeUtils#findIndentationStart` | 18 | 8 | 4 | 4 | 4 |
| `spoon.SpoonGroupSeparatorUtils#resolveSeparator` | 16 | 6 | 4 | 3 | 6 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printType` | 29 | 16 | 3 | 3 | 4 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printTypeMember` | 13 | 8 | 4 | 3 | 5 |
| `translator.spoon.SpoonSrcPrinter$Serialization#needsSeparatorBefore` | 5 | 2 | 1 | 2 | 5 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printTypeMembers` | 16 | 9 | 3 | 2 | 4 |
| `translator.spoon.SrcPrinterOutput#printFragment` | 24 | 10 | 4 | 2 | 2 |

## Architecture

| Metric | Value |
| --- | ---: |
| internalEdges | 18 |
| cycleCount | 1 |
| CCD | 39 |
| ACD | 3.25 |
| RACD | 0.2708333333333333 |
| NCCD | 1.054054054054054 |

See `architecture.md` for the graph and `architecture.json` for every dependency.

## Throughput and allocation

An operation serializes the entire workload batch. JMH error is its reported confidence half-width.

| Workload | Threads | Batches/s | JMH error | Documents/s | Allocated B/batch | Sum of pool peaks MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| fixtures | 1 | 3552.142 | 825.855 | 39073.564 | 176248.892 | 675.796 |
| fixtures | 2 | 6522.571 | 1491.251 | 71748.284 | 178061.824 | 683.089 |
| fixtures | 4 | 9911.235 | 2079.733 | 109023.585 | 178123.011 | 693.771 |
| fixtures | 8 | 11873.051 | 1298.930 | 130603.564 | 177979.621 | 704.363 |
| flat1024 | 1 | 988.897 | 265.420 | 988.897 | 921414.694 | 677.859 |
| flat1024 | 2 | 1840.992 | 358.795 | 1840.992 | 921628.965 | 675.335 |
| flat1024 | 4 | 2448.782 | 387.467 | 2448.782 | 922139.389 | 683.122 |
| flat1024 | 8 | 2940.858 | 709.165 | 2940.858 | 923839.653 | 691.512 |
| flat128 | 1 | 9749.091 | 1868.754 | 9749.091 | 116386.349 | 675.469 |
| flat128 | 2 | 14615.899 | 2778.788 | 14615.899 | 116418.907 | 677.685 |
| flat128 | 4 | 22424.573 | 2932.204 | 22424.573 | 116468.662 | 681.958 |
| flat128 | 8 | 27923.374 | 1909.833 | 27923.374 | 116384.702 | 680.861 |
| nested128 | 1 | 765.913 | 140.207 | 765.913 | 1244877.531 | 678.750 |
| nested128 | 2 | 1276.957 | 183.688 | 1276.957 | 1245997.806 | 682.534 |
| nested128 | 4 | 1706.897 | 402.428 | 1706.897 | 1246991.256 | 688.266 |
| nested128 | 8 | 2286.030 | 162.299 | 2286.030 | 1249219.368 | 690.018 |

Pool peaks include prepared ASTs and JVM overhead. The sum need not occur simultaneously; it is not RSS, retained printer size, or bytes allocated per call.

## Throughput scaling

Ratios of independently measured means; no confidence interval is inferred for these ratios.

| Workload | Threads | Speedup over one worker | Efficiency % |
| --- | ---: | ---: | ---: |
| fixtures | 1 | 1.000 | 100.000 |
| fixtures | 2 | 1.836 | 91.812 |
| fixtures | 4 | 2.790 | 69.755 |
| fixtures | 8 | 3.343 | 41.781 |
| flat128 | 1 | 1.000 | 100.000 |
| flat128 | 2 | 1.499 | 74.960 |
| flat128 | 4 | 2.300 | 57.504 |
| flat128 | 8 | 2.864 | 35.803 |
| flat1024 | 1 | 1.000 | 100.000 |
| flat1024 | 2 | 1.862 | 93.083 |
| flat1024 | 4 | 2.476 | 61.907 |
| flat1024 | 8 | 2.974 | 37.173 |
| nested128 | 1 | 1.000 | 100.000 |
| nested128 | 2 | 1.667 | 83.362 |
| nested128 | 4 | 2.229 | 55.714 |
| nested128 | 8 | 2.985 | 37.309 |

## Sampled batch service time

One worker; no arrival queue. These quantiles describe whole batches, not individual files.

| Workload | p50 µs | p95 µs | p99 µs |
| --- | ---: | ---: | ---: |
| fixtures | 173.056 | 501.760 | 860.160 |
| flat1024 | 826.368 | 2101.248 | 3493.315 |
| flat128 | 83.840 | 238.336 | 472.064 |
| nested128 | 1183.744 | 2662.400 | 4262.871 |
