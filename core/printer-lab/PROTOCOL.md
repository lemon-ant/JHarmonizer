<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer experiment protocol v1

## Question and boundaries

Compare the maintenance cost and runtime behavior of two implementations that serialize the
same prepared Spoon models to the same source text and skipped-type ranges.

`scope.tsv` identifies handwritten production source. Include printer coordination, layout,
fragment slicing, enum correction, comment/member inspection, configuration and output value
objects. Count all local helper code wherever it is placed. `SpoonTypeUtils` contributes only
`getRootTypes` and `hasNoDeclaredTypes`; its unrelated parsing/sorting helpers are excluded.
All three methods of the shared `SrcCodeUtils` belong to the printing boundary.

Exclude test code, this lab, parser/model preparation, sorter, formatter, relocation/diff
reporting, and inherited or called third-party implementations. The scope is a semantic
boundary, not a frozen list of class names: after a rewrite, update it to include replacement
classes and every extracted local helper. The dependency audit rejects outgoing references
to unmeasured project classes. Review constant inlining and partial-helper boundaries manually.

Physical and PMD source metrics analyze source before Lombok expansion. The bytecode inventory
separately includes compiler/Lombok-generated members of our classes. Neither counts inherited
method bodies. Runtime measurements necessarily include third-party work invoked during
serialization: that work contributes to the actual cost of the implementation.

## Source metrics

Use PMD **7.24.0**, Java language level **21**, default metric options, no incremental cache.
`SourceMetrics` calls PMD's public `JavaMetrics` API; it does not implement complexity algorithms.

| Measurement | Exact interpretation |
| --- | --- |
| Physical file lines | `String.lines().count()` after CRLF normalization; includes imports, comments and blank lines. |
| Selected physical lines | Whole-file count for full entries; sum of PMD method spans for selected shared methods. This mixed boundary is explicit, not a claim of comment-free LOC. |
| PMD LOC | Inclusive source span of the declared class or operation, including comments and whitespace within that span. |
| NCSS | PMD non-commenting source statements, default options: counts declarations/statements, excludes comments, whitespace, package and import declarations. |
| Cyclo | PMD cyclomatic complexity, including its default treatment of Boolean paths. Report per operation and sum over selected declarations. |
| Cognitive | PMD Cognitive Complexity per declared method/constructor; report its distribution and sum. |
| NPath | PMD `NPATH_COMP`, the current control-flow-based acyclic-path metric, not deprecated `NPATH`. Do not sum NPath across methods. |
| WMC | PMD sum of operation cyclomatic complexity for a complete class. |
| Fan-out | PMD `FAN_OUT` with default options. This source metric is distinct from the unique bytecode dependency targets below. |
| TCC | PMD direct field-sharing cohesion. Treat values for stateless utilities, tiny classes and Lombok data classes as context, not a grade. |
| Arity | Number of explicitly declared parameters. |
| AST inventory | Counts of PMD node kinds in the selected source regions, including lambda bodies and nested declarations exactly once per selected file region. |
| Short-circuit operators | Counts of `ASTInfixExpression` nodes with `CONDITIONAL_AND` or `CONDITIONAL_OR`. |
| Method-call names | Histogram of `ASTMethodCall.getMethodName()` in the same regions; syntactic call sites, not dynamic dispatch counts. |

Methods/constructors are inventoried once by their declaring type. Class LOC/NCSS may contain
nested types, so do not sum all class rows. The selected NCSS total comes from each compilation
unit once, plus explicitly selected shared methods. Method LOC measures spans and is not
additive with class/file LOC. The primary source-size total includes configuration/value types;
the `role` fields allow implementation, shared support and contracts to be inspected separately.

For each operation metric report the mean, nearest-rank p50/p95 and maximum. Nearest-rank
percentile is the sorted value at index `ceil(p * N) - 1`. Preserve the individual operation
rows so another agent can check aggregates and locate outliers.

Static path metrics do not prove that every counted path is feasible, and do not prescribe
an exact number of tests. Use the existing JaCoCo coverage protocol separately when assessing
test coverage; its bytecode branches include different constructs and generated checks.

Use `TextDocument.toLocation(node.getTextRegion())` for full declaration boundaries, including
attached JavaDoc where PMD includes it. `getBeginLine()`/`getEndLine()` use report locations;
for Java methods these identify the name, not the body. Partial-helper extraction, AST inventory
and bytecode line selection therefore use the full text region.

