<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Replacement printer assessment

The replacement has higher mean throughput and fewer allocated bytes per batch on every
measured workload/thread combination. Selected source size and Cognitive complexity also
decreased. The internal dependency graph and eight-worker scaling deteriorated; extreme
sampled delays did not improve uniformly. These are measured trade-offs, not an overall
numerical grade for the design.

Baseline: `5a121b67ae240177a250585db3e1d07abfbf42cf`; candidate:
`0f6aa72de70606b241a3e2bc11898c7fb8580199`. This compares complete delivered implementations,
including the reusable configured service. The explicit [v2 amendment](../../experiments/invocation-state-v2/README.md)
permits fresh invocation state without constructing another outer printer each call.
The numerical instrument, timed statement, corpus and settings are unchanged; both revisions
received the same protocol/JavaDoc patch in isolated checkouts.

The [generated comparison](comparison.md) contains all 20 primary comparisons and source
summaries. [Runtime details](runtime-details.md) contain every GC/pool counter, original units,
reported errors, primary intervals and all sampled percentiles. [Static inventories](static-details.md)
contain every selected file, explicit operation and source/bytecode class. Original JSON,
logs, iterations, histograms and [dependency graph](architecture.md) accompany these reports.

## Throughput

Means below are whole batches/s. `fixtures` contains 11 documents/batch; each synthetic
workload contains one. Values after `±` are JMH's reported confidence half-width.

| Workload | Old, 1 worker | New, 1 worker | New / old | Old, 8 workers | New, 8 workers | New / old |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| fixtures | 729.620 ± 233.817 | 3552.142 ± 825.855 | 4.868 | 2915.618 ± 922.579 | 11873.051 ± 1298.930 | 4.072 |
| flat128 | 4627.382 ± 599.920 | 9749.091 ± 1868.754 | 2.107 | 15721.098 ± 1493.336 | 27923.374 ± 1909.833 | 1.776 |
| flat1024 | 497.883 ± 57.000 | 988.897 ± 265.420 | 1.986 | 1740.199 ± 145.491 | 2940.858 ± 709.165 | 1.690 |
| nested128 | 362.436 ± 56.874 | 765.913 ± 140.207 | 2.113 | 1451.562 ± 128.582 | 2286.030 ± 162.299 | 1.575 |

Across all 16 throughput combinations, ratios range from 1.398 (`nested128`, four workers)
to 5.072 (`fixtures`, two workers). Reported mean intervals overlap for `nested128` at four
workers; the other throughput intervals do not overlap. This is descriptive, not a hypothesis
test. Documents/s, both intermediate worker counts and complete intervals are in the
[baseline overview](../baseline-5a121b67-v2/measurements.md), [candidate overview](measurements.md)
and runtime details. There is no aggregate speedup across differently sized workloads.

## Scaling

Ratios compare each implementation with its own one-worker mean. They are derived from
independent runs and have no inferred confidence intervals.

| Workload | Old 8/1 speedup | New 8/1 speedup | Old efficiency % | New efficiency % |
| --- | ---: | ---: | ---: | ---: |
| fixtures | 3.996 | 3.343 | 49.951 | 41.781 |
| flat128 | 3.397 | 2.864 | 42.468 | 35.803 |
| flat1024 | 3.495 | 2.974 | 43.690 | 37.173 |
| nested128 | 4.005 | 2.985 | 50.063 | 37.309 |

Eight-worker efficiency decreased for all workloads, despite higher absolute throughput.
Fixtures improve slightly at two/four workers relative to their own single-worker result;
the synthetic workloads have lower relative scaling at all measured worker counts. The
hardware has four physical cores and eight logical CPUs. This experiment does not isolate
the cause of the scaling change or establish a new contention/bandwidth bottleneck.

## Allocation and memory

Single-worker throughput allocation, in bytes per complete batch:

| Workload | Old B/batch | New B/batch | Change % |
| --- | ---: | ---: | ---: |
| fixtures | 518486.964 | 176248.892 | -66.007 |
| flat128 | 135988.099 | 116386.349 | -14.414 |
| flat1024 | 1038317.129 | 921414.694 | -11.259 |
| nested128 | 1334445.476 | 1244877.531 | -6.712 |

All 20 combinations allocate fewer bytes per batch: reductions range from 6.712% to 66.184%.
Allocation rate in MB/sec and GC count increase in all 16 throughput combinations. These
fixed-duration measurements complete more batches. GC time increases in ten and decreases
in six; these are aggregate counters, not GC cost for a fixed amount of work.

The profiler's total pool peak changes range from -1.295% to +1.543% across all 20 combinations.
There is no substantial reduction in this particular peak metric. The fixed 1 GiB heap,
prepared ASTs and JVM overhead remain part of it. Pool totals are sums of maxima that need
not occur together; they do not measure retained printer size or process RSS. Original KiB
values, all individual pools and profiler errors are retained in runtime details; overviews
convert the total to MiB. No extra total is formed by adding its subtotal rows.

