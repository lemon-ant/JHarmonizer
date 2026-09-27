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
SpoonParser.parseJavaSrcFile(srcFile, printerConfig)
    ↓
SpoonAstModel
    ├─ CtCompilationUnit       (Spoon AST)
    ├─ JHarmonizerOptOuts      (resolved file/type-scope opt-out directives)
    ├─ originalMemberOrder     (DFS source-order snapshot of CtTypeMembers)
    ├─ annotationSrcGroups     (group bounds, mixed annotation/gap fragments, and lazy replacement code)
    ├─ srcCode                 (exact original text for source offsets)
    └─ PrinterConfig           (immutable spacing configuration)
    ↓
Sorter → SpoonSrcPrinter → Formatter
```

## Key components

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `SpoonParser`                        | Entry point. Wraps the source in a `VirtualFile`, builds a `Launcher` with `complianceLevel = 21`, and assembles the `SpoonAstModel`. |
| `SpoonAstModel`                      | Typed processing context with the mutable Spoon AST, immutable annotation groups, original source, and printer configuration. |
| `JHarmonizerOptOutResolver`          | Resolves file-scope and type-scope opt-out directives from the parsed `CtCompilationUnit`.            |
| `RelocationDetector`                 | Captures the original DFS source order of `CtTypeMember`s so the serializer can compute relocations.  |
| `SpoonSrcPrinter` / `SrcPrinterOutput` | Standalone source-fragment printing and member layout; see [source printer](source-printer.md). |
| `SpoonModelBuildException`           | Wraps Spoon parse failures with the offending source path and a human-readable diagnostic.            |

`AnnotationSrcGroup` is the shared model in `core.spoon`, with nested annotation, gap, layout, and base-fragment types.
`AnnotationSourceScanner` produces these groups and retains the private parsing state and source-preparation logic.
The groups belong to `SpoonAstModel`, including annotations absent from Spoon's AST. Sorting mutates
declaration order in the AST and returns a model wrapper with the new immutable group order. AST annotation lists
remain unchanged. Printing consumes that returned model directly;
annotation order is neither stored in `CtCompilationUnit` metadata nor captured by a serialization supplier.
The scanner collects annotations and gaps in separate lists with source bounds and comment ownership. The group
constructor retains unmodifiable views; callers must not modify the lists after handoff. The scanner starts fresh
lists for each group. The sorter supplies a new annotation order and resolved gaps to the same constructor.
`annotationSrcFragments` represents source order after scanning and comparator order after sorting. The private
`srcFragments` sequence is assembled by a static interleaving helper when replacement code is requested, then cached.
Each group lazily concatenates and caches its replacement code from that sequence. All original ranges remain unchanged.
Each gap contains one ready-to-emit code string and a shared `AnnotationGapLayout` prepared by the scanner. The layout
separates fixed content from whitespace prefixes and records available physical separators. An annotation whose line
comment already ends with a Unicode escape requires no additional physical separator. Sorting uses these prepared
components and counts without reading or analyzing the original source.

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
