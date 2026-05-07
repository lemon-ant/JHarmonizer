<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Documentation map

This document indexes the project's documentation set as it actually exists on disk.

## Repository layout

```text
JHarmonizer/
├── README.md                  # Main entry point
├── CHANGELOG.md               # Release history
├── AGENTS.md                  # Repository-wide conventions for coding agents
├── .github/
│   ├── CONTRIBUTING.md        # Contributor guide and build instructions
│   ├── SECURITY.md            # Security policy and contact
│   └── CODE_OF_CONDUCT.md     # Community code of conduct
├── cli/
│   └── README.md              # CLI fat-JAR usage, options and exit codes
├── core/
│   └── README.md              # jharmonizer-core: public entry point, pipeline overview, references
├── maven-plugin/
│   └── README.md              # jharmonizer-maven-plugin: goals, parameters, dispatch
├── dependency-aware-sorting/
│   └── README.md              # SimplifiedDependencyAwareSorter library (reusable, JHarmonizer-independent)
├── quality-gates/             # Quality-gate Maven module
└── docs/
    ├── 01-Intro.md
    ├── 02-Configurator.md
    ├── 03-Parser.md
    ├── 04-Sorter.md
    ├── 05-Formatter.md
    ├── 06-Processor.md
    ├── 07-DiffReporter.md
    ├── 08-CliRunner.md
    ├── 09-Maven-plugin.md
    ├── 10-Gradle-plugin-optional.md   (not implemented)
    ├── 11-Tests.md
    ├── 12-Documentation.md            (this file)
    ├── PROJECT-WHITE-PAPER.md
    ├── benchmark.md
    ├── config-dsl.md
    ├── declaration-order-dependencies.md
    ├── default-rule-ordering-rule.md
    ├── directives.md
    ├── jharmonizer-architecture.md
    ├── known-limitations.md
    ├── known-unhandled-patterns.md
    ├── sorting-algorythm.md
    ├── test-coverage-plan.md
    ├── TODO.md                         (long-form backlog; intentions, not behaviour)
    ├── IDEA/                           (working notes)
    ├── bad-design/                     (working notes)
    ├── spoon-bugs/                     (working notes)
    └── formatter-bugs/                 (working notes)
```

## Reading order for new contributors

1. [`README.md`](../README.md) — project overview, CLI/Maven quick starts, current status.
2. [`docs/01-Intro.md`](01-Intro.md) — purpose, key advantages, top-level pipeline.
3. [`docs/jharmonizer-architecture.md`](jharmonizer-architecture.md) — module map and per-file pipeline.
4. [`docs/02-Configurator.md`](02-Configurator.md) and [`docs/config-dsl.md`](config-dsl.md) — configuration model.
5. Pipeline stage docs in order: [`03-Parser.md`](03-Parser.md), [`04-Sorter.md`](04-Sorter.md),
   [`05-Formatter.md`](05-Formatter.md), [`06-Processor.md`](06-Processor.md),
   [`07-DiffReporter.md`](07-DiffReporter.md).
6. Front-end docs: [`08-CliRunner.md`](08-CliRunner.md) and
   [`09-Maven-plugin.md`](09-Maven-plugin.md).
7. Algorithm deep-dive: [`sorting-algorythm.md`](sorting-algorythm.md),
   [`declaration-order-dependencies.md`](declaration-order-dependencies.md).
8. Limitations: [`known-limitations.md`](known-limitations.md),
   [`known-unhandled-patterns.md`](known-unhandled-patterns.md).
9. Operational topics: [`directives.md`](directives.md), [`benchmark.md`](benchmark.md),
   [`11-Tests.md`](11-Tests.md), [`test-coverage-plan.md`](test-coverage-plan.md).

## Conventions

- Every tracked text/source/config/documentation file carries the SPDX header
  `SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>` and
  `SPDX-License-Identifier: Apache-2.0`. The `LICENSE` file is the only allowed
  exception (canonical Apache-2.0 legal text).
- `docs/TODO.md` and the `docs/IDEA`, `docs/bad-design`, `docs/spoon-bugs`,
  `docs/formatter-bugs` directories are working notes / backlog and do not describe
  shipped behaviour.
- When code and a document disagree, the code is the source of truth and the document
  is updated.
