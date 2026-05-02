<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Sorting performance benchmark (JMH)

JHarmonizer includes a JMH benchmark that measures **sorting performance in isolation** — without
parsing, serialization, or formatting in the measured time.

## What it measures

The benchmark:

- uses fixtures from `src/test/resources/test-cases/core/e2e/reorder/**/input/*.java`
  and `src/test/resources/test-cases/core/e2e/regression/**/input/*.java`;
- parses fixtures once during JMH setup;
- clones AST models and runs only `SpoonSorter.sortCompilationUnitRecursively(...)` in benchmarked operations.

## Running the benchmark

Run with defaults (4 threads, batch size 1000, warmup 5 iterations, measure 10 iterations, no forks):

```bash
mvn -pl core -Pbenchmark-sort -Dskip-quality-gates -DskipTests test-compile exec:java
```

Run with custom parameters (example: 4 threads, batch 200, warmup 1, measure 2):

```bash
mvn -pl core -Pbenchmark-sort \
  -Dskip-quality-gates -DskipTests \
  -Dbenchmark.threads=4 \
  -Dbenchmark.measurementBatchSize=200 \
  -Dbenchmark.warmupIterations=1 \
  -Dbenchmark.iterations=2 \
  test-compile exec:java
```

All commands must be run from the repository root.

## Output

- Console summary from JMH.
- Machine-readable JSON results at `core/sorting-benchmark.json`.
