<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer

[![CI](https://github.com/lemon-ant/JHarmonizer/actions/workflows/ci.yml/badge.svg)](https://github.com/lemon-ant/JHarmonizer/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)
[![Coverage](https://img.shields.io/codecov/c/github/lemon-ant/JHarmonizer)](https://codecov.io/gh/lemon-ant/JHarmonizer)

JHarmonizer is a Java source harmonization tool that keeps class member layout deterministic and readable.
It parses Java source, resolves grouping/sorting rules, applies dependency-safe reordering, and formats output
using [Palantir Java Format](https://github.com/palantir/palantir-java-format).

## Quick Start

The primary usage pattern is to integrate JHarmonizer into the Maven build so sources are automatically
reordered and/or checked without manual invocation.

### Auto-reorder on every build

Bind the `reorder` goal to the `process-sources` phase so sources are reordered before compilation.
`process-sources` is the standard Maven phase for reformatting existing sources (used by tools such as
Spotless); it runs after `generate-sources` and before `compile`.

```xml
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

`check-fast` immediately halts the build pipeline the moment it detects an out-of-order or unformatted file —
no further processing happens. `check` scans all files, collects every violation, and only then interrupts the
build with the full report; set `-Djharmonizer.failOnViolation=false` to make `check` report violations without
failing the build.

### Manual invocation

```bash
mvn jharmonizer:reorder      # reorder all sources
mvn jharmonizer:check        # report all violations
mvn jharmonizer:check-fast   # fail fast on first violation
```

### CLI

JHarmonizer is also available as a standalone CLI fat JAR for use outside of Maven.
See [`cli/README.md`](cli/README.md) for command-line usage, all options, exit codes, and CI integration examples.

## ⭐ Ways to support this project

I'm an open-source developer building reliable, intelligent tooling for the Java community 🛠️.

JHarmonizer turned out to be **significantly more complex** than I originally anticipated ⚙️. There is a large backlog of
planned features — smarter ordering rules, IDE plugins, more formatting options, and deeper static analysis
integration — but delivering them takes a huge amount of time and effort ⏳.

**This project is free for you, but it is not free for me 💙.** Every hour spent here is an investment I make in the
hope that it saves you many more hours maintaining clean, consistent Java code.

Your support is the most direct feedback I can receive 💬 — it tells me the project matters and gives me the energy to
keep pushing forward 🚀. And every contribution comes back to you as a smarter, more capable tool that saves you even
more time with each new release.

If this project is useful to you, please consider:

- ⭐ **Star the repository** — it improves visibility and takes 2 seconds.
- ☕ **[Buy me a coffee](https://buymeacoffee.com/antonlem)** — even a small one-time contribution makes a real
  difference and keeps me motivated.
- 💖 **[GitHub Sponsors](https://github.com/sponsors/AntonLem)** — recurring sponsorship directly through GitHub
  to support ongoing development of new features.

Every donation, no matter how small, directly accelerates the roadmap 🙏. Thank you!

## Current status

- Core pipeline is available: parse → group → sort → serialize → format.
- A declaration-order dependency graph is built to ensure members are never reordered in ways that would break
  field initialization or accessor semantics; handles direct initializer references and accessor bundling.
- Comment-based opt-out directives are supported for file scope and type scope.

## Roadmap (next versions)

- Add selector matching by type (field type / method return type).
- Add explicit enum constant ordering strategies.
- Add automatic parameter-order harmonization for overriding methods and constructors: base/forwarded parameters first, extension parameters after them.
- Add redundant-modifier cleanup pass that removes semantically useless Java modifiers (for example implicit interface/class member modifiers) while preserving behavior.
- Add inter-procedural initializer dependency analysis.

The full idea backlog is significantly longer — see [docs/TODO.md](docs/TODO.md) for the complete list.

## Opt-out directives

JHarmonizer supports two opt-out directives placed as source comments.

To disable all harmonization for a file or type:

```java
// @jharmonizer:fully-off
```

To disable sorting only (formatting still runs):

```java
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

## 📖 Documentation

- [Javadoc (latest)](https://javadoc.io/doc/io.github.lemon-ant/jharmonizer-core)

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) to get started.
See [CHANGELOG.md](CHANGELOG.md) for what is planned.

## License

Copyright 2026 Anton Lem

Licensed under the [Apache License, Version 2.0](LICENSE).
