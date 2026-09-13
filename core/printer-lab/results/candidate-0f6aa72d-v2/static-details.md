<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Complete static inventories

Values are exported by the frozen PMD/ArchUnit instrument. The [assessment](assessment.md)
compares the totals, responsibilities and changed hotspots. Original AST node inventories,
method-call names, source hashes, source positions and dependency edges remain in each
archive's `source-metrics.json` and `architecture.json`.

The tables retain separate inventories because removed, split and newly introduced types
are not interchangeable observations. `SpoonTypeUtils` contributes only its two selected
methods; its whole-class fields and bytecode methods are labeled partial and excluded from
owned-state totals. Source class spans can contain nested types and are not additive.

## Baseline 5a121b67

### Files

Whole-file and blank-line counts are context; only selected lines and NCSS enter totals.

| File | Role | Whole file | File lines | Blank file lines | Selected lines | Selected NCSS |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| `spoon/SpoonTypeUtils.java` | shared | false | 145 | 16 | 18 | 4 |
| `translator/SerializedSrcWithSkippedTypeRanges.java` | contract | true | 26 | 5 | 26 | 6 |
| `translator/SrcCharacterRange.java` | contract | true | 20 | 3 | 20 | 8 |
| `translator/spoon/EnumMemberStartCorrectionResolver.java` | implementation | true | 100 | 8 | 100 | 45 |
| `translator/spoon/PrinterConfig.java` | contract | true | 28 | 5 | 28 | 4 |
| `translator/spoon/SpoonCustomSrcPrinter.java` | implementation | true | 156 | 14 | 156 | 39 |
| `translator/spoon/SpoonPrinterHelper.java` | implementation | true | 40 | 5 | 40 | 8 |
| `translator/spoon/SpoonSrcPrinterUtils.java` | implementation | true | 135 | 13 | 135 | 41 |
| `translator/spoon/SpoonTypeMemberUtils.java` | implementation | true | 131 | 8 | 131 | 15 |
| `translator/spoon/SpoonTypeStructurePrinter.java` | implementation | true | 268 | 20 | 268 | 81 |
| `utilities/SrcCodeUtils.java` | shared | true | 78 | 5 | 78 | 25 |

### Source classes

LOC is the PMD class span. WMC, fan-out and TCC use the pinned PMD definitions.

| Class | LOC | NCSS | WMC | Fan-out | TCC |
| --- | ---: | ---: | ---: | ---: | ---: |
| `translator.SerializedSrcWithSkippedTypeRanges` | 16 | 6 | 1 | 7 | 0 |
| `translator.SrcCharacterRange` | 13 | 8 | 1 | 3 | 0 |
| `translator.spoon.EnumMemberStartCorrectionResolver` | 86 | 45 | 18 | 14 | 0 |
| `translator.spoon.PrinterConfig` | 22 | 4 | 0 | 1 | 0 |
| `translator.spoon.SpoonCustomSrcPrinter` | 132 | 39 | 12 | 30 | 1 |
| `translator.spoon.SpoonPrinterHelper` | 32 | 8 | 6 | 3 | 0 |
| `translator.spoon.SpoonSrcPrinterUtils` | 118 | 41 | 19 | 10 | 0 |
| `translator.spoon.SpoonTypeMemberUtils` | 117 | 15 | 6 | 15 | 0 |
| `translator.spoon.SpoonTypeStructurePrinter` | 235 | 81 | 28 | 31 | 0.25 |
| `utilities.SrcCodeUtils` | 70 | 25 | 13 | 3 | 0 |

### Explicit operations

Constructors are included; Lombok-generated methods are outside these source operation counts.
NPath values remain per operation and are not summed.

