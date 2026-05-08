<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer

[![Verify](https://github.com/lemon-ant/JHarmonizer/actions/workflows/verify.yml/badge.svg)](https://github.com/lemon-ant/JHarmonizer/actions/workflows/verify.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://adoptium.net/)
[![Coverage](https://img.shields.io/codecov/c/github/lemon-ant/JHarmonizer)](https://codecov.io/gh/lemon-ant/JHarmonizer)

JHarmonizer **sorts and formats Java source files** while keeping the code safe.
It reorders class members — fields, constructors, methods, nested types, initializer blocks — according to
configurable rules, and formats the output with [Palantir Java Format](https://github.com/palantir/palantir-java-format).

What sets it apart from a plain formatter: before moving any member, JHarmonizer builds a
**declaration-order dependency graph** that captures the JLS rules governing field initializer references,
static and instance block sequencing, enum constant initializers, blank-final assignment ordering, and
cross-member back-references. The sorter honours those constraints so the reordered source **compiles and
runs correctly** — not just looks different.

> **Why not just use IntelliJ, Spotless, or Checkstyle?**
> IDEs cannot run in CI, formatters do not reorder members, and checkers only flag without fixing.
> IDE arrangement rules also apply a single global layout to every class — there is no way to define
> different ordering rules for test classes, DTOs, annotated controllers, or other class-type selectors.
> JHarmonizer's selector DSL provides that flexibility out of the box.
> See [docs/motivation.md](docs/motivation.md) for the full comparison.

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
            <version>1.0.1</version>
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

`check-fast` and `check-all` default to the `validate` phase — the build fails early,
before any compilation happens:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.lemon-ant.jharmonizer</groupId>
            <artifactId>jharmonizer-maven-plugin</artifactId>
            <version>1.0.1</version>
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
```

`check-fast` immediately halts the build pipeline the moment it detects an out-of-order or unformatted file —
no further processing happens. `check-all` scans all files, collects every violation, and only then interrupts the
build with the full report; set `-Djharmonizer.failOnViolation=false` to make `check-all` report violations without
failing the build.

### Manual invocation

```bash
mvn jharmonizer:reorder      # reorder all sources
mvn jharmonizer:check-all        # report all violations
mvn jharmonizer:check-fast   # fail fast on first violation
```

### CLI

JHarmonizer is also available as a standalone CLI fat JAR for use outside of Maven.

**Download directly** (no build required):
[jharmonizer-cli-1.0.1.jar](https://repo1.maven.org/maven2/io/github/lemon-ant/jharmonizer/jharmonizer-cli/1.0.1/jharmonizer-cli-1.0.1.jar)

Browse all versions on [Maven Central](https://central.sonatype.com/artifact/io.github.lemon-ant.jharmonizer/jharmonizer-cli).

```bash
java -jar jharmonizer-cli-1.0.1.jar reorder --base-dir src/main/java
```

See [`cli/README.md`](cli/README.md) for full command-line usage, all options, exit codes, and CI integration examples.

## ⭐ Ways to support this project

I'm an open-source developer building reliable, intelligent tooling for the Java community 🛠️.

The ideas behind JHarmonizer are simple, but implementing them correctly turned out to be surprisingly hard ⚙️.
There is a large backlog of planned features — smarter ordering rules, IDE plugins, more formatting options, and
deeper static analysis integration — but delivering them takes a huge amount of time and effort ⏳.

**This project is free for you, but it is not free for me 💙.** Every hour spent here is a personal investment —
and beyond time, I also put real money into it: AI tooling, compute credits, and the infra that powers
development 💸. All of that goes toward making this tool better for you.

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
- A declaration-order dependency graph is built to avoid reorderings that would break compilation
  or field/constant initialization. The current model handles direct initializer references
  (forward and backward, including explicit `this.` / `<EnclosingType>.` qualifiers), enum
  constant initializers, instance and static initializer blocks, blank-final definite-assignment
  ordering, cross-type constant back-references, and accessor bundling.
- Comment-based opt-out directives are supported for file scope and type scope.

## Roadmap (next versions)

The five most impactful planned features for everyday Java development:

- **Enum constant ordering strategies** — configurable `PRESERVE` / `ALPHA_ASC` / `ALPHA_DESC` ordering for enum constants, with placement guarantees that keep them before other enum members.
- **Annotation ordering policies** — configurable `ALPHA` / `LENGTH_ASC` / `LENGTH_DESC` ordering for annotations on declarations, keeping annotation lists deterministic and reducing diff noise.
- **Record member ordering** — dedicated ordering strategies for record components, with safety guarantees that preserve generated member contracts and binary compatibility.
- **Vendor-format configuration adapters** — import existing IntelliJ IDEA or Eclipse formatter/arrangement profiles directly so teams do not have to duplicate their configuration in a separate `jharmonizer.yml`.
- **Git-aware changed-files processing** — limit sorting and checking to files that actually changed in Git (working tree vs. index, branch vs. merge-base), so large repositories stay fast in CI and pre-commit hooks.

The full idea backlog is significantly longer — see [docs/TODO.md](docs/TODO.md) for the complete list, ordered from the most developer-visible features to the most internal improvements.

## Opt-out directives

JHarmonizer supports two opt-out directives placed as source comments.

```java
// @jharmonizer:fully-off   // disable all harmonization for this file or type
// @jharmonizer:sort-off    // disable sorting only; formatting still runs
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

Contributions are welcome! Please read [CONTRIBUTING.md](.github/CONTRIBUTING.md) to get started.
See [CHANGELOG.md](CHANGELOG.md) for what is planned.

## License

Copyright 2026 Anton Lem

Licensed under the [Apache License, Version 2.0](LICENSE).
