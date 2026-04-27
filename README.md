<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer

[![CI](https://github.com/lemon-ant/JHarmonizer/actions/workflows/ci.yml/badge.svg)](https://github.com/lemon-ant/JHarmonizer/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)

JHarmonizer is a Java source harmonization tool that keeps class member layout deterministic and readable.
It parses Java source, resolves grouping/sorting rules, applies dependency-safe reordering, and formats output.

---

## Quick Start

The primary usage pattern is to integrate JHarmonizer into the Maven build so sources are automatically
reordered and/or checked without manual invocation.

### Auto-reorder on every build

Bind the `reorder` goal to the `generate-sources` phase so sources are reordered before compilation:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.lemon-ant.jharmonizer</groupId>
            <artifactId>jharmonizer-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>reorder</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Enforce order in CI

Bind `check-fast` to the `verify` phase to fail the build on the first out-of-order file:

```xml
<build>
    <plugins>
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
    </plugins>
</build>
```

`check-fast` exits non-zero on the first violation. `check` reports all violations and fails the build when violations are found; set `-Djharmonizer.failOnViolation=false` to make `check` non-failing.

### Manual invocation

```bash
mvn jharmonizer:reorder      # reorder all sources
mvn jharmonizer:check        # report all violations
mvn jharmonizer:check-fast   # fail fast on first violation
```

### CLI

JHarmonizer is also available as a standalone CLI fat JAR for use outside of Maven.
See [`cli/README.md`](cli/README.md) for command-line usage, all options, exit codes, and CI integration examples.

---

## Current status

- Core pipeline is available: parse → classify → sort → print → format.
- Declaration-order dependency graph is implemented for direct initializer/read-write scenarios and accessor bundling.
- Comment-based opt-out directives are supported for file scope and type scope.
- Advanced inter-procedural dependency tracing for field initializers through called methods is **not implemented yet**.

## Roadmap (next versions)

- Compile sorting behavior once per group and precompute reusable sort keys in member descriptors.
- Add selector matching by type (field type / method return type).
- Add explicit enum constant ordering strategies.
- Add support for corner-cases with explicit declaring-type instance forward references during class initialization (currently parked as known failing E2E scenario).
- Add automatic parameter-order harmonization for overriding methods and constructors: base/forwarded parameters first, extension parameters after them.
- Add redundant-modifier cleanup pass that removes semantically useless Java modifiers (for example implicit interface/class member modifiers) while preserving behavior.
- Add inter-procedural initializer dependency analysis.

Details and implementation notes are tracked in [docs/TODO.md](docs/TODO.md).

## Opt-out directives

JHarmonizer supports two opt-out directives placed as source comments:

- `@jharmonizer:fully-off` — disable all harmonization for the file or type
- `@jharmonizer:sort-off` — disable sorting only; formatting still runs

```java
// @jharmonizer:fully-off
// @jharmonizer:sort-off
```

Directive matching is case-insensitive. Both line (`//`) and block (`/* */`) comment forms are supported.
Directives can be placed at **file scope** (in the compilation-unit preamble) or **type scope** (immediately
before a type declaration).

For the full reference — placement rules, scope semantics, and unsupported tokens — see [`docs/directives.md`](docs/directives.md).

## Known limitations

- **Non-idempotent formatter output:** Palantir formatter can produce slightly different results across
  consecutive runs for some long or heavily wrapped constructs. This is an upstream issue unrelated to JHarmonizer.
- **Maven archetype template files:** files containing `${package}` or similar placeholders are not valid
  Java source and are skipped with a warning.

See [`docs/known-limitations.md`](docs/known-limitations.md) for details and workarounds.

## Build

```bash
mvn clean verify
```

## Sorting performance benchmark (JMH)

JHarmonizer includes a JMH benchmark that measures the sorting algorithm in isolation (without parsing,
serialization, or formatting overhead). Activate with the `benchmark-sort` Maven profile.

See [`docs/benchmark.md`](docs/benchmark.md) for usage instructions and output format.

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) to get started.
See [CHANGELOG.md](CHANGELOG.md) for what is planned.

## License

Copyright 2026 Anton Lem

Licensed under the [Apache License, Version 2.0](LICENSE).
