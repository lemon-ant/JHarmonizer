<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Documentation Map

This repository keeps user-facing and design documentation in Markdown under the repository root, module directories, and `docs/`. PDF files under `docs/` are generated or externally maintained artifacts and are not updated by automated documentation synchronization tasks.

## Primary user documents

| Document | Purpose |
|---|---|
| `README.md` | Project overview, Maven quick start, CLI pointer, limitations, and build command. |
| `cli/README.md` | Standalone CLI build, commands, options, exit codes, and logging. |
| `docs/config-dsl.md` | Current YAML configuration syntax and merge semantics. |
| `docs/directives.md` | Supported opt-out comment directives and placement rules. |
| `docs/known-limitations.md` | Known runtime/formatter limitations and workarounds. |
| `docs/known-unhandled-patterns.md` | Dependency patterns intentionally not handled or not currently modeled. |
| `docs/09-Maven-plugin.md` | Current Maven plugin goals and parameters. |

## Component documents

| Document | Component |
|---|---|
| `docs/02-Configurator.md` | Configuration loading, overlays, and merging. |
| `docs/03-Parser.md` | Spoon parser/translator role. |
| `docs/04-Sorter.md` | Sorting responsibilities and dependency safety. |
| `docs/05-Formatter.md` | Formatter/import wrapper behavior. |
| `docs/06-Processor.md` | `SrcProcessor` flows and result semantics. |
| `docs/07-DiffReporter.md` | Check-flow diff rendering. |
| `docs/08-CliRunner.md` | CLI implementation summary. |
| `docs/jharmonizer-architecture.md` | Current high-level module architecture. |

## Planning documents

`docs/TODO.md`, `docs/test-coverage-plan.md`, `docs/sorting-algorythm.md`, and `docs/order-dependency-filter.md` describe planned, exploratory, or backlog work. They should be read as future-work notes unless they explicitly say a behavior is current.