| Operation | LOC | NCSS | Cyclo | Cognitive | NPath | Fan-out | Arity |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `spoon.SpoonTypeUtils#getRootTypes` | 9 | 2 | 1 | 0 | 1 | 4 | 1 |
| `spoon.SpoonTypeUtils#hasNoDeclaredTypes` | 9 | 2 | 1 | 1 | 2 | 4 | 1 |
| `translator.SerializedSrcWithSkippedTypeRanges#SerializedSrcWithSkippedTypeRanges` | 6 | 3 | 1 | 0 | 1 | 6 | 2 |
| `translator.SrcCharacterRange#SrcCharacterRange` | 7 | 5 | 1 | 0 | 1 | 2 | 2 |
| `translator.spoon.EnumMemberStartCorrectionResolver#buildFirstLinePattern` | 27 | 21 | 7 | 13 | 11 | 2 | 1 |
| `translator.spoon.EnumMemberStartCorrectionResolver#findNextNonWhitespace` | 8 | 5 | 3 | 3 | 3 | 0 | 2 |
| `translator.spoon.EnumMemberStartCorrectionResolver#findPreviousNonWhitespace` | 8 | 5 | 3 | 3 | 3 | 0 | 2 |
| `translator.spoon.EnumMemberStartCorrectionResolver#resolveCorrectedStarts` | 36 | 13 | 5 | 3 | 4 | 13 | 2 |
| `translator.spoon.SpoonCustomSrcPrinter#SpoonCustomSrcPrinter` | 22 | 7 | 1 | 0 | 1 | 10 | 4 |
| `translator.spoon.SpoonCustomSrcPrinter#serializeCompilationUnit` | 12 | 3 | 1 | 0 | 1 | 5 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtAnnotationType` | 9 | 2 | 1 | 0 | 1 | 4 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtClass` | 8 | 2 | 1 | 0 | 1 | 3 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtCompilationUnit` | 36 | 16 | 5 | 5 | 7 | 17 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtEnum` | 8 | 2 | 1 | 0 | 1 | 3 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtInterface` | 8 | 2 | 1 | 0 | 1 | 3 | 1 |
| `translator.spoon.SpoonCustomSrcPrinter#visitCtRecord` | 8 | 2 | 1 | 0 | 1 | 3 | 1 |
| `translator.spoon.SpoonPrinterHelper#SpoonPrinterHelper` | 8 | 2 | 1 | 0 | 1 | 2 | 1 |
| `translator.spoon.SpoonPrinterHelper#getPrintedLength` | 8 | 2 | 1 | 0 | 1 | 0 | 0 |
| `translator.spoon.SpoonPrinterHelper#terminateLine` | 8 | 3 | 4 | 3 | 4 | 1 | 0 |
| `translator.spoon.SpoonSrcPrinterUtils#compileNeedsBlankLineAfterTypeHeader` | 16 | 4 | 2 | 1 | 2 | 5 | 1 |
| `translator.spoon.SpoonSrcPrinterUtils#compileNeedsSeparatorAfter` | 16 | 5 | 2 | 1 | 2 | 6 | 1 |
| `translator.spoon.SpoonSrcPrinterUtils#compileNeedsSeparatorBefore` | 17 | 2 | 1 | 2 | 1 | 6 | 0 |
| `translator.spoon.SpoonSrcPrinterUtils#detectDominantLineSeparator` | 38 | 19 | 7 | 11 | 7 | 1 | 1 |
| `translator.spoon.SpoonSrcPrinterUtils#selectDominantLineSeparator` | 15 | 8 | 7 | 5 | 16 | 1 | 3 |
| `translator.spoon.SpoonTypeMemberUtils#findEffectiveMemberEnd` | 17 | 3 | 1 | 0 | 1 | 10 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#findExplicitTypeMembers` | 16 | 2 | 1 | 0 | 1 | 7 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#findGroupHeader` | 14 | 5 | 2 | 1 | 2 | 3 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#hasLeadingCommentOnSeparateLine` | 43 | 2 | 1 | 0 | 1 | 8 | 3 |
| `translator.spoon.SpoonTypeMemberUtils#hasMatchingLeadingComment` | 14 | 2 | 1 | 0 | 1 | 8 | 2 |
| `translator.spoon.SpoonTypeStructurePrinter#SpoonTypeStructurePrinter` | 21 | 8 | 1 | 0 | 1 | 8 | 4 |
| `translator.spoon.SpoonTypeStructurePrinter#getSortingSkippedTypeRanges` | 13 | 4 | 1 | 0 | 1 | 5 | 0 |
| `translator.spoon.SpoonTypeStructurePrinter#printMemberSeparator` | 33 | 10 | 8 | 8 | 108 | 6 | 6 |
| `translator.spoon.SpoonTypeStructurePrinter#printOriginalFragment` | 31 | 9 | 4 | 2 | 2 | 3 | 2 |
| `translator.spoon.SpoonTypeStructurePrinter#printSkippedType` | 7 | 5 | 1 | 0 | 1 | 5 | 1 |
| `translator.spoon.SpoonTypeStructurePrinter#printType` | 39 | 15 | 4 | 3 | 4 | 22 | 1 |
| `translator.spoon.SpoonTypeStructurePrinter#printTypeMember` | 10 | 7 | 3 | 4 | 3 | 4 | 3 |
| `translator.spoon.SpoonTypeStructurePrinter#printTypeMembers` | 32 | 10 | 3 | 2 | 4 | 12 | 5 |
| `translator.spoon.SpoonTypeStructurePrinter#requireSortingSkippedTypeRanges` | 7 | 4 | 3 | 1 | 2 | 4 | 0 |
| `utilities.SrcCodeUtils#findFragmentEndExclusive` | 17 | 6 | 3 | 2 | 3 | 2 | 3 |
| `utilities.SrcCodeUtils#findFragmentStartWithIndentation` | 26 | 10 | 6 | 9 | 6 | 2 | 3 |
| `utilities.SrcCodeUtils#findIndentationStart` | 18 | 8 | 4 | 4 | 4 | 1 | 2 |

