<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Central Processor

`SrcProcessor` is the public core entry point for processing source trees. It is constructed with the embedded default configuration, optionally overlaid by a `FlexibleUnifiedConfig`, then runs one of the configured flow strategies over Java files discovered under a base directory.

## Public API

```java
SrcProcessingResult processSources(
        Path baseDir,
        Collection<String> includeGlobs,
        Collection<String> excludeGlobs,
        FlowType flowType)
```

The result contains aggregated processing statistics and a success flag. Include and exclude patterns are interpreted by the source-file handler while scanning Java files below `baseDir`.

## Flow types

| Flow type | Behavior | Success flag |
|---|---|---|
| `REORDER` | Parses, sorts, serializes, formats, and rewrites changed files. Backups are created when enabled. | Always successful unless a command wrapper fails before/around the flow. |
| `CHECK_ALL` | Runs the same transformation in memory, logs all member relocations and formatting diffs, and does not write files. | Successful only when no files are `REORDERED` or `FORMATTED`. |
| `CHECK_FAIL_FAST` | Checks files until the first ordering or formatting violation requests stop, then returns accumulated statistics. | Successful only when no files are `REORDERED` or `FORMATTED`. |

## Per-file processing

For each file, the processor records parsing, sorting, serialization, and formatting statistics. File statuses are:

- `REORDERED` — member relocations were detected;
- `FORMATTED` — no member relocation was detected, but formatted output differs;
- `CHECKED` — check flow found no changes;
- `UNCHANGED` — reorder flow found no changes;
- `SKIPPED` — a file-level `@jharmonizer:fully-off` directive skipped processing;
- `ERROR` — an unexpected per-file runtime error was caught and logged.

Per-file `ERROR` results are counted and reported but are separate from ordering/formatting non-conformance counts.

## Pipeline

The normal processing pipeline is:

1. read matching source files;
2. parse with the Spoon-based translator;
3. honor file/type opt-out directives;
4. sort members using the compiled configuration and dependency-aware ordering;
5. serialize the Spoon model back to source;
6. format the result with the configured formatter/import options;
7. collect statistics and log diagnostics.

If Spoon model creation fails for a file, the flow logs the model-build failure and tries a formatting-only fallback. If that fallback throws, the file becomes an `ERROR` result and the rest of the stream continues.
