<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Maven Plugin: `jharmonizer-maven-plugin`

The Maven plugin integrates JHarmonizer into a Maven build and delegates to the same core flows as the CLI.

## Goals

| Goal | Default phase | Flow | Behavior |
|---|---|---|---|
| `jharmonizer:reorder` | `process-sources` | `REORDER` | Rewrites matching Java files in place. |
| `jharmonizer:check` | `verify` | `CHECK_ALL` | Reports all non-conforming files, then fails the build when `failOnViolation` is `true`. |
| `jharmonizer:check-fast` | `verify` | `CHECK_FAIL_FAST` | Stops at the first non-conforming file, then fails the build when `failOnViolation` is `true`. |

Check goals never modify source files.

## Parameters

| Parameter / property | Type | Default | Meaning |
|---|---|---|---|
| `baseDir` / `jharmonizer.baseDir` | `File` | project base directory when omitted | Scan root. If omitted, include patterns are auto-derived for existing main and test source directories. |
| `includes` / `jharmonizer.includes` | `Set<String>` | auto-derived main/test source includes when `baseDir` is omitted; otherwise all `.java` files under `baseDir` | Glob include patterns. User includes are added to auto-derived includes in project-base mode. |
| `excludes` / `jharmonizer.excludes` | `Set<String>` | empty | Glob exclude patterns. |
| `skip` / `jharmonizer.skip` | `boolean` | `false` | Skip plugin execution. |
| `configFile` / `jharmonizer.configFile` | `File` | `${project.basedir}/jharmonizer.yml` | YAML overlay file. If the default path does not exist, embedded defaults are used. |
| `backupsEnabled` / `jharmonizer.backupsEnabled` | `Boolean` | value from config | Overrides the active config's `backups-enabled` value. |
| `printProcessingStatistics` / `jharmonizer.printProcessingStatistics` | `Boolean` | value from config | Overrides the active config's `print-processing-statistics` value. |
| `failOnViolation` / `jharmonizer.failOnViolation` | `boolean` | `true` | For check goals, fail the build when violations are found. |

## Example

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <configFile>${project.basedir}/jharmonizer.yml</configFile>
        <failOnViolation>true</failOnViolation>
    </configuration>
</plugin>
```

Set `-Djharmonizer.failOnViolation=false` to report violations without failing the build.
