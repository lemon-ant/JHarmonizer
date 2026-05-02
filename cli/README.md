<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# jharmonizer-cli

Command-line interface for JHarmonizer. Wraps `jharmonizer-core` and exposes its
functionality as three runnable commands from a single executable fat JAR.

## Building the JAR

From the repository root:

```bash
mvn -B -ntp package -pl cli -am -DskipTests
```

The fat JAR is produced at:

```
cli/target/jharmonizer-cli.jar
```

## Packaged-JAR end-to-end tests

The CLI module includes a black-box end-to-end suite that runs in Maven's
`verify` phase **after** the shaded executable JAR has been packaged. The tests
copy a small Java source project from
`cli/src/test/resources/test-cases/cli/e2e/projects/basic-project/` into a temporary working
directory and invoke the packaged artifact exactly as a user would:

```bash
mvn -B -ntp verify -pl cli -am -Dci-pipeline
```

The suite validates:

- `java -jar cli/target/jharmonizer-cli.jar`
- root help and subcommand help output
- all real commands: `reorder`, `check-all`, and `check-fast`
- repeated `--include` / `--exclude` combinations
- exit codes, stdout/stderr, and filesystem side effects
- negative scenarios such as invalid options and invalid base directories

## Running

```bash
java -jar cli/target/jharmonizer-cli.jar <command> [options]
```

Print top-level help:

```bash
java -jar jharmonizer-cli.jar --help
```

Print help for a specific command:

```bash
java -jar jharmonizer-cli.jar reorder --help
```

## Commands

### `reorder`

Rewrites Java source files in `--base-dir` so their member order matches
JHarmonizer's ordering rules. Files that are already correct are left untouched.

```bash
java -jar jharmonizer-cli.jar reorder \
  --base-dir src/main/java

java -jar jharmonizer-cli.jar reorder \
  -b src/main/java \
  -i "**/*.java" \
  -e "**/generated/**"
```

### `check-all`

Scans **all** files under `--base-dir` and logs every file that would be changed
by a reorder. Exits with code `0` when every scanned file is already conformant
and with code `3` when at least one file requires reordering. Source files are
never modified by this command.

```bash
java -jar jharmonizer-cli.jar check-all \
  --base-dir src/main/java

java -jar jharmonizer-cli.jar check-all \
  -b src/main/java \
  -i "**/*.java"
```

### `check-fast`

Like `check-all`, but stops at the **first** file that requires reordering and
exits immediately with code `3`. Useful in CI pipelines where failing fast is
preferred. Source files are never modified by this command.

```bash
java -jar jharmonizer-cli.jar check-fast \
  --base-dir src/main/java

java -jar jharmonizer-cli.jar check-fast \
  -b src/main/java \
  -i "**/*.java"
```

## Options

All commands share the same set of options (inherited from `BaseCommand`):

| Option | Short | Required | Description |
|---|---|---|---|
| `--base-dir` | `-b` | no | Root directory to scan for Java source files (default: current directory) |
| `--include` | `-i` | no | Glob patterns for files to include; repeat the option or pass a comma-separated list |
| `--exclude` | `-e` | no | Glob patterns for files to exclude; repeat the option or pass a comma-separated list |
| `--verbose` | `-v` | no | Enable DEBUG-level logging |
| `--config` | `-c` | no | Path to custom YAML configuration file merged over defaults |
| `--no-backup` | `-B` | no | Disable `.bak` file creation even if enabled in config |
| `--no-statistics` | `-S` | no | Disable final processing statistics output |

Glob patterns follow the `java.nio.file.PathMatcher` `glob:` syntax,
e.g. `**/*.java`, `**/generated/**`.

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Processing completed successfully and no violations were detected |
| `1` | Processing error (I/O problem, unexpected exception, invalid `--base-dir`, invalid `--config` path) |
| `2` | Invalid CLI arguments (picocli default) |
| `3` | At least one file requires reordering — emitted by both `check-all` and `check-fast` |

`reorder` always returns `0` on a successful run; the exit code is reserved for
*detected non-conformance* and is the same (`3`) for both check commands so that CI
gates can match a single value.

## Typical CI usage

```yaml
# GitHub Actions example
- name: Check source order
  run: |
    java -jar jharmonizer-cli.jar check-fast \
      -b src/main/java \
      -i "**/*.java"
```

Exit code `3` causes the step to fail, signalling that `reorder` needs to be
run locally before pushing.

## Logging configuration and startup overrides

The CLI uses **SLF4J + Logback**:

- API: `org.slf4j:slf4j-api`
- Backend: `ch.qos.logback:logback-classic`
- Default config file in this module: `cli/src/main/resources/logback.xml`

By default, the root logger level is `INFO`.

### 1) CLI switch: `--verbose` / `-v`

For all functional commands (`reorder`, `check-all`, `check-fast`), passing
`-v` (`--verbose`) raises the **root** log level to `DEBUG` at command start.
This is the easiest way to get detailed diagnostics from the packaged JAR:

```bash
java -jar cli/target/jharmonizer-cli.jar check-fast \
  -b src/main/java \
  -i "**/*.java" \
  -v
```

### 2) JVM startup key: `-Dlogback.configurationFile=...`

Logback supports replacing the bundled logging config at startup using a JVM
system property. This lets you override levels, appenders, and patterns without
changing application code.

Example custom config (`/tmp/jharmonizer-logback-debug.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} %-5level %logger - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="DEBUG">
    <appender-ref ref="STDOUT"/>
  </root>
</configuration>
```

Run CLI with override:

```bash
java -Dlogback.configurationFile=/tmp/jharmonizer-logback-debug.xml \
  -jar cli/target/jharmonizer-cli.jar check-all \
  -b src/main/java \
  -i "**/*.java"
```

### What is and is not supported now

- ✅ Supported:
  - `-v` / `--verbose` (built-in CLI option)
  - `-Dlogback.configurationFile=...` (standard Logback startup override)
- ⚠️ Not implemented in current bundled config:
  - Dedicated env vars like `LOG_LEVEL=DEBUG`
  - Dedicated JVM keys like `-Dapp.log.level=DEBUG`

The latter can be added later by parameterizing `logback.xml` with custom
properties, but the current file defines a fixed default root level (`INFO`),
with `-v` as the built-in runtime switch.
