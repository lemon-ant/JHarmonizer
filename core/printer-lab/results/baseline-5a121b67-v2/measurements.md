<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer measurements

Revision: `5a121b67ae240177a250585db3e1d07abfbf42cf`. Protocol: `printer-lab-v2-invocation-state`. Diagnostic only: **false**.

JDK 21.0.8+9-LTS; 8 logical processors; Windows 11.

## Source size and structure

| Metric | Value |
| --- | ---: |
| sourceFiles | 11 |
| declaredTypesInWholeFiles | 10 |
| declaredOperations | 41 |
| physicalLinesSelected | 1000 |
| ncssSelected | 276 |
| sumCyclo | 106 |
| sumCognitive | 87 |
| shortCircuitOperators | 24 |
| cycloOver10 | 0 |
| cognitiveOver15 | 0 |

| Per-operation metric | Mean | p50 | p95 | Max |
| --- | ---: | ---: | ---: | ---: |
| loc | 17.195 | 14.000 | 38.000 | 43.000 |
| ncss | 6.024 | 5.000 | 16.000 | 21.000 |
| cyclo | 2.585 | 1.000 | 7.000 | 8.000 |
| cognitive | 2.122 | 1.000 | 9.000 | 13.000 |
| npath | 5.341 | 2.000 | 11.000 | 108.000 |
| fanOut | 5.146 | 4.000 | 13.000 | 22.000 |
| arity | 1.683 | 1.000 | 4.000 | 6.000 |

| Source construct | Occurrences |
| --- | ---: |
| IfStatement | 30 |
| ConditionalExpression | 4 |
| ForStatement | 4 |
| ForeachStatement | 2 |
| WhileStatement | 4 |
| DoStatement | 0 |
| SwitchStatement | 0 |
| SwitchExpression | 0 |
| CatchClause | 1 |
| LambdaExpression | 24 |

## Complexity hotspots

| Method | Lines | NCSS | Cyclo | Cognitive | NPath |
| --- | ---: | ---: | ---: | ---: | ---: |
| `translator.spoon.EnumMemberStartCorrectionResolver#buildFirstLinePattern` | 27 | 21 | 7 | 13 | 11 |
| `translator.spoon.SpoonSrcPrinterUtils#detectDominantLineSeparator` | 38 | 19 | 7 | 11 | 7 |
| `utilities.SrcCodeUtils#findFragmentStartWithIndentation` | 26 | 10 | 6 | 9 | 6 |
| `translator.spoon.SpoonTypeStructurePrinter#printMemberSeparator` | 33 | 10 | 8 | 8 | 108 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtCompilationUnit` | 36 | 16 | 5 | 5 | 7 |
| `translator.spoon.SpoonSrcPrinterUtils#selectDominantLineSeparator` | 15 | 8 | 7 | 5 | 16 |
| `translator.spoon.SpoonTypeStructurePrinter#printTypeMember` | 10 | 7 | 3 | 4 | 3 |
| `utilities.SrcCodeUtils#findIndentationStart` | 18 | 8 | 4 | 4 | 4 |
| `translator.spoon.EnumMemberStartCorrectionResolver#findNextNonWhitespace` | 8 | 5 | 3 | 3 | 3 |
| `translator.spoon.EnumMemberStartCorrectionResolver#findPreviousNonWhitespace` | 8 | 5 | 3 | 3 | 3 |
| `translator.spoon.EnumMemberStartCorrectionResolver#resolveCorrectedStarts` | 36 | 13 | 5 | 3 | 4 |
| `translator.spoon.SpoonPrinterHelper#terminateLine` | 8 | 3 | 4 | 3 | 4 |

## Architecture

| Metric | Value |
| --- | ---: |
| internalEdges | 15 |
| cycleCount | 0 |
| CCD | 30 |
| ACD | 2.727272727272727 |
| RACD | 0.24793388429752064 |
| NCCD | 0.9090909090909091 |

