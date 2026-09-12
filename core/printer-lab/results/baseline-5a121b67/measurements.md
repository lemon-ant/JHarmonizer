<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer measurements

Revision: `5a121b67ae240177a250585db3e1d07abfbf42cf`. Protocol: `printer-lab-v1`. Diagnostic only: **false**.

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
| fixtures | 1 | 773.776 | 254.413 | 8511.538 | 518900.314 | 674.895 |
| fixtures | 2 | 1587.858 | 428.791 | 17466.435 | 521177.637 | 691.625 |
| fixtures | 4 | 2448.616 | 766.119 | 26934.774 | 521232.894 | 697.216 |
| fixtures | 8 | 3470.400 | 1400.117 | 38174.404 | 521635.517 | 711.003 |
| flat1024 | 1 | 574.386 | 69.885 | 574.386 | 1038115.425 | 676.804 |
| flat1024 | 2 | 1110.350 | 160.152 | 1110.350 | 1038199.151 | 676.110 |
| flat1024 | 4 | 1654.557 | 190.736 | 1654.557 | 1044415.605 | 684.314 |
| flat1024 | 8 | 1738.864 | 314.997 | 1738.864 | 1074489.584 | 693.239 |
| flat128 | 1 | 4690.462 | 794.386 | 4690.462 | 134634.685 | 671.093 |
| flat128 | 2 | 9793.109 | 1142.761 | 9793.109 | 134622.263 | 677.887 |
| flat128 | 4 | 13950.337 | 2407.465 | 13950.337 | 133918.965 | 682.158 |
| flat128 | 8 | 17832.048 | 3934.088 | 17832.048 | 135907.166 | 684.087 |
| nested128 | 1 | 446.708 | 70.149 | 446.708 | 1338442.159 | 674.335 |
| nested128 | 2 | 806.956 | 128.973 | 806.956 | 1345199.394 | 682.086 |
| nested128 | 4 | 1292.533 | 217.463 | 1292.533 | 1346641.924 | 689.932 |
| nested128 | 8 | 1735.273 | 176.984 | 1735.273 | 1348336.905 | 691.872 |

Pool peaks include prepared ASTs and JVM overhead. The sum need not occur simultaneously; it is not RSS, retained printer size, or bytes allocated per call.

## Throughput scaling

Ratios of independently measured means; no confidence interval is inferred for these ratios.

| Workload | Threads | Speedup over one worker | Efficiency % |
| --- | ---: | ---: | ---: |
| fixtures | 1 | 1.000 | 100.000 |
| fixtures | 2 | 2.052 | 102.604 |
| fixtures | 4 | 3.165 | 79.113 |
| fixtures | 8 | 4.485 | 56.063 |
| flat128 | 1 | 1.000 | 100.000 |
| flat128 | 2 | 2.088 | 104.394 |
| flat128 | 4 | 2.974 | 74.355 |
| flat128 | 8 | 3.802 | 47.522 |
| flat1024 | 1 | 1.000 | 100.000 |
| flat1024 | 2 | 1.933 | 96.655 |
| flat1024 | 4 | 2.881 | 72.014 |
| flat1024 | 8 | 3.027 | 37.842 |
| nested128 | 1 | 1.000 | 100.000 |
| nested128 | 2 | 1.806 | 90.323 |
| nested128 | 4 | 2.893 | 72.337 |
| nested128 | 8 | 3.885 | 48.557 |

## Sampled batch service time

One worker; no arrival queue. These quantiles describe whole batches, not individual files.

| Workload | p50 µs | p95 µs | p99 µs |
| --- | ---: | ---: | ---: |
| fixtures | 1122.304 | 3121.152 | 5116.723 |
| flat1024 | 1581.056 | 3104.768 | 4702.208 |
| flat128 | 169.472 | 333.312 | 479.432 |
| nested128 | 2146.304 | 4218.061 | 8061.256 |