### Bytecode classes

Dependency targets include the scope-filtered targets reported by ArchUnit. Field and method
inventories include compiler/Lombok-generated declarations; inherited fields are not counted.
A final reference does not make its referenced object immutable.

| Class | Partial helper | Fields | Non-final fields | Declared methods | Internal fan-in | Internal fan-out | Dependency targets | External non-Object superclass |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `spoon.SpoonTypeUtils` | true | 2 | 0 | 10 | 1 | 0 | 4 | none |
| `translator.SerializedSrcWithSkippedTypeRanges` | false | 2 | 0 | 5 | 1 | 1 | 9 | none |
| `translator.SrcCharacterRange` | false | 2 | 0 | 5 | 2 | 0 | 5 | none |
| `translator.spoon.EnumMemberStartCorrectionResolver` | false | 0 | 0 | 4 | 1 | 0 | 20 | none |
| `translator.spoon.PrinterConfig` | false | 3 | 0 | 6 | 3 | 0 | 3 | none |
| `translator.spoon.SpoonCustomSrcPrinter` | false | 2 | 0 | 7 | 0 | 6 | 32 | `spoon.reflect.visitor.DefaultJavaPrettyPrinter` |
| `translator.spoon.SpoonPrinterHelper` | false | 0 | 0 | 2 | 2 | 0 | 5 | `spoon.reflect.visitor.PrinterHelper` |
| `translator.spoon.SpoonSrcPrinterUtils` | false | 2 | 0 | 5 | 2 | 1 | 16 | none |
| `translator.spoon.SpoonTypeMemberUtils` | false | 0 | 0 | 5 | 1 | 0 | 17 | none |
| `translator.spoon.SpoonTypeStructurePrinter` | false | 8 | 1 | 8 | 1 | 7 | 34 | none |
| `utilities.SrcCodeUtils` | false | 0 | 0 | 3 | 1 | 0 | 8 | none |

## Candidate 0f6aa72d

### Files

Whole-file and blank-line counts are context; only selected lines and NCSS enter totals.

| File | Role | Whole file | File lines | Blank file lines | Selected lines | Selected NCSS |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| `config/unified/UnifiedSeparator.java` | contract | true | 14 | 1 | 14 | 1 |
| `spoon/SpoonGroupSeparatorUtils.java` | shared | true | 79 | 13 | 79 | 20 |
| `spoon/SpoonTypeUtils.java` | shared | false | 145 | 16 | 18 | 4 |
| `translator/SerializedSrcWithSkippedTypeRanges.java` | contract | true | 26 | 5 | 26 | 6 |
| `translator/SrcCharacterRange.java` | contract | true | 20 | 3 | 20 | 8 |
| `translator/spoon/PrinterConfig.java` | contract | true | 28 | 5 | 28 | 4 |
| `translator/spoon/SpoonSrcPrinter.java` | implementation | true | 202 | 18 | 202 | 73 |
| `translator/spoon/SpoonTypeMemberUtils.java` | implementation | true | 86 | 6 | 86 | 10 |
| `translator/spoon/SrcPrinterOutput.java` | implementation | true | 150 | 21 | 150 | 59 |
| `utilities/SrcCodeUtils.java` | shared | true | 78 | 5 | 78 | 25 |