See `architecture.md` for the graph and `architecture.json` for every dependency.

## Throughput and allocation

An operation serializes the entire workload batch. JMH error is its reported confidence half-width.

| Workload | Threads | Batches/s | JMH error | Documents/s | Allocated B/batch | Sum of pool peaks MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| fixtures | 1 | 729.620 | 233.817 | 8025.821 | 518486.964 | 674.600 |
| fixtures | 2 | 1286.081 | 367.745 | 14146.887 | 520784.064 | 678.897 |
| fixtures | 4 | 1990.710 | 675.138 | 21897.815 | 521707.198 | 698.160 |
| fixtures | 8 | 2915.618 | 922.579 | 32071.798 | 521172.301 | 709.644 |
| flat1024 | 1 | 497.883 | 57.000 | 497.883 | 1038317.129 | 676.439 |
| flat1024 | 2 | 944.433 | 186.112 | 944.433 | 1038266.191 | 679.838 |
| flat1024 | 4 | 1444.030 | 202.067 | 1444.030 | 1045014.767 | 684.200 |
| flat1024 | 8 | 1740.199 | 145.491 | 1740.199 | 1073874.287 | 694.239 |
| flat128 | 1 | 4627.382 | 599.920 | 4627.382 | 135988.099 | 671.895 |
| flat128 | 2 | 8344.599 | 1337.317 | 8344.599 | 134634.001 | 678.148 |
| flat128 | 4 | 12151.033 | 2220.464 | 12151.033 | 134696.205 | 681.648 |
| flat128 | 8 | 15721.098 | 1493.336 | 15721.098 | 135931.190 | 689.795 |
| nested128 | 1 | 362.436 | 56.874 | 362.436 | 1334445.476 | 668.439 |
| nested128 | 2 | 662.955 | 170.326 | 662.955 | 1346275.953 | 682.039 |
| nested128 | 4 | 1220.759 | 165.890 | 1220.759 | 1346467.936 | 690.163 |
| nested128 | 8 | 1451.562 | 128.582 | 1451.562 | 1350783.819 | 690.656 |

Pool peaks include prepared ASTs and JVM overhead. The sum need not occur simultaneously; it is not RSS, retained printer size, or bytes allocated per call.

## Throughput scaling

Ratios of independently measured means; no confidence interval is inferred for these ratios.

| Workload | Threads | Speedup over one worker | Efficiency % |
| --- | ---: | ---: | ---: |
| fixtures | 1 | 1.000 | 100.000 |
| fixtures | 2 | 1.763 | 88.134 |
| fixtures | 4 | 2.728 | 68.211 |
| fixtures | 8 | 3.996 | 49.951 |
| flat128 | 1 | 1.000 | 100.000 |
| flat128 | 2 | 1.803 | 90.165 |
| flat128 | 4 | 2.626 | 65.647 |
| flat128 | 8 | 3.397 | 42.468 |
| flat1024 | 1 | 1.000 | 100.000 |
| flat1024 | 2 | 1.897 | 94.845 |
| flat1024 | 4 | 2.900 | 72.509 |
| flat1024 | 8 | 3.495 | 43.690 |
| nested128 | 1 | 1.000 | 100.000 |
| nested128 | 2 | 1.829 | 91.458 |
| nested128 | 4 | 3.368 | 84.205 |
| nested128 | 8 | 4.005 | 50.063 |

## Sampled batch service time

One worker; no arrival queue. These quantiles describe whole batches, not individual files.

| Workload | p50 µs | p95 µs | p99 µs |
| --- | ---: | ---: | ---: |
| fixtures | 1105.920 | 3432.448 | 6186.598 |
| flat1024 | 1779.712 | 3552.666 | 5406.720 |
| flat128 | 183.296 | 371.200 | 568.822 |
| nested128 | 2256.896 | 4308.992 | 6676.480 |
