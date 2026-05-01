<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Java AST Parser

## Current parser

JHarmonizer currently uses Spoon as its Java model builder and source translator. The parser is wrapped by the core translator layer and produces a `SpoonAstModel` that carries the parsed compilation unit, opt-out metadata, and source-order snapshots used later for relocation detection.

## Role in the pipeline

```text
Java source
    ↓
Spoon parser / model builder
    ↓
SpoonAstModel
    ↓
Sorter
    ↓
Spoon serialization
    ↓
Formatter/import pass
```

## Behavior

- Java source files are parsed into Spoon compilation units.
- The model keeps enough source-position information for sorting, serialization, opt-out handling, and relocation diagnostics.
- File-level and type-level opt-out comments are collected during translation.
- The original member order is captured so check flows can identify moved members after sorting.
- If Spoon model creation fails for a file, the processing flow attempts a formatting-only fallback; if that also fails, the file is reported as an `ERROR` result and processing continues for other files.

## Java version expectations

The build runs with JDK 21. Main project bytecode is compiled with the configured Maven release level, while test execution and Java 21 fixture handling require JDK 21 in the environment.
