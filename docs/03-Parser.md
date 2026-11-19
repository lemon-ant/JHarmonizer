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

The selection criteria (semantic-rich AST, comment/annotation preservation, robust
re-serialization, Java 21 support) were resolved during the initial POC. Other
candidates evaluated at the time (JavaParser, Eclipse JDT, ANTLR) are not used.

The decisive advantage of Spoon over a purely syntactic parser is that it does
**not** stop at a token tree: it provides a fully resolved, navigable object model
of the type system and of the in-code dependencies (`CtFieldReference`,
`CtExecutableReference`, `CtTypeReference`, `CtVariableAccess`, etc.). This is what
makes the declaration-order dependency graph (see
[`declaration-order-dependencies.md`](declaration-order-dependencies.md)) feasible:
each `*DependencyProvider` walks the resolved references inside a member's body to
discover which other members it depends on, instead of guessing from textual
identifiers.

## Where it fits in the pipeline

```
SrcFile (raw text + path)
    ↓
SpoonParser.parseJavaSrcFile(srcFile)
    ↓
SpoonAstModel
    ├─ CtCompilationUnit       (Spoon AST)
    ├─ JHarmonizerOptOuts      (resolved file/type-scope opt-out directives)
    ├─ originalMemberOrder     (DFS source-order snapshot of CtTypeMembers)
    ├─ annotationSrcGroups     (immutable source fragments, gap layouts, and original replacement code)
    ├─ annotationOrderByGroupSrcStart (empty during parsing; changed orders after sorting)
    └─ srcCode                 (exact original text for source offsets)
    ↓
Sorter → SpoonSrcPrinter → Formatter
```

## Key components

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `SpoonParser`                        | Entry point. Wraps the source in a `VirtualFile`, builds a `Launcher` with `complianceLevel = 21`, and assembles the `SpoonAstModel`. |
| `SpoonAstModel`                      | Typed processing context with the mutable Spoon AST, immutable annotation groups, original source, opt-outs, and source-order snapshot. |
| `JHarmonizerOptOutResolver`          | Resolves file-scope and type-scope opt-out directives from the parsed `CtCompilationUnit`.            |
| `SpoonTypeTreeUtils`                  | Traverses source type trees and enclosing types; captures the original DFS order of `CtTypeMember`s with `streamDeclaredHierarchy`. |
| `SpoonSrcPrinter` / `SrcPrinterOutput` | Standalone source-fragment printing and member layout; see [source printer](source-printer.md). |
| `SpoonModelBuildException`           | Wraps Spoon parse failures with the offending source path and a human-readable diagnostic.            |

[`AnnotationSrcGroup`](../core/src/main/java/io/github/lemon_ant/jharmonizer/core/translator/spoon/annotation/AnnotationSrcGroup.java)
is the shared model beside the scanner, with nested annotation, gap, and private base-fragment types.
`AnnotationSourceScanner` produces these groups and retains the private parsing state and source-preparation logic.
The scanner and its type-name, import, inheritance, and dependency preparation helpers belong to
`translator.spoon.annotation`. Parser-wide tree traversal stays in
[`SpoonTypeTreeUtils`](../core/src/main/java/io/github/lemon_ant/jharmonizer/core/translator/spoon/SpoonTypeTreeUtils.java).
The neutral `core.spoon.SpoonTypeUtils` retains only main-type selection, effective member boundaries, and explicit
source-member traversal used across processing stages.
For the annotation identity algorithm, lookup precedence, stopping conditions, and worked examples, see
[annotation type resolution](annotation-type-resolution.md) (in Russian).
The groups belong to `SpoonAstModel`, including annotations absent from Spoon's AST. Sorting mutates
declaration order in the AST and returns a model wrapper with `annotationOrderByGroupSrcStart`: only changed groups map
from their original start offsets to existing fragments in sorted order. The original groups and AST annotation lists
remain unchanged. Printing consumes that returned model together with the flow's `PrinterConfig`;
annotation order is neither stored in `CtCompilationUnit` metadata nor captured by a serialization supplier.
Parsing does not depend on printer settings. The flow passes its immutable configuration to
`SrcAstTranslator.serialize(model, printerConfig)` for each printing invocation.
The scanner collects annotations and gaps in separate lists with source bounds and comment ownership. The group
constructor retains unmodifiable views; callers must not modify the lists after handoff. Group, gap, and layout
construction and the annotation-fragment builder are package-private. The scanner starts fresh lists for each group
and prepares moved-gap layouts for separator requirements zero, one, and two. Each layout contains the ready leading
whitespace and whether the selected annotation's exact separators are required. Fixed gap content and original code
remain separate and unchanged. An annotation whose line comment already ends with a Unicode escape needs no additional
physical separator.
`annotationSrcFragmentsInSrcOrder` always retains source order within its group. The private `srcFragments` sequence
and `replacementCode` lazily cache the original text. Sorting records a separate order without creating new gaps or groups.
`AnnotationSrcGroupRenderer` joins that order with the prepared layouts and preserves an original annotation/gap pair
verbatim. A missing order uses the group's cached original code. Rendering does not analyze source text or reassign
comment ownership; the source printer only delegates this joining while replacing the original range.

## What is preserved

- Block, line, and Javadoc comments — preserved through the Spoon model and the custom
  printer, including comments attached to top-level types and members.
- Annotations on declarations.
- Member-level source positions (`SourcePosition`) used for ordering tie-breakers and
  for emitting opt-out warnings with `path:line:column` locations.

## Java version

`SpoonParser.JAVA_VERSION = 21` — Spoon 11.x is required because earlier Spoon versions
do not support Java 17+ source compliance levels. See the build memory: minimum runtime
target is Java 17 (Spoon's own requirement); test compilation targets Java 21.

## Failure mode

If Spoon fails to build the model for a file, `SpoonParser` throws
`SpoonModelBuildException` carrying the source path and a normalized message. The
flow layer captures it as a per-file processing failure without aborting the whole run.
