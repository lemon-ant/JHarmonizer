<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Maven Plugin: `jharmonizer-maven-plugin`

## Purpose

Integrates JHarmonizer into Maven builds so Java sources can be reordered or validated
as part of the standard lifecycle.

## Goals

The plugin exposes three goals, each implemented by a dedicated Mojo class:

| Goal                     | Mojo                | Default phase     | Effect                                                                                                                                              |
|--------------------------|---------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `jharmonizer:reorder`    | `ReorderMojo`       | `process-sources` | Rewrites Java source files in-place so member layout matches the configured ordering. Creates `.bak` backup files when backups are enabled.         |
| `jharmonizer:check`      | `CheckMojo`         | `verify`          | Scans all sources, collects every file that does not conform to the configured ordering, and (by default) fails the build with the full report.     |
| `jharmonizer:check-fast` | `CheckFastMojo`     | `verify`          | Scans sources and stops at the first non-conforming file. Faster than `check` when a single violation is enough to fail the build.                  |

The `check` goals **never modify source files**. Only `reorder` writes to the working tree.

All goals are declared `threadSafe = true`.

## Parameters

All goals share the same parameters, defined on the abstract base
`AbstractJHarmonizerMojo`.

| Parameter                   | Property                                | Type      | Default                                                                                                                       | Description                                                                                                                                                            |
|-----------------------------|-----------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `baseDir`                   | `jharmonizer.baseDir`                   | `File`    | _unset_                                                                                                                       | Base directory to scan. When unset, falls back to the project base directory and auto-derives include patterns for both `src/main/java` and `src/test/java`.           |
| `includes`                  | `jharmonizer.includes`                  | `Set<String>` | empty                                                                                                                     | Glob patterns of Java files to include. When `baseDir` is unset, the auto-derived patterns are used in addition to user-provided patterns.                             |
| `excludes`                  | `jharmonizer.excludes`                  | `Set<String>` | empty                                                                                                                     | Glob patterns of Java files to exclude.                                                                                                                                |
| `skip`                      | `jharmonizer.skip`                      | `boolean` | `false`                                                                                                                       | When `true`, the goal logs a skip message and exits without doing any work.                                                                                            |
| `configFile`                | `jharmonizer.configFile`                | `File`    | `${project.basedir}/jharmonizer.yml`                                                                                          | YAML configuration file merged over the embedded defaults. When the file does not exist, only the embedded default configuration is used.                              |
| `backupsEnabled`            | `jharmonizer.backupsEnabled`            | `Boolean` | _unset_                                                                                                                       | Overrides the `backupsEnabled` setting from the active configuration. When unset, the configuration value is used (the embedded default is `true`).                     |
| `printProcessingStatistics` | `jharmonizer.printProcessingStatistics` | `Boolean` | _unset_                                                                                                                       | Overrides the `printProcessingStatistics` setting from the active configuration. When unset, the configuration value is used (the embedded default is `true`).          |
| `failOnViolation`           | `jharmonizer.failOnViolation`           | `boolean` | `true`                                                                                                                        | Applies to `check` and `check-fast`. When `true` and the flow reports any violation, the build fails with `MojoFailureException`. Set to `false` to report-only.       |

Glob patterns follow the standard `java.nio.file.PathMatcher` `glob:` syntax,
e.g. `**/*.java`, `**/generated/**`.

## Sample usage

### Auto-reorder on every build

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>process-sources</phase>
            <goals>
                <goal>reorder</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Enforce order in CI (fail-fast)

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>check-fast</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Manual invocation

```bash
mvn jharmonizer:reorder        # reorder all sources
mvn jharmonizer:check          # report all violations
mvn jharmonizer:check-fast     # fail fast on first violation
mvn jharmonizer:check -Djharmonizer.failOnViolation=false   # report-only
```

## Notes

- `reorder` is bound to `process-sources` by default; this is the standard Maven phase
  for reformatting existing sources (it runs after `generate-sources` and before `compile`).
- `check` and `check-fast` are bound to `verify` by default.
- When the build fails because of `check`/`check-fast`, the plugin logs a hint pointing at
  `mvn jharmonizer:reorder` to fix the violations automatically.
- When `baseDir` is configured explicitly, no auto-derived include patterns are added —
  only user-provided `includes` are used.