The source now appends source ranges directly into a pre-sized StringBuilder and uses an
array for member-boundary lookup. It removes intermediate substrings from fragment copying,
framework character-by-character printing and enum boundary correction that invoked Spoon
printing. These are source-supported explanations for the observed gains, not isolated
causal measurements: the frozen protocol does not collect CPU or allocation stack profiles.

## Sampled service time

One worker, microseconds per whole batch, with no arrival queue. Each cell is old / new.

| Workload | p50 us | p95 us | p99 us |
| --- | ---: | ---: | ---: |
| fixtures | 1105.920 / 173.056 | 3432.448 / 501.760 | 6186.598 / 860.160 |
| flat128 | 183.296 / 83.840 | 371.200 / 238.336 | 568.822 / 472.064 |
| flat1024 | 1779.712 / 826.368 | 3552.666 / 2101.248 | 5406.720 / 3493.315 |
| nested128 | 2256.896 / 1183.744 | 4308.992 / 2662.400 | 6676.480 / 4262.871 |

Mean, p50, p95 and p99 are lower for all workloads. Mean reductions range from 41.522% to
83.451%; the `flat128` mean intervals overlap. Higher percentiles are mixed: `flat128` p99.9
increases from 1695.840 to 3006.530 us and p99.99 from 8240.520 to 20303.289 us. The maximum
for fixtures increases from 42.861 to 91.226 ms; the nested maximum increases from 42.795 to
50.921 ms. Both `flat128` runs contain a roughly 2.6-second sample (2.651 / 2.638 seconds).
Their causes were not determined. All were retained; extreme percentiles based on few
observations are not treated as stable tail-latency guarantees. Every reported sampled-time
percentile is shown in runtime details.

## Correctness and scope controls

Strict v2 comparison passed for protocol, hardware identity, JVM/runtime, external library
hashes, harness/corpus hashes, prepared-model fingerprints, exact output text and skipped
ranges. All 14 documents also match the original v1 golden artifact. Outputs compiled with
JDK 21; ten repeats and worker setup/teardown checks passed. Both captures completed all
20 combinations, with 60 JVM forks each. Logs contain no benchmark failures.

Core verification passed: 755 tests, zero failures/errors, three existing skips, successful
coverage gates and zero SpotBugs findings. The printer E2E runner's 75 cases passed. The
footer contract fix is outside the frozen performance corpus and remains covered there;
exact equality on these 14 documents is not a claim about every possible Java file.

Source/POM review found no production changes between the old archived revision and the
candidate's parent `d026ca93`. Candidate changes outside the selected printing boundary are
the production supplier's reuse of configuration and equivalent group metadata assignment.
Neither output nor AST results are cached between invocations. Workers own separate models;
the parallel benchmark does not test concurrent mutation of a shared Spoon AST.

## Scope and source size

| Role | Files, old / new | Selected physical lines, old / new | NCSS, old / new |
| --- | ---: | ---: | ---: |
| Implementation | 6 / 3 | 830 / 438 | 229 / 142 |
| Shared helpers | 2 / 3 | 96 / 175 | 29 / 49 |
| Contracts | 3 / 4 | 74 / 88 | 18 / 19 |
| Total | 11 / 10 | 1000 / 701 | 276 / 210 |

Selected physical lines decreased 29.9%; NCSS decreased 23.9%. Explicit operations decreased
from 41 to 34: methods from 36 to 29, with five constructors in both versions. Complete
source files contain 10 versus 11 declared types; the class graph has 11 versus 12 nodes,
including the partially selected shared helper. Source and bytecode type counts differ by
that explicitly partial boundary.

The new scope replaces the inherited printer, structure printer, buffer helper, printer
utilities and enum-correction helper with `SpoonSrcPrinter`, `SrcPrinterOutput` and the
remaining member utilities. It includes all of `SpoonGroupSeparatorUtils`, its nested value
type and `UnifiedSeparator`, including metadata assignment called during sorting. Moving
logic to shared code therefore does not remove it from the measurement. The ArchUnit
unscoped-project dependency audit passed.

## Operation distributions

| Metric | Old mean / p50 / p95 / max | New mean / p50 / p95 / max |
| --- | --- | --- |
| LOC | 17.195 / 14 / 38 / 43 | 13.206 / 11 / 29 / 32 |
| NCSS | 6.024 / 5 / 16 / 21 | 5.265 / 3 / 16 / 19 |
| Cyclo | 2.585 / 1 / 7 / 8 | 2.382 / 1 / 7 / 9 |
| Cognitive | 2.122 / 1 / 9 / 13 | 1.735 / 0 / 9 / 11 |
| NPath | 5.341 / 2 / 11 / 108 | 3.971 / 1 / 16 / 42 |
| Operation fan-out | 5.146 / 4 / 13 / 22 | 4.147 / 3 / 14 / 16 |
| Arity | 1.683 / 1 / 4 / 6 | 1.735 / 2 / 4 / 5 |

Summed Cyclo decreased from 106 to 81 and summed Cognitive from 87 to 59. NPath is not
summed. The distributions are mixed: maximum Cyclo, p95 NPath, p95 operation fan-out, and
mean/median arity increased. No operation exceeds the fixed Cyclo 10 or Cognitive 15 screens.

