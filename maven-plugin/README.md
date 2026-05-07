<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# `jharmonizer-maven-plugin`

Maven front-end for [`jharmonizer-core`](../core/README.md). Adds three goals to a
Maven build so Java sources can be reordered or validated as part of the standard
lifecycle, with no IDE coupling.

## Maven coordinates

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0.1</version>
</plugin>
```

The plugin is `threadSafe = true` on every goal.

## Goals

| Goal                     | Mojo            | Default phase     | Effect                                                                                          |
|--------------------------|-----------------|-------------------|-------------------------------------------------------------------------------------------------|
| `jharmonizer:reorder`    | `ReorderMojo`   | `process-sources` | Rewrites Java source files in place; optional `.bak` backups.                                   |
| `jharmonizer:check-all`  | `CheckAllMojo`  | `validate`        | Read-only. Collects every non-conforming file and (by default) fails the build with the report. |
| `jharmonizer:check-fast` | `CheckFastMojo` | `validate`        | Read-only. Stops at the first non-conforming file. Faster than `check-all`.                     |

`check-all` and `check-fast` **never modify source files**. Only `reorder` writes
to the working tree. With `failOnViolation=false` both check goals switch to
report-only mode (violations are logged at ERROR, the build does not fail).

## Quick start

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.lemon-ant.jharmonizer</groupId>
            <artifactId>jharmonizer-maven-plugin</artifactId>
            <version>1.0.1</version>
            <executions>
                <execution>
                    <id>jharmonizer-reorder</id>
                    <phase>process-sources</phase>
                    <goals><goal>reorder</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

For a typical "auto-reorder locally, fail the build in CI" setup using two
profiles activated by the `CI` environment variable, see the
[CI vs. local development profile snippet in `docs/09-Maven-plugin.md`](../docs/09-Maven-plugin.md#ci-vs-local-development-profile-snippet).

## Parameters

All goals share the same parameters (see `AbstractJHarmonizerMojo`):

| Parameter                   | Property                                | Default                              | Notes                                                                                            |
|-----------------------------|-----------------------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------|
| `baseDir`                   | `jharmonizer.baseDir`                   | _unset_ → `${project.basedir}`        | When unset, auto-derives `src/main/java/**` and `src/test/java/**` includes.                     |
| `includes`                  | `jharmonizer.includes`                  | empty                                | Glob patterns of files to include (in addition to auto-derived patterns when `baseDir` is unset).|
| `excludes`                  | `jharmonizer.excludes`                  | empty                                | Glob patterns of files to exclude.                                                               |
| `skip`                      | `jharmonizer.skip`                      | `false`                              | When `true`, the goal logs a skip message and exits.                                             |
| `configFile`                | `jharmonizer.configFile`                | `${project.basedir}/jharmonizer.yml` | YAML overlay merged over the embedded defaults. Missing file → defaults only.                    |
| `backupsEnabled`            | `jharmonizer.backupsEnabled`            | _unset_                              | Overrides `backupsEnabled` from the active configuration (default `true`).                       |
| `processingStatisticsMode`  | `jharmonizer.processingStatisticsMode`  | _unset_                              | Overrides `processingStatisticsMode` from the active configuration (default `MINIMAL`). Accepted values: `FULL`, `MINIMAL`, `DISABLED`. |
| `failOnViolation`           | `jharmonizer.failOnViolation`           | `true`                               | Applies to `check-all`/`check-fast`. `false` → report-only.                                      |

`includes`/`excludes` follow the same convention as standard Maven plugins
(Ant/glob-style patterns). Internally the plugin uses
[`io.github.lemon-ant:glob-path-finder`](https://github.com/lemon-ant/glob-path-finder),
an in-house library purpose-built for parallel streaming traversal of directory
trees with glob include/exclude patterns and additional filters.

For the full reference (parameter descriptions, profile snippet, log levels,
backup behavior), see [`docs/09-Maven-plugin.md`](../docs/09-Maven-plugin.md).

## How it dispatches

Each Mojo class is a thin wrapper that:

1. Resolves `baseDir` and the effective `includes`/`excludes`.
2. Builds an optional `FlexibleUnifiedConfig` overlay from `configFile` and from
   per-run parameter overrides (`backupsEnabled`, `processingStatisticsMode`).
3. Constructs a `SrcProcessor(overlay)` and invokes `processSources(...)` with
   the goal-specific `FlowType` (`REORDER`, `CHECK_ALL`, `CHECK_FAIL_FAST`).
4. For `check-*` goals: when the result is non-successful and
   `failOnViolation=true`, throws `MojoFailureException` with the
   non-conforming file count and the hint to run `jharmonizer:reorder`.

All real work happens in [`jharmonizer-core`](../core/README.md).

## Building and testing

The standard repository build command exercises the plugin together with its
integration-test scenarios under `src/it/`:

```bash
mvn verify
```

To build and test only this module (with its in-repo dependencies):

```bash
mvn -pl maven-plugin -am verify
```