`maintainability-rules.xml` freezes PMD diagnostic screens: Cyclo method/class thresholds
10/80, Cognitive 15, NPath 200, object coupling 20 and parameter count 10. These are inspection
triggers, not pass/fail quality gates. PMD reports equality at its rule threshold where defined;
the summary's explicitly named `Over10`/`Over15` counts use strict `>` and may differ.
Existing PMD suppressions remain visible as a count for analyzed files; independent numeric
metrics are still collected. Shared-helper findings outside the selected methods are filtered.

CPD uses PMD 7.24.0, minimum **50 tokens**, exact identifiers/literals/annotations and default
Java tokenization. It sees whole selected files and line-preserving excerpts of partial helpers.
Keep its original XML, token count and clone-group count. A clone group is not automatically
a defect; do not claim that zero groups means no smaller repetition exists.
For a committed archive, expand the added SPDX comment to the exact multiline form shown in
README, leaving the XML declaration and PMD report payload unchanged, then run `license:check`.
The configured `license:format` delimiter scanner consumes the remaining XML for the compact
exported comment; do not use it for this normalization. This metadata-only editorial step
is outside metric computation and satisfies the repository's XML header style.

Algorithm reference: [PMD 7.24.0 JavaMetrics](https://github.com/pmd/pmd/blob/pmd_releases/7.24.0/pmd-java/src/main/java/net/sourceforge/pmd/lang/java/metrics/JavaMetrics.java).

## Architecture metrics

Use ArchUnit **1.5.0** on `core/target/classes`, with missing-dependency resolution disabled.
Import our compiled classes, select the scope and its nested classes, and inspect only direct
dependencies originating there. No library class body enters the analysis.

For a partial shared helper, retain dependencies whose bytecode source line lies inside a
selected operation. Dependencies without a source line cannot be assigned to such a method.
For complete classes, retain field/signature/annotation/inheritance dependencies as well as
method and constructor accesses. Normalize array targets to their base component type and
exclude primitive targets; `char[]` must not introduce a fictitious external library class.

- Internal fan-out/fan-in: distinct other selected classes with outgoing/incoming edges.
- External targets: distinct third-party class names, separate from JDK and project targets.
- Inheritance: direct superclass and whether it lies outside the selected code and is not `Object`.
- State surface: declared fields and non-final fields. A final collection may still be mutable;
  the metric is an inventory, not a proof of immutability. Partial-helper whole-class inventories
  are explicitly labeled and must not be folded into selected source totals.
- Cycles: ArchUnit `CycleDetector` on selected class nodes and deduplicated directed edges;
  report whether enumeration was truncated.
- Lakos metrics from ArchUnit: CCD, ACD, RACD and NCCD on the same graph, one selected class per
  component. CCD counts transitively reachable components including self; ACD divides by the
  component count, RACD divides once more, and NCCD uses ArchUnit's balanced-tree normalization.

Bytecode dependency edges omit inlined constants and source-only annotations. These metrics
describe the chosen printer boundary; they are not whole-application coupling measures.
Graphs, individual edges and source positions are retained to make the boundary reviewable.
[ArchUnit metrics and dependency model](https://www.archunit.org/userguide/html/000_Index.html#_software_architecture_metrics).

## Correctness and workload preparation

1. Require JDK 21, committed production inputs, and an exact byte match between the core JAR
   loaded by the lab and the freshly built core JAR. Record revision, library hashes and protocol hashes.
2. Copy the 11 entries of `corpus.tsv` into the scratch run. Normalize input line endings and
   then apply the manifest's LF/CRLF/CR variant. Never modify repository fixtures.
3. Generate `flat128`, `flat1024` and `nested128` from frozen templates. Flat cases contain
   respectively 128/1024 initialized fields in reverse order. The nested case has 128 nested
   classes, each with eight fields: 1024 fields plus 128 nested declarations.
4. Load the real embedded default sorting configuration, parse with `SpoonParser`, resolve
   opt-outs through production parsing, and sort with `SpoonSorter` once during setup.
5. Use `PrinterConfig(true, true, true)` for every workload. Configuration factorial exploration
   is outside v1; these flags deliberately exercise separator and comment handling.
6. Capture source/output SHA-256, exact output text, skipped-range offsets and fragment hashes,
   plus a fingerprint of type names, member classes/names/order and source positions.
7. Compile each printed file separately with `javac --release 21 -proc:none`. Check ten repeated
   serializations against the frozen text/ranges and structural fingerprint.

Every JMH worker creates its own models in `@Setup(Level.Trial)`. It validates them against
the frozen golden artifact before timing. `@TearDown(Level.Trial)` repeats the same stability
checks. Models are never shared between workers. The fingerprint checks specified structural
invariants; it is not a complete proof that every internal Spoon field stays unchanged.

## Timed boundary and fixed JMH matrix

The timed operation loops once through a workload and calls the model's serialization supplier
for each document. In the baseline this factory creates a **new printer per call**. It includes
factory dispatch and its no-declared-types guard; all inputs are checked to contain types.
Each complete result is passed to JMH's `Blackhole`. There is no checksum traversal in the
timed method and no possibility of omitting printing because its output is unused.

Parsing, file I/O, sorting, AST cloning, output validation/compilation, stats/logging wrappers,
Palantir formatting and import cleanup are outside the timed operation. Input models are
already prepared and warmed; this measures repeated serialization, not cold application startup.
Preserve fresh printer construction after a rewrite; do not turn the benchmark into a lookup
of a previously cached serialized result.

| Setting | Frozen value |
| --- | --- |
| JMH | 1.37 |
| Workloads | `fixtures`, `flat128`, `flat1024`, `nested128` |
| Throughput workers | 1, 2, 4, 8, sequential runs |
| Sample-time workers | 1, after throughput runs |
| Forks | 3 separate JVMs per combination |
| Warmup | 5 iterations × 1 second |
| Measurement | 5 iterations × 2 seconds |
| Fork heap / GC | `-Xms1g -Xmx1g -XX:+UseG1GC` |
| Locale / encoding | English / US / UTC / UTF-8 |
| Profilers | JMH `GCProfiler`, `MemPoolProfiler` |
| Forced GC | disabled |
| Instrumentation | no debugger, JaCoCo or other injected agents |
| Result consumption | one `Blackhole.consume(result)` per serialized document |

There are 60 forked JVMs in a complete capture. Preserve raw iteration values, confidence
intervals and console logs. Smoke uses fewer iterations/forks and is explicitly rejected as
a comparison baseline. Do not change run length selectively to make one implementation win.

The primary throughput unit is **whole workload batches/second**, aggregated across workers.
`fixtures` has 11 documents/batch; synthetic workloads have one. Documents/second is derived
by multiplying by that count. Do not compare different workload means as though they were
the same-sized document. Scaling is throughput(T)/throughput(1); efficiency divides it by T.
Report sampled service-time quantiles in microseconds per whole batch. They do not include
an arrival queue and are not end-to-end request-latency percentiles.

## Memory and uncertainty

`gc.alloc.rate.norm` is the primary allocation metric, in bytes per complete batch. Also retain
allocation rate, GC counts/time and all original profiler counters. The profiler covers the
measurement JVM; small harness/background allocations can remain. It does not measure retained
printer size or OS resident memory. See [JMH profiler examples](https://github.com/openjdk/jmh/blob/1.37/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java).

`MemPoolProfiler` resets pool peaks before each iteration and reports maximum used bytes for
each pool, with maximum aggregation. `mempool.total.used` is a **sum of pool peaks**, potentially
from different moments; it includes prepared ASTs, metadata and code caches. It is neither
RSS nor a simultaneous process high-water mark. A 1 GiB fixed heap can make these values similar
even when allocation rates differ substantially. [JMH 1.37 implementation](https://github.com/openjdk/jmh/blob/1.37/jmh-core/src/main/java/org/openjdk/jmh/profile/MemPoolProfiler.java).

Use an idle machine on external power with the same power mode, JDK, GC, heap and dependencies.
The tool records machine identity as a hash, logical CPUs, physical/free memory and runtime
settings. It cannot prove the absence of competing processes or thermal throttling; record
those conditions in the assessment. Avoid concurrent Maven builds, tests or debugger runs
during timing. Multiple forks and reported intervals quantify observed variation, not every
source of systematic bias. CI overlap is descriptive, not a hypothesis test.
V1 does not automatically establish stationarity after warmup. Inspect the saved iteration
series for remaining trends before treating the means as steady-state performance.

## Interpretation

There is no validated scalar measure of code beauty or maintenance hours here. Assess size,
outliers, duplication, dependency concentration, cycles, inherited state, contracts and runtime
cost together. More handwritten lines after removing inheritance can accompany a simpler
mental model; fewer lines can hide framework coupling. TCC penalizes stateless utility shapes
for reasons unrelated to cohesion of behavior. Name the methods and dependencies supporting
each architectural conclusion, and keep judgment separate from measured values.

The future comparison must preserve correctness and workload identity first. Use the exact
same protocol for both revisions. Prefer fresh baseline/candidate runs in alternating order on
the same machine if a performance difference will guide a decision; a historical baseline alone
does not control changes in host load, thermal state or operating-system behavior.
