<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# SrcProcessor

## Purpose

`io.github.lemon_ant.jharmonizer.core.SrcProcessor` is the entry point of the core
module. It owns the `CompiledConfig` and per-run components (`Formatter`, `Sorter`,
`PrinterConfig`) and dispatches each processing run to one of the three flows.

## Construction

```java
new SrcProcessor()                                  // default config only
new SrcProcessor(FlexibleUnifiedConfig externalConfig) // default + overlay
```

The constructor compiles the active config via
`ConfigurationManager.overrideDefaultConfig(externalConfig)` (see
[`02-Configurator.md`](02-Configurator.md)), then builds the per-run helpers:

- `Formatter(formatter-style, fix-imports)` — Palantir wrapper.
- `Sorter(compiledConfig)` — member sorter.
- `PrinterConfig(blank-line-after-type-header, blank-line-before-comment, blank-line-between-fields)` — passed to the Spoon custom printer.

## Public API

```java
SrcProcessingResult processSources(
        Path baseDir,
        Collection<String> includeGlobs,
        Collection<String> excludeGlobs,
        FlowType flowType);
```

| Parameter      | Description                                                                                  |
|----------------|----------------------------------------------------------------------------------------------|
| `baseDir`      | Root directory to scan (relative paths are resolved against it).                             |
| `includeGlobs` | `PathMatcher` `glob:` patterns of files to include (`**/*.java`, etc.).                      |
| `excludeGlobs` | `PathMatcher` `glob:` patterns of files to exclude.                                          |
| `flowType`     | One of `REORDER`, `CHECK_ALL`, `CHECK_FAIL_FAST`.                                            |

There are no separate single-file or single-string entry points — the same call
processes one file or many files depending on the resolved file set.

## Result

`SrcProcessingResult` carries:

- `AggregatedProcessingStatistic` — file count, total size, wall-clock and CPU
  timings, per-status breakdown, list of files with unexpected errors, list of
  non-conforming files.
- `boolean success` — computed by the active flow (`flow.isSuccessful(...)`).

The CLI / Maven plugin layer maps `success` to the documented exit codes.

## Flow types

| `FlowType`        | Implementation       | Modifying | Stream behavior        | Success rule                                          |
|-------------------|----------------------|-----------|------------------------|-------------------------------------------------------|
| `REORDER`         | `ReorderFlow`        | yes       | parallel, full         | `true` unless an unexpected per-file error occurred.  |
| `CHECK_ALL`       | `CheckAllFlow`       | no        | parallel, full         | `true` only when no file is non-conforming.           |
| `CHECK_FAIL_FAST` | `CheckFailFastFlow`  | no        | parallel, short-circuit | `true` only when no file is non-conforming. Stops at the first non-conforming file. |

## Internal pipeline

For each source file (driven by the parallel `Stream<Path>` returned by
`SrcFilesHandler.readJavaFiles(...)`), the active flow runs:

1. **Parse** — `SpoonParser.parseJavaSrcFile(srcFile, printerConfig)` →
   `SpoonAstModel` (also resolves opt-out directives).
2. **Opt-out short-circuit** — if the file is `@jharmonizer:fully-off`, the original
   text is reused verbatim and the flow records `SKIPPED_BY_OPT_OUT`.
3. **Sort** — `Sorter.sort(...)` reorders members per `CompiledConfig`.
4. **Serialize** — `SpoonCustomSrcPrinter` re-emits Java source from the reordered AST.
5. **Format** — `Formatter.formatSrc(...)` runs the Palantir pass and (optionally)
   import fixing, skipping ranges marked `@jharmonizer:fully-off`.
6. **Diff** — `DiffReporter` compares original and rewritten text when needed by the flow.
7. **Write / report** — `REORDER` writes the new text to disk (with optional `.bak`
   backup); the `CHECK_*` flows only record the violation.

Each step is timed individually and the timing flows into `FlowProcessingStats`.

## Statistics output

When `print-processing-statistics: true` (default), the run finishes by calling
`ProcessingStatisticsPrintService.render(...)` on the aggregated statistic and logging
it at `INFO`. When statistics are disabled, only a one-line debug summary plus a list
of files with unexpected errors is logged.

## Backups

Backups are controlled by `backups-enabled` in the configuration (default `true`,
overridable via the CLI `--no-backup` flag and the Maven plugin
`jharmonizer.backupsEnabled` parameter). Only `REORDER` writes — and only when the
output text actually differs from the input. The backup file is `<file>.bak` in the
same directory.

## Configuration validation as a pre-step

There is no separate "compile each file with javac before parsing" step. Spoon parse
failures are wrapped in `SpoonModelBuildException`, captured per file, and reported as
`ERROR` in the per-file processing status without aborting the run.
