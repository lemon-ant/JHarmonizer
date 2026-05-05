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
| `jharmonizer:check-all`      | `CheckAllMojo`         | `validate`        | Scans all sources, collects every file that does not conform to the configured ordering, and (by default) fails the build with the full report.     |
| `jharmonizer:check-fast` | `CheckFastMojo`     | `validate`        | Scans sources and stops at the first non-conforming file. Faster than `check-all` when a single violation is enough to fail the build.                  |

The `check-all` goals **never modify source files**. Only `reorder` writes to the working tree.

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
| `failOnViolation`           | `jharmonizer.failOnViolation`           | `boolean` | `true`                                                                                                                        | Applies to `check-all` and `check-fast`. When `true` and the flow reports any violation, the build fails with `MojoFailureException`. Set to `false` to report-only.       |

`includes` and `excludes` follow the same convention as standard Maven plugins
(`maven-compiler-plugin`, `maven-resources-plugin`, etc.) — Ant/glob-style
patterns such as `**/*.java`, `**/generated/**`. Internally the plugin uses an
in-house parallel directory walker,
[`io.github.lemon-ant:glob-path-finder`](https://github.com/lemon-ant/glob-path-finder),
which streams matching paths in parallel.

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

`check-fast` and `check-all` default to the `validate` phase — the build fails early,
before compilation:

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>check-fast</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### CI vs. local development (profile snippet)

A common setup is "auto-reorder locally, fail the build in CI". You can express
this with two Maven profiles activated by the same property — typically the
`CI` environment variable that GitHub Actions, GitLab CI, Jenkins and most other
CI systems set to `true` automatically:

```xml
<profiles>
    <!-- Local development: rewrite sources in place during process-sources. -->
    <profile>
        <id>jharmonizer-local-reorder</id>
        <activation>
            <property>
                <name>!env.CI</name>
            </property>
        </activation>
        <build>
            <plugins>
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
            </plugins>
        </build>
    </profile>

    <!-- CI: never write, fail fast on the first violation. -->
    <profile>
        <id>jharmonizer-ci-check</id>
        <activation>
            <property>
                <name>env.CI</name>
                <value>true</value>
            </property>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.github.lemon-ant.jharmonizer</groupId>
                    <artifactId>jharmonizer-maven-plugin</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>check-fast</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

The `reorder` profile activates whenever `env.CI` is **not** set (typical local
build), and the `check-fast` profile activates when `env.CI=true` (typical CI
build). Swap `check-fast` for `check-all` if you prefer to surface every
violation in a single CI run instead of stopping at the first one.

### Manual invocation

```bash
mvn jharmonizer:reorder        # reorder all sources
mvn jharmonizer:check-all          # report all violations
mvn jharmonizer:check-fast     # fail fast on first violation
mvn jharmonizer:check-all -Djharmonizer.failOnViolation=false   # report-only
```

## Notes

- `reorder` is bound to `process-sources` by default; this is the standard Maven phase
  for reformatting existing sources (it runs after `generate-sources` and before `compile`).
- `check-all` and `check-fast` are bound to `validate` by default.
- `check-all` performs a **complete pass** over the source tree and collects every
  non-conforming file before deciding the build outcome; `check-fast` short-circuits
  at the first violation. Use `check-all` when you want the full violation report,
  `check-fast` when you want the build to fail as quickly as possible.
- `check-all` and `check-fast` are reporting goals — they never modify source files.
  With `failOnViolation=false`, they switch to **report-only** mode: violations are
  still printed (per-file member-relocation reports and formatting diffs are logged
  at the **ERROR** level), but the build does not fail.
- When `baseDir` is configured explicitly, no auto-derived include patterns are added —
  only user-provided `includes` are used.
