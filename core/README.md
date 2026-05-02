<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# `jharmonizer-core`

Core processing engine of JHarmonizer. This module owns the full per-file pipeline
— parse, sort, serialize, format, diff, write — and is independent of any
front-end (CLI, Maven plugin). The `cli/` and `maven-plugin/` modules are thin
wrappers around the public entry points exposed here.

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Built with **Java 21** (`--release 21` for tests, `--release 17` for main classes
because Spoon 11.x requires JDK 17+ at runtime).

## Public entry point

```java
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;

new SrcProcessor()                                  // defaults only
    .processSources(
        baseDir,           // Path
        includeGlobs,      // Collection<String>
        excludeGlobs,      // Collection<String>
        FlowType.REORDER); // or CHECK_ALL / CHECK_FAIL_FAST
```

The same call drives one file or many; the `FlowType` selects the strategy:

| `FlowType`        | Effect                                                                                    |
|-------------------|-------------------------------------------------------------------------------------------|
| `REORDER`         | Rewrites Java sources in place; optional `.bak` backups.                                  |
| `CHECK_ALL`       | Read-only. Collects all non-conforming files; success only when none are found.           |
| `CHECK_FAIL_FAST` | Read-only. Stops at the first non-conforming file in the parallel stream.                 |

See [`docs/06-Processor.md`](../docs/06-Processor.md) for the full `SrcProcessor`
contract and [`docs/jharmonizer-architecture.md`](../docs/jharmonizer-architecture.md)
for the per-file pipeline diagram.

## Module layout

```
core/
├── src/main/java/io/github/lemon_ant/jharmonizer/core/
│   ├── SrcProcessor.java                            # public entry point
│   ├── config/                                      # vendor → unified → compiled config pipeline
│   ├── flow/                                        # ReorderFlow / CheckAllFlow / CheckFailFastFlow
│   ├── translator/spoon/                            # Spoon-backed parser, AST snapshot, opt-out resolver
│   ├── sorter/spoon/                                # SpoonSorter, OrderingKeyFactory, ComparatorUtils
│   ├── dependency_graph/                            # *DependencyProvider catalog + MemberDependencyGraph
│   ├── translator/spoon/printer/                    # SpoonCustomSrcPrinter (serialization)
│   ├── formatter/                                   # Palantir wrapper + import fixer + blank-line policy
│   ├── diff/                                        # DiffReporter
│   ├── files_handler/                               # SrcFilesHandler (parallel stream)
│   └── stats/                                       # processing statistics
└── src/main/resources/
    └── default-config.yml                           # embedded baseline configuration
```

## Configuration

`SrcProcessor()` (no-arg) uses the embedded `default-config.yml`. To overlay a
user-supplied configuration, build a `FlexibleUnifiedConfig` (loaded from disk
via `JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(path)`,
or from CLI/Mojo parameters via the builder) and pass it to the constructor:

```java
FlexibleUnifiedConfig overlay =
    JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configPath);
new SrcProcessor(overlay).processSources(...);
```

The constructor compiles `default-config + overlay` once and reuses the
`CompiledConfig` for every file; `SrcProcessor` instances are therefore
**stateful** (they own the compiled config and per-run helpers) but safe to use
from a single thread. The actual per-file work is parallelized internally
through the file stream.

See [`docs/02-Configurator.md`](../docs/02-Configurator.md) for the full
config layering and [`docs/config-dsl.md`](../docs/config-dsl.md) for the
YAML schema.

## Algorithm references

| Topic                                       | Document                                                                                |
|---------------------------------------------|-----------------------------------------------------------------------------------------|
| Sorting algorithm and accessor co-location  | [`docs/sorting-algorythm.md`](../docs/sorting-algorythm.md), [`docs/04-Sorter.md`](../docs/04-Sorter.md) |
| Declaration-order dependency graph          | [`docs/declaration-order-dependencies.md`](../docs/declaration-order-dependencies.md), [`docs/order-dependency-filter.md`](../docs/order-dependency-filter.md) |
| LIS-based relocation detector               | [`docs/relocation-detector.md`](../docs/relocation-detector.md)                         |
| Default member ordering shipped out of the box | [`docs/default-rule-ordering-rule.md`](../docs/default-rule-ordering-rule.md)         |
| Opt-out directives (`@jharmonizer:fully-off`) | [`docs/directives.md`](../docs/directives.md)                                          |

## Building and testing

The standard repository build command builds and tests every module:

```bash
mvn -B -ntp verify
```

To build and test only this module (with its single in-repo dependency
[`dependency-aware-sorting`](../dependency-aware-sorting/README.md)):

```bash
mvn -B -ntp -pl core -am verify
```