### Source classes

LOC is the PMD class span. WMC, fan-out and TCC use the pinned PMD definitions.

| Class | LOC | NCSS | WMC | Fan-out | TCC |
| --- | ---: | ---: | ---: | ---: | ---: |
| `config.unified.UnifiedSeparator` | 10 | 1 | 0 | 0 | 0 |
| `spoon.SpoonGroupSeparatorUtils` | 66 | 20 | 7 | 10 | 0.333333 |
| `spoon.SpoonGroupSeparatorUtils$GroupSeparator` | 11 | 3 | 0 | 6 | 0 |
| `translator.SerializedSrcWithSkippedTypeRanges` | 16 | 6 | 1 | 7 | 0 |
| `translator.SrcCharacterRange` | 13 | 8 | 1 | 3 | 0 |
| `translator.spoon.PrinterConfig` | 22 | 4 | 0 | 1 | 0 |
| `translator.spoon.SpoonSrcPrinter` | 172 | 73 | 2 | 30 | 0 |
| `translator.spoon.SpoonSrcPrinter$Serialization` | 139 | 67 | 26 | 29 | 0.285714 |
| `translator.spoon.SpoonTypeMemberUtils` | 75 | 10 | 4 | 14 | 0 |
| `translator.spoon.SrcPrinterOutput` | 140 | 59 | 25 | 2 | 0.535714 |
| `utilities.SrcCodeUtils` | 70 | 25 | 13 | 3 | 0 |

### Explicit operations

Constructors are included; Lombok-generated methods are outside these source operation counts.
NPath values remain per operation and are not summed.

| Operation | LOC | NCSS | Cyclo | Cognitive | NPath | Fan-out | Arity |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `spoon.SpoonGroupSeparatorUtils#markBlankLine` | 7 | 2 | 1 | 0 | 1 | 2 | 1 |
| `spoon.SpoonGroupSeparatorUtils#markHeader` | 8 | 2 | 1 | 0 | 1 | 2 | 2 |
| `spoon.SpoonGroupSeparatorUtils#markSeparator` | 3 | 2 | 1 | 0 | 1 | 2 | 2 |
| `spoon.SpoonGroupSeparatorUtils#resolveSeparator` | 16 | 6 | 4 | 3 | 6 | 4 | 1 |
| `spoon.SpoonTypeUtils#getRootTypes` | 9 | 2 | 1 | 0 | 1 | 4 | 1 |
| `spoon.SpoonTypeUtils#hasNoDeclaredTypes` | 9 | 2 | 1 | 1 | 2 | 4 | 1 |
| `translator.SerializedSrcWithSkippedTypeRanges#SerializedSrcWithSkippedTypeRanges` | 6 | 3 | 1 | 0 | 1 | 6 | 2 |
| `translator.SrcCharacterRange#SrcCharacterRange` | 7 | 5 | 1 | 0 | 1 | 2 | 2 |
| `translator.spoon.SpoonSrcPrinter#SpoonSrcPrinter` | 7 | 2 | 1 | 0 | 1 | 3 | 1 |
| `translator.spoon.SpoonSrcPrinter#serializeCompilationUnit` | 14 | 2 | 1 | 0 | 1 | 6 | 3 |
| `translator.spoon.SpoonSrcPrinter$Serialization#Serialization` | 4 | 3 | 1 | 0 | 1 | 4 | 2 |
| `translator.spoon.SpoonSrcPrinter$Serialization#needsSeparatorAfter` | 5 | 2 | 1 | 1 | 3 | 4 | 1 |
| `translator.spoon.SpoonSrcPrinter$Serialization#needsSeparatorBefore` | 5 | 2 | 1 | 2 | 5 | 4 | 2 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printMemberSeparator` | 21 | 8 | 9 | 6 | 42 | 7 | 5 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printType` | 29 | 16 | 3 | 3 | 4 | 16 | 1 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printTypeMember` | 13 | 8 | 4 | 3 | 5 | 5 | 2 |
| `translator.spoon.SpoonSrcPrinter$Serialization#printTypeMembers` | 16 | 9 | 3 | 2 | 4 | 9 | 4 |
| `translator.spoon.SpoonSrcPrinter$Serialization#serializeCompilationUnit` | 25 | 15 | 4 | 4 | 6 | 14 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#findEffectiveMemberEnd` | 17 | 3 | 1 | 0 | 1 | 10 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#findExplicitTypeMembers` | 16 | 2 | 1 | 0 | 1 | 7 | 1 |
| `translator.spoon.SpoonTypeMemberUtils#hasLeadingCommentOnSeparateLine` | 20 | 2 | 1 | 0 | 1 | 8 | 3 |
| `translator.spoon.SpoonTypeMemberUtils#hasMatchingLeadingComment` | 14 | 2 | 1 | 0 | 1 | 8 | 2 |
| `translator.spoon.SrcPrinterOutput#SrcPrinterOutput` | 9 | 4 | 1 | 0 | 1 | 2 | 1 |
| `translator.spoon.SrcPrinterOutput#detectDominantLineSeparator` | 32 | 19 | 7 | 11 | 7 | 1 | 1 |
| `translator.spoon.SrcPrinterOutput#getPrintedLength` | 7 | 2 | 1 | 0 | 1 | 0 | 0 |
| `translator.spoon.SrcPrinterOutput#printFragment` | 24 | 10 | 4 | 2 | 2 | 0 | 2 |
| `translator.spoon.SrcPrinterOutput#printGroupHeader` | 11 | 3 | 1 | 0 | 1 | 1 | 2 |
| `translator.spoon.SrcPrinterOutput#printTail` | 11 | 5 | 2 | 1 | 2 | 0 | 1 |
| `translator.spoon.SrcPrinterOutput#selectDominantLineSeparator` | 15 | 8 | 7 | 5 | 16 | 1 | 3 |
| `translator.spoon.SrcPrinterOutput#toString` | 4 | 2 | 1 | 0 | 1 | 0 | 0 |
| `translator.spoon.SrcPrinterOutput#writeln` | 4 | 2 | 1 | 0 | 1 | 0 | 0 |
| `utilities.SrcCodeUtils#findFragmentEndExclusive` | 17 | 6 | 3 | 2 | 3 | 2 | 3 |
| `utilities.SrcCodeUtils#findFragmentStartWithIndentation` | 26 | 10 | 6 | 9 | 6 | 2 | 3 |
| `utilities.SrcCodeUtils#findIndentationStart` | 18 | 8 | 4 | 4 | 4 | 1 | 2 |

