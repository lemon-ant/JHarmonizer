<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# jharmonizer-cli

Command-line interface for JHarmonizer. It wraps `jharmonizer-core` and exposes three runnable commands from a single executable fat JAR.

## Building the JAR

From the repository root:

```bash
mvn -B -ntp package -pl cli -am -DskipTests
```

The fat JAR is produced at:

```text
cli/target/jharmonizer-cli.jar
```

## Packaged-JAR end-to-end tests

The CLI module includes a black-box end-to-end suite that runs in Maven's `verify` phase after the shaded executable JAR has been packaged:

```bash
mvn -B -ntp verify -pl cli -am -Dci-pipeline
```

The suite validates root help, subcommand help, `reorder`, `check-all`, `check-fast`, include/exclude combinations, exit codes, stdout/stderr, filesystem side effects, invalid options, and invalid base directories.

## Running

```bash
java -jar cli/target/jharmonizer-cli.jar <command> [options]
```

Print help:

```bash
java -jar cli/target/jharmonizer-cli.jar --help
java -jar cli/target/jharmonizer-cli.jar reorder --help
```

## Commands

### `reorder`

Rewrites Java source files under `--base-dir` so their member order and formatting match JHarmonizer's rules. Files that are already correct are left untouched.

```bash
java -jar cli/target/jharmonizer-cli.jar reorder \
  --base-dir src/main/java

java -jar cli/target/jharmonizer-cli.jar reorder \
  -b src/main/java \
  -i "**/*.java" \
  -e "**/generated/**"
```

### `check-all`

Scans all matching files under `--base-dir`, logs every file that would be changed by a reorder/format pass, and does not write files. It exits `0` when all files conform and exits `1` when ordering or formatting violations are detected.

```bash
java -jar cli/target/jharmonizer-cli.jar check-all \
  --base-dir src/main/java

java -jar cli/target/jharmonizer-cli.jar check-all \
  -b src/main/java \
  -i "**/*.java"
```

### `check-fast`

Like `check-all`, but stops at the first file that requires reordering or formatting and exits immediately with code `3`. Useful in CI pipelines where failing fast is preferred.

```bash
java -jar cli/target/jharmonizer-cli.jar check-fast \
  --base-dir src/main/java

java -jar cli/target/jharmonizer-cli.jar check-fast \
  -b src/main/java \
  -i "**/*.java"
```

## Options

All functional commands share the same options:

| Option | Short | Required | Description |
|---|---|---:|---|
| `--base-dir` | `-b` | no | Root directory to scan for Java source files (default: current directory). |
| `--include` | `-i` | no | Glob patterns for files to include; repeat the option or pass a comma-separated list. |
| `--exclude` | `-e` | no | Glob patterns for files to exclude; repeat the option or pass a comma-separated list. |
| `--verbose` | `-v` | no | Enable DEBUG-level logging and detailed runtime failures. |
| `--config` | `-c` | no | Path to custom YAML configuration file merged over defaults. |
| `--no-backup` | `-B` | no | Disable `.bak` file creation even if enabled in config. |
| `--no-statistics` | `-S` | no | Disable final processing statistics output. |

Glob patterns follow the `java.nio.file.PathMatcher` `glob:` syntax, for example `**/*.java` or `**/generated/**`.

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Command completed successfully. For `reorder`, this includes files that were modified. |
| `1` | Invalid base/config path, unexpected command-level runtime failure, or `check-all` detected ordering/formatting violations. |
| `2` | Invalid CLI arguments (picocli default). |
| `3` | `check-fast` detected at least one file that requires reordering or formatting. |

Per-file unexpected processing errors are logged and counted as `ERROR` file results; they do not by themselves change a check command's success flag unless an ordering or formatting violation is also detected.

## Typical CI usage

```yaml
# GitHub Actions example
- name: Check source order
  run: |
    java -jar cli/target/jharmonizer-cli.jar check-fast \
      -b src/main/java \
      -i "**/*.java"
```

Exit code `3` causes the step to fail, signalling that `reorder` needs to be run locally before pushing.

## Logging configuration and startup overrides

The CLI uses SLF4J + Logback with `cli/src/main/resources/logback.xml`. By default, the root logger level is `INFO`.

Passing `-v` / `--verbose` raises the root log level to `DEBUG` at command start and switches to a more detailed console pattern.

Logback can also be configured at JVM startup:

```bash
java -Dlogback.configurationFile=/path/to/logback.xml \
  -jar cli/target/jharmonizer-cli.jar check-fast \
  -b src/main/java
```
