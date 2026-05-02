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
    └─ Supplier<SerializedSrcWithSkippedTypeRanges>  (lazy re-serialization)
    ↓
Sorter → SpoonCustomSrcPrinter → Formatter
```

## Key components

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `SpoonParser`                        | Entry point. Wraps the source in a `VirtualFile`, builds a `Launcher` with `complianceLevel = 21`, and assembles the `SpoonAstModel`. |
| `SpoonAstModel`                      | Immutable post-parse snapshot used by the rest of the pipeline.                                       |
| `JHarmonizerOptOutResolver`          | Resolves file-scope and type-scope opt-out directives from the parsed `CtCompilationUnit`.            |
| `RelocationDetector`                 | Captures the original DFS source order of `CtTypeMember`s so the serializer can compute relocations.  |
| `SpoonCustomSrcPrinter` / `SpoonTypePrinter` / `SpoonSrcPrinterUtils` | Spoon-printer customization used when the AST is serialized back to text.        |
| `EnumMemberStartCorrectionResolver`  | Compensates for Spoon offset quirks at the start of enum bodies.                                      |
| `SpoonModelBuildException`           | Wraps Spoon parse failures with the offending source path and a human-readable diagnostic.            |

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
