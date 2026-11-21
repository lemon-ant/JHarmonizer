<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Java AST Parser

## Purpose

Convert each `.java` source file into an AST that the rest of the pipeline (sorter,
serializer, formatter) can manipulate and reserialize back into Java source.

## Library

JHarmonizer uses **[Spoon](https://github.com/INRIA/spoon)** as its single AST library.
The implementation lives in
`io.github.lemon_ant.jharmonizer.core.translator.spoon`.

Spoon provides declarations, source positions, and references to types and members. Dependency providers use these
references to identify declarations that must stay in order. The parser builds one model per file in no-classpath
mode, so some external declarations may be unavailable. [Declaration-order dependencies](declaration-order-dependencies.md).

## Where it fits in the pipeline

```
SrcFile (raw text + path)
    ↓
SpoonParser.parseJavaSrcFile(srcFile)
    ↓
SpoonAstModel
    ├─ CtCompilationUnit           (Spoon AST)
    ├─ JHarmonizerOptOuts          (resolved file/type-scope opt-out directives)
    ├─ originalMemberOrder         (DFS source-order snapshot of CtTypeMembers)
    ├─ originalAnnotationSrcGroups (immutable source fragments, gap layouts, and original replacement code)
    └─ srcCode                     (exact original text for source offsets)
    ↓
Sorter → SortingResult (same SpoonAstModel after sorting, selected orders, change flags, diagnostics, timing)
    ↓
Flow: SortingResult → SpoonPrintingInput → SpoonSrcPrinter → Formatter
```

## Key components

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `SpoonParser`                        | Entry point. Wraps the source in a `VirtualFile`, builds a `Launcher` with `complianceLevel = 21`, and assembles the `SpoonAstModel`. |
| `SpoonAstModel`                      | Typed processing context with the mutable Spoon AST, immutable annotation groups, original source, opt-outs, and source-order snapshot. |
| `SpoonPrintingInput`                 | Post-parse printing context with the shared working AST, source data, skipped types, and selected annotation orders. |
| `JHarmonizerOptOutResolver`          | Resolves file-scope and type-scope opt-out directives from the parsed `CtCompilationUnit`.            |
| `SpoonTypeTreeUtils`                  | Traverses source type trees and enclosing types; captures the original DFS order of `CtTypeMember`s with `streamDeclaredHierarchy`. |
| `SpoonSrcPrinter` / `SrcPrinterOutput` | Standalone source-fragment printing and member layout; see [source printer](source-printer.md). |
| `SpoonModelBuildException`           | Wraps Spoon parse failures with the offending source path and a human-readable diagnostic.            |

## Annotation groups

The parser supplies the original source and Spoon model to `AnnotationSourceScanner`. The scanner uses Java tokens
to find adjacent annotation uses, including array-dimension and receiver annotations omitted by Spoon. For each group
it records the exact annotation text, source ranges, sorting criteria, and order-preservation keys. It also records
the gaps and prepares the whitespace needed when an annotation moves.

`SpoonAstModel` holds these groups in source order. The sorter and printer read the same groups.
See [annotation type resolution](annotation-type-resolution.md), [annotation order preservation](annotation-order-preservation.md),
and [annotation rendering](source-printer.md#annotation-group-rendering).

## What is preserved

- Block, line, and Javadoc comments — preserved through the Spoon model and the custom
  printer, including comments attached to top-level types and members.
- Annotations on declarations.
- Member-level source positions (`SourcePosition`) used for ordering tie-breakers and
  for emitting opt-out warnings with `path:line:column` locations.

## Java version

`SpoonParser` parses source at Java 21 compliance. The core module targets Java 17 at runtime and Java 21 for tests.

## Failure mode

If Spoon fails to build the model for a file, `SpoonParser` throws
`SpoonModelBuildException` carrying the source path and a normalized message. The
flow layer captures it as a per-file processing failure without aborting the whole run.
