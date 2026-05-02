<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Testing strategy

Tests are organized to mirror the production package layout under
`io.github.lemon_ant.jharmonizer.core.*` and are run by JUnit 5 with AssertJ for
assertions. Test conventions (naming, structure, fixtures, helpers) are documented in
[`AGENTS.md`](../AGENTS.md) and the matching Copilot instructions; the rules in this
file are deliberately scoped to which categories of tests exist and how they are
arranged.

## Categories

### Unit tests

Per-class tests under `core/src/test/java/...` (`<ProductionClassName>Test`). They
cover the building blocks in isolation:

- configuration: `config/`, `config/compiled/`, `config/input/`, `config/unified/`;
- file scanning: `files_handler/`;
- parser: `translator/spoon/`;
- sorter: `sorter/`, `sorter/spoon/`, `sorter/spoon/dependency_graph/`;
- formatter: `formatter/`;
- diff: `diff/`;
- flows: `flow/`;
- opt-out resolution: `optout/`;
- statistics: `processing_stat/`.

### End-to-end / regression tests

End-to-end tests live in `core/src/test/java/.../core/e2e/` and run the full pipeline
(`SrcProcessor.processSources(...)`) over fixtures under
`core/src/test/resources/test-cases/core/e2e/`:

- `test-cases/core/e2e/reorder/**` — happy-path reorder fixtures with
  `input/` (source) and `expected/` (post-rewrite) folders.
- `test-cases/core/e2e/regression/**` — numbered regression scenarios that pin down
  specific bugs and edge cases (for example
  `08-enum-constant-cross-type-back-reference/`).

Each scenario folder is self-contained; fixtures are loaded through shared helpers
such as `TestCaseResourceUtils` and never written back to `src/test/resources` (they
copy into a `@TempDir` first).

### Sorting benchmark

`SortingAlgorithmBenchmark` (under `core/src/test/java/.../sorter/`) is a JMH harness
exercising `SpoonSorter.sortCompilationUnitRecursively(...)` over the same E2E
fixtures. It is gated behind the `benchmark-sort` Maven profile and is not part of the
default `verify` build. See [`benchmark.md`](benchmark.md).

## Running the tests

The standard repository command runs unit and integration tests together with the rest
of the quality gates:

```bash
mvn -B -ntp verify
```

Individual modules can be tested with the usual Maven flags
(`mvn -B -ntp -pl core -am test`, etc.).

## Fixture rules

The repository-wide convention is `valid/` fixtures must compile (they are gated by
the build) and `invalid/` fixtures may intentionally fail to compile. Fixtures used
specifically by the formatter must be valid Java but **not** already formatted to the
expected output, so the assertion proves the formatter actually rewrote the file. See
the test conventions in `AGENTS.md` for details.
