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
java -jar jharmonizer-cli.jar restructure --help
```

## Commands

### `restructure`

Rewrites Java source files in `--base-dir` so their member order matches
JHarmonizer's ordering rules. Files that are already correct are left untouched.

```bash
java -jar jharmonizer-cli.jar restructure \
  --base-dir src/main/java

java -jar jharmonizer-cli.jar restructure \
  -b src/main/java \
  -i "**/*.java" \
  -e "**/generated/**"
```

### `check`

Scans **all** files under `--base-dir` and logs every file that would be changed
by a restructure. Always exits `0` when processing completes without errors — use
the log output to see which files need attention.

```bash
java -jar jharmonizer-cli.jar check \
  --base-dir src/main/java

java -jar jharmonizer-cli.jar check \
  -b src/main/java \
  -i "**/*.java"
```

### `check-fast`

Like `check`, but stops at the **first** file that requires restructuring and
exits immediately with code `3`. Useful in CI pipelines where failing fast is
preferred.

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
| `--base-dir` | `-b` | yes | Root directory to scan for Java source files |
| `--include` | `-i` | no | Glob pattern for files to include; can be repeated |
| `--exclude` | `-e` | no | Glob pattern for files to exclude; can be repeated |
| `--verbose` | `-v` | no | Enable DEBUG-level logging |

Glob patterns follow the `java.nio.file.PathMatcher` `glob:` syntax,
e.g. `**/*.java`, `**/generated/**`.

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Processing completed successfully |
| `1` | Processing error (I/O problem, unexpected exception) |
| `2` | Invalid CLI arguments (picocli default) |
| `3` | `check-fast` only — at least one file requires restructuring |

## Typical CI usage

```yaml
# GitHub Actions example
- name: Check source order
  run: |
    java -jar jharmonizer-cli.jar check-fast \
      -b src/main/java \
      -i "**/*.java"
```

Exit code `3` causes the step to fail, signalling that `restructure` needs to be
run locally before pushing.
