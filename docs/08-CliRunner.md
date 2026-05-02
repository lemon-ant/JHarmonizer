<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# CLI Runner

## Purpose

Console-invocable entry point for the JHarmonizer pipeline. Wraps `jharmonizer-core` and
exposes its functionality as three runnable commands from a single executable fat JAR
(`cli/target/jharmonizer-cli.jar`).

The CLI is aimed at:

- local invocation outside Maven builds
- integration into CI/CD pipelines via exit codes
- ad-hoc validation or reordering of an arbitrary source tree

## Implementation

- Entry point: `io.github.lemon_ant.jharmonizer.cli.command.JHarmonizerCliApplication`.
- Command parser: [picocli](https://picocli.info/).
- Sub-commands extend `BaseCommand`, which holds shared options and the dispatch logic
  to `SrcProcessor#processSources` from `jharmonizer-core`.

## Commands

| Command       | Class               | Flow                       |
|---------------|---------------------|----------------------------|
| `reorder`     | `ReorderCommand`    | `FlowType.REORDER`         |
| `check-all`   | `CheckAllCommand`   | `FlowType.CHECK_ALL`       |
| `check-fast`  | `CheckFastCommand`  | `FlowType.CHECK_FAIL_FAST` |

`reorder` rewrites source files in place (creating `.bak` files when backups are enabled).
`check-all` and `check-fast` never modify files.

## Shared options (from `BaseCommand`)

| Option            | Short | Description                                                                                       |
|-------------------|-------|---------------------------------------------------------------------------------------------------|
| `--base-dir`      | `-b`  | Base directory containing Java source files. Defaults to the current directory when not provided. |
| `--include`       | `-i`  | Glob patterns for files to include. Repeat the option or pass a comma-separated list.             |
| `--exclude`       | `-e`  | Glob patterns for files to exclude. Repeat the option or pass a comma-separated list.             |
| `--verbose`       | `-v`  | Enable DEBUG-level logging and switch to a verbose log pattern.                                   |
| `--config`        | `-c`  | Path to a YAML configuration file merged over the embedded defaults.                              |
| `--no-backup`     | `-B`  | Disable `.bak` file creation even when backups are enabled in configuration.                      |
| `--no-statistics` | `-S`  | Disable the final processing statistics report output.                                            |

Glob patterns follow `java.nio.file.PathMatcher` `glob:` syntax (e.g. `**/*.java`).

## Exit codes

| Code | Meaning                                                                                                |
|------|--------------------------------------------------------------------------------------------------------|
| `0`  | Processing completed successfully and no violations were detected.                                     |
| `1`  | Processing error (I/O problem, unexpected exception, invalid `--base-dir`, invalid `--config` path).   |
| `2`  | Invalid CLI arguments (picocli default).                                                               |
| `3`  | At least one file requires reordering — emitted by both `check-all` and `check-fast`.                  |

Both check commands use the same `3` for "non-conforming files detected" so a CI gate
can match a single value regardless of whether it runs `check-all` or `check-fast`.
`reorder` always returns `0` on a successful run.

## Reference

For full command-line examples, packaging instructions, and logging configuration details,
see the module-level [`cli/README.md`](../cli/README.md).
