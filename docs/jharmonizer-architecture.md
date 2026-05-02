<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer architecture

JHarmonizer is split into four Maven modules:

| Module                       | Maven artifact                                          | Role                                                                                |
|------------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------|
| `dependency-aware-sorting/`  | `io.github.lemon-ant.jharmonizer:dependency-aware-sorting` | A standalone, **reusable Java library** for fast, deterministic ordering of items under group + DAG (`provider → dependent`) constraints. JHarmonizer uses it as the underlying engine for member ordering, but the library has no JHarmonizer-specific assumptions and can be consumed independently — see [`dependency-aware-sorting/README.md`](../dependency-aware-sorting/README.md). |
| `core/`                      | `jharmonizer-core`                                      | The full processing pipeline. No CLI or Maven coupling.                             |
| `cli/`                       | `jharmonizer-cli`                                       | Picocli front-end producing an executable fat JAR.                                  |
| `maven-plugin/`              | `jharmonizer-maven-plugin`                              | `@Mojo`-based wrappers (`reorder`, `check-all`, `check-fast`) over `jharmonizer-core`. |

## Top-level orchestration

`io.github.lemon_ant.jharmonizer.core.SrcProcessor` is the public entry point. Its
`processSources(baseDir, includeGlobs, excludeGlobs, FlowType)` method:

1. Compiles the active `CompiledConfig` (see [`02-Configurator.md`](02-Configurator.md)).
2. Resolves the matching files via `SrcFilesHandler`, which delegates the actual
   directory walk to [`glob-path-finder`](https://github.com/lemon-ant/glob-path-finder)
   — a small in-house library purpose-built for parallel streaming traversal of
   directory trees with glob include/exclude patterns and additional filters — and
   returns a **parallel** `Stream<Path>`.
3. Selects an `IFlow` implementation from the requested `FlowType`:
   - `REORDER` → `ReorderFlow`
   - `CHECK_ALL` → `CheckAllFlow`
   - `CHECK_FAIL_FAST` → `CheckFailFastFlow`
4. Invokes `flow.processStream(...)`, which drives the per-file pipeline and aggregates
   `FileProcessingResult`s into an `AggregatedProcessingStatistic` /
   `FlowProcessingStats`.
5. Optionally renders processing statistics via `ProcessingStatisticsPrintService`.

`AbstractOptOutFlow` is the shared base class that handles file-scope opt-out
short-circuiting (see [`docs/directives.md`](directives.md)).

## Per-file pipeline

For each source file, the flow runs the following stages in order:

```
   ┌──────────────────┐
   │  SrcFilesHandler │  scan working tree, return Stream<SrcFile>
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │   SpoonParser    │  parse to a Spoon CtCompilationUnit + SpoonAstModel snapshot
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │  Opt-out probe   │  JHarmonizerOptOutResolver: file-scope + type-scope directives
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │      Sorter      │  GroupMembersOrderer + ComparatorUtils + dependency graph
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │   Spoon printer  │  serialize the reordered AST to text (PrinterConfig)
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │    Formatter     │  Palantir java-format pass + import fixing + blank-line rules
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │   DiffReporter   │  compute diff vs. original (only when needed for the flow)
   └────────┬─────────┘
            ▼
   ┌──────────────────┐
   │   Write / Skip   │  REORDER writes (with optional .bak); CHECK flows never write
   └──────────────────┘
```

Each stage corresponds to its own document:

- Parser — [`03-Parser.md`](03-Parser.md)
- Sorter — [`04-Sorter.md`](04-Sorter.md)
- Formatter — [`05-Formatter.md`](05-Formatter.md)
- Processor (orchestration) — [`06-Processor.md`](06-Processor.md)
- DiffReporter — [`07-DiffReporter.md`](07-DiffReporter.md)
- Algorithms (sorting + dependency graph) — [`sorting-algorythm.md`](sorting-algorythm.md),
  [`declaration-order-dependencies.md`](declaration-order-dependencies.md)

## Concurrency model

- File scanning returns a parallel `Stream<Path>` (`SrcFilesHandler.findSrcFiles(...)`
  → `GlobPathFinder.findPaths(...).parallel()`).
- Each file is processed independently inside the per-file pipeline; there is no
  shared mutable state across files.
- `CHECK_FAIL_FAST` short-circuits the parallel stream as soon as one violation is
  observed (`FlowResultUtils` collaborates with `JvmShutdownSignal` to drain in flight
  work cleanly).
- Statistics are aggregated through reduction operators on the parallel stream.

## Design rationale

The pipeline is procedural inside each file (single `WorkflowRunner`-style call chain
inside the flow implementations) but uses a parallel stream at the file granularity.
This gives:

- Maximum throughput on multi-core machines without per-stage object allocation churn.
- A simple debug story per file — one straight-line execution to step through.
- Clean error boundaries — a failing file raises a runtime exception that is captured
  in its own `FileProcessingResult` without poisoning the rest of the run.