The selected AST has 20 versus 30 `if` statements, two versus four ordinary `for` loops,
two versus four `while` loops, two enhanced loops in both versions, six versus four
ternaries, 21 versus 24 short-circuit operators, and 14 versus 24 lambdas. Both versions
have one catch and no switch or do-while constructs.

`printMemberSeparator` changed from Cyclo 8, Cognitive 8, NPath 108 to 9, 6, 42. Its source
span decreased from 33 to 21 lines and NCSS from 10 to 8. Combining separator requests and
resolving group kind before printing reduces path combinations despite one additional
cyclomatic decision. The remaining largest Cognitive value is 11 in dominant line-ending
detection; that algorithm and the shared fragment scanners retain their previous values.
Removing enum correction removes `buildFirstLinePattern`, formerly Cognitive 13.

PMD reports zero findings and zero suppressed findings in both scopes. CPD reports zero
clone groups at the same 50-token threshold, over 3197 versus 2443 tokens. This does not
exclude smaller repeated fragments.

## Architecture and state

| Metric | Old | New |
| --- | ---: | ---: |
| Class-graph nodes | 11 | 12 |
| Internal edges | 15 | 18 |
| Cycles | 0 | 1 |
| Maximum internal fan-out | 7 | 10 |
| Maximum internal fan-in | 3 | 3 |
| CCD | 30 | 39 |
| ACD | 2.727273 | 3.250000 |
| RACD | 0.247934 | 0.270833 |
| NCCD | 0.909091 | 1.054054 |
| Distinct non-JDK library targets | 21 | 11 |
| Spoon library targets | 18 | 8 |
| Direct Spoon superclasses | 2 | 0 |
| All external non-Object superclasses | 2 | 1 |
| Declared bytecode fields in complete selected classes | 19 | 25 |
| Non-final fields in complete selected classes | 1 | 0 |

The internal graph deteriorated under the fixed per-class definition. `Serialization`
depends on ten internal classes. Its implicit reference to the enclosing printer creates
the only cycle, `SpoonSrcPrinter -> Serialization -> SpoonSrcPrinter`; cycle enumeration
was not truncated. A static nested serialization class receiving configuration explicitly
could remove that cycle, but this experiment does not modify the measured implementation.
The extra data and enum types also change the graph boundary; eliminating the cycle alone
would not restore all old dependency totals.

Framework coupling decreased: neither `DefaultJavaPrettyPrinter` nor `PrinterHelper` is a
superclass. The remaining external non-Object superclass is the JDK's `Enum`, used by
`UnifiedSeparator`, not Spoon printing infrastructure. Runtime dependency JARs remain the
same; fewer referenced library classes do not mean fewer bundled libraries.

The 25 bytecode fields include the enum's four fields and the inner class's
enclosing-instance reference. Final references still point to mutable buffers and maps.
The configured service retains only configuration; each invocation owns its output and
range map. The old non-final range-map handoff is gone. Partial `SpoonTypeUtils` whole-class
fields are excluded from these owned-state totals.

PMD TCC is 0 for the small entry service, 0.285714 for `Serialization`, 0.535714 for the
output buffer and 0.333333 for the group utility. The old entry and structure printers had
1.0 and 0.25. TCC observes source-visible field sharing; it is not a quality grade for small
facades, static utilities or Lombok value types. Class LOC/NCSS containing nested types are
not additive and must not be summed again.

The responsibility split is more explicit: serialization owns declaration ordering and
separator decisions, output owns source copying and line endings, and the group helper
owns metadata representation. Source-boundary policy still spans serialization, member
inspection and the shared scanners. The retained full-output and fixed-point tests remain
necessary for those interactions.


## Execution and uncertainty

The fresh baseline ran `2026-09-13T18:09:10.841021900Z` to
`2026-09-13T18:35:33.416071200Z` (26 min 22.575 s). The candidate ran
`2026-09-13T18:35:53.381706800Z` to `2026-09-13T19:01:51.948266Z`
(25 min 58.567 s). Exact commands, JDK/build/test validation, host conditions and archive
checks are in the [execution record](../../experiments/invocation-state-v2/execution.md).

This was one sequential old/new pair on the same machine, not a crossover experiment.
Ordinary desktop processes remained open; no concurrent agent-started build, test, debugger
or benchmark ran. Temperature and CPU affinity were not controlled, and absence of background
load is not proved. Fresh baseline means differ from the historical run; that context is
reported in the [baseline assessment](../baseline-5a121b67-v2/assessment.md).

Both implementations have trends or variation after the fixed warmup. For example, old
fixtures at four workers, third fork, rise from 1002.476 to 3177.806 batches/s across measured
iterations. New `flat1024` at eight workers, second fork, falls from 3189.820 to 1479.193.
The candidate's single-worker `flat128` forks also occupy different throughput levels.
The fixed protocol does not establish stationarity. No iterations were removed or warmup
settings changed to improve a result; intervals capture observed variation, not all systematic
bias. The reported ratios apply to this corpus, configuration and protocol.