### Bytecode classes

Dependency targets include the scope-filtered targets reported by ArchUnit. Field and method
inventories include compiler/Lombok-generated declarations; inherited fields are not counted.
A final reference does not make its referenced object immutable.

| Class | Partial helper | Fields | Non-final fields | Declared methods | Internal fan-in | Internal fan-out | Dependency targets | External non-Object superclass |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `config.unified.UnifiedSeparator` | false | 4 | 0 | 3 | 3 | 0 | 2 | `java.lang.Enum` |
| `spoon.SpoonGroupSeparatorUtils` | false | 4 | 0 | 4 | 1 | 2 | 9 | none |
| `spoon.SpoonGroupSeparatorUtils$GroupSeparator` | false | 2 | 0 | 5 | 2 | 1 | 6 | none |
| `spoon.SpoonTypeUtils` | true | 2 | 0 | 10 | 1 | 0 | 4 | none |
| `translator.SerializedSrcWithSkippedTypeRanges` | false | 2 | 0 | 5 | 2 | 1 | 9 | none |
| `translator.SrcCharacterRange` | false | 2 | 0 | 5 | 2 | 0 | 5 | none |
| `translator.spoon.PrinterConfig` | false | 3 | 0 | 6 | 2 | 0 | 3 | none |
| `translator.spoon.SpoonSrcPrinter` | false | 1 | 0 | 1 | 1 | 3 | 10 | none |
| `translator.spoon.SpoonSrcPrinter$Serialization` | false | 4 | 0 | 7 | 1 | 10 | 32 | none |
| `translator.spoon.SpoonTypeMemberUtils` | false | 0 | 0 | 4 | 1 | 0 | 17 | none |
| `translator.spoon.SrcPrinterOutput` | false | 3 | 0 | 8 | 1 | 1 | 9 | none |
| `utilities.SrcCodeUtils` | false | 0 | 0 | 3 | 1 | 0 | 8 | none |
