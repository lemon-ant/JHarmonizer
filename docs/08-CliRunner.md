<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# CLI Runner

The CLI is implemented in the `cli` module as a picocli-based fat JAR. It delegates to `jharmonizer-core` and exposes three subcommands: `reorder`, `check-all`, and `check-fast`.

## Commands

| Command | Flow | Behavior |
|---|---|---|
| `reorder` | `REORDER` | Rewrites matching Java files in place. If backups are enabled, changed files are first renamed to `.bak`. |
| `check-all` | `CHECK_ALL` | Processes every matching Java file, logs all ordering/formatting violations, and does not rewrite files. |
| `check-fast` | `CHECK_FAIL_FAST` | Stops after the first ordering or formatting violation and does not rewrite files. |

## Common options

| Option | Short | Meaning |
|---|---|---|
| `--base-dir` | `-b` | Directory to scan. Defaults to the current directory. |
| `--include` | `-i` | Glob include patterns. May be repeated or comma-separated. |
| `--exclude` | `-e` | Glob exclude patterns. May be repeated or comma-separated. |
| `--config` | `-c` | YAML overlay merged over the embedded default config. |
| `--verbose` | `-v` | Switch root logging to DEBUG and print detailed runtime failures. |
| `--no-backup` | `-B` | Override configuration to disable `.bak` backup creation. |
| `--no-statistics` | `-S` | Override configuration to suppress the final statistics report. |

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Command completed successfully. For `reorder`, this includes files that were modified. |
| `1` | Invalid base/config path, unexpected command-level runtime failure, or `check-all` detected ordering/formatting violations. |
| `2` | Invalid CLI arguments reported by picocli. |
| `3` | `check-fast` detected the first ordering/formatting violation. |

Per-file unexpected processing errors are logged and counted as `ERROR` file results; they do not by themselves change the check-flow success flag unless an ordering or formatting violation is also detected.
