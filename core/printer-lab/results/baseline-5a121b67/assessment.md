<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Baseline assessment: inherited Spoon printer

Production revision: `5a121b67ae240177a250585db3e1d07abfbf42cf`.
Protocol: [printer-lab-v1](../../PROTOCOL.md). The printer was not changed for this experiment.
Measured values are in [measurements.md](measurements.md); original operation-level values,
dependency edges, JMH iterations and sample histograms accompany this assessment.

The owned implementation stays below the chosen per-method inspection thresholds and has
no internal class dependency cycles. The main maintenance concerns visible in this review
are separator combinations, source/comment boundary corrections and implicit Spoon state.
Those concerns provide concrete comparison targets for the replacement; low totals alone
do not establish that the current design is easy to maintain.

## Source size and branching

| Boundary role | Files | Selected physical lines | NCSS |
| --- | ---: | ---: | ---: |
| Printer implementation | 6 | 830 | 229 |
| Shared printing helpers | 2 | 96 | 29 |
| Configuration and result contracts | 3 | 74 | 18 |
| Total | 11 | 1000 | 276 |

There are 41 explicitly declared operations: 36 methods and five constructors. The complete
files contain ten declared types; the eleventh file contributes two selected shared methods.
Physical lines include comments, documentation, imports and whitespace. NCSS is a separate
PMD statement metric, not an alternative count of text lines.

The selected AST contains 30 `if` statements, four ternaries, four `for` loops, two enhanced
`for` loops, four `while` loops, one `catch`, 24 short-circuit operators and 24 lambdas.
There are no switch statements/expressions or do-while loops. These source constructs are
not JaCoCo bytecode branch outcomes; do not compare their totals as equivalent metrics.

| Per-operation metric | Median | p95 | Maximum |
| --- | ---: | ---: | ---: |
| Cyclomatic complexity | 1 | 7 | 8 |
| Cognitive complexity | 1 | 9 | 13 |
| NPath | 2 | 11 | 108 |
| LOC | 14 | 38 | 43 |
| NCSS | 5 | 16 | 21 |
| Parameter count | 1 | 4 | 6 |

No operation exceeds Cyclo 10 or Cognitive 15. The fixed PMD maintainability screens report
zero findings and zero suppressed findings in the analyzed files. CPD reports zero clone
groups at 50 tokens, over 3197 selected Java tokens. Smaller repetition can still exist.

## Where understanding and maintenance cost concentrate

The following interpretations are based on the measured declarations and their code; they
are not predictions of maintenance hours or an overall quality score.

- `EnumMemberStartCorrectionResolver.buildFirstLinePattern` has the largest Cognitive value,
  13, with Cyclo 7 and 21 NCSS. The resolver totals 22 Cognitive points across four methods.
  Nested whitespace scanning and context-dependent regex construction deserve attention
  when changing enum boundary correction. `resolveCorrectedStarts` also invokes Spoon's
  `CtTypeMember.toString()` before matching original text; its own source size hides called
  framework printing work, which remains included in runtime measurement.
- `SpoonSrcPrinterUtils.detectDominantLineSeparator` has Cognitive 11 and Cyclo 7. Its utility
  class totals 20 Cognitive points. LF/CRLF/CR selection and tie-breaking are localized, but
  rely on several interacting counters and conditions. Preserve the existing separator tests.
- `SrcCodeUtils.findFragmentStartWithIndentation` has Cognitive 9 and Cyclo 6. Correctness
  depends on inclusive/exclusive source boundaries and preserving indentation while removing
  whitespace gaps. Its shared location is included in the scope so extraction cannot hide cost.
- `SpoonTypeStructurePrinter.printMemberSeparator` has the largest Cyclo, 8, and NPath, 108,
  despite Cognitive 8. It combines previous-member, current-member, group-header, first-member
  and comment conditions. This is the clearest concentration of combinations to preserve in
  tests; modest nesting alone does not describe its behavioral surface.

These four methods account for 41 of the 87 summed Cognitive points. Source metrics therefore
locate particular algorithms and decision combinations instead of suggesting uniformly complex
code throughout the printer.

Following separator behavior requires reading `printTypeMembers`, `printMemberSeparator`,
the predicates compiled in `SpoonSrcPrinterUtils`, and the comment filters in
`SpoonTypeMemberUtils`. Fragment boundaries add `SrcCodeUtils`, with a separate enum-specific
correction. This split separates distinct concerns, but changes to one boundary policy can
still require understanding several files. The future review should check whether those
relationships become more explicit, alongside changes in the measured dependency graph.

## Architecture and state

The measured class graph has 11 nodes, 15 distinct internal edges, and no cycles. ArchUnit
reports CCD 30, ACD 2.727273, RACD 0.247934 and NCCD 0.909091. See the
[dependency graph](architecture.md) and [full inventory](architecture.json).

`SpoonCustomSrcPrinter` has six outgoing internal targets and `SpoonTypeStructurePrinter`
has seven. Together they originate 13 of the 15 internal edges. `PrinterConfig` has the largest
internal fan-in, three. The entry printer routes declaration kinds and owns root separators;
the structure printer owns nested/member layout and fragment/range handling. This is
concentrated coordination with one-way helper dependencies. This is a class dependency graph;
same-class recursion and callbacks through inherited framework code are outside that cycle
count. The graph cannot prove that responsibility boundaries are easy to change.

The structure printer is the largest file: 268 physical lines, 81 NCSS, nine operations and
WMC 28. It contains seven final fields and one non-final range-map field. Handing off the
immutable range view sets that field to null, making printer reuse invalid. The benchmark
therefore constructs a new printer each call and keeps models private to each worker.

There are two direct external non-`Object` superclasses: `DefaultJavaPrettyPrinter` and
`PrinterHelper`. The latter supplies the mutable `sbf` buffer accessed by our helper; zero
fields declared in `SpoonPrinterHelper` does not mean that printing has no mutable state.
The custom printer installs `DefaultTokenWriter` and relies on inherited scanning, comment
printing, environment and line-separator behavior. None of those inherited bodies is counted
in our 1000 lines. Removing inheritance can increase our source size while making these
dependencies more explicit.

The scope has 21 distinct non-JDK library targets: 18 Spoon types, two Lombok annotations and
Apache Commons `Validate`. These are dependency targets, not 21 runtime libraries. Primitive
array components are excluded. Full-class inventories in the partially selected `SpoonTypeUtils`
row are labeled accordingly and are not counted as owned printing state.

PMD TCC is 1.0 for the entry printer and 0.25 for the structure printer, and zero for the other
complete types. These values depend on direct field sharing and source-visible declarations.
They provide little evidence against stateless utilities, thin inherited wrappers or Lombok
value types; interpreting every zero as poor cohesion would be misleading.

## Correctness context

The original printer behavior, including its output contracts and known limitations, is the
reference for this experiment. The source metrics above do not grade its functional correctness.

The baseline's normal JDK 21 reactor verification passed before measurement. Existing evidence
includes 29 isolated printer E2E scenarios and six `SpoonPrinterHelperTest` cases. See the
[separate coverage protocol](../../../../docs/printer-coverage.md); its scope and JaCoCo
denominator differ from this source-metric experiment.

The known [trailing top-level comment defect](../../../../docs/known-limitations.md#comments-after-the-last-top-level-type)
remains relevant. The performance corpus samples declarations, comments, opt-outs, enum
correction, line endings and large flat/nested structures; it is not the entire regression
suite. Stable, compilable output on this corpus does not prove correctness for every input.

## Measurement conditions and reproduction

The host is an Intel Core i7-6700HQ at nominal 2.60 GHz, four physical cores/eight logical
processors, with 17036767232 bytes visible physical memory. Windows 11 Pro reports build
10.0.26100 through the OS inventory; Java exposes the coarser OS version `10.0`.
The active Windows plan is Balanced (`381b4222-f694-41f0-9685-ff5bb260df2e`); the battery
reported AC power and 100% charge. This is a developer laptop, without CPU affinity or a
controlled thermal chamber. The IDE and ordinary background applications remained open.
No agent-started build, test run or debugger ran concurrently with accepted timing.

The runtime is Microsoft OpenJDK HotSpot `21.0.8+9-LTS`. Maven is 3.9.11. The host JVM uses
`-Xms128m -Xmx512m`; forks use the fixed 1 GiB G1 settings in the protocol. The inherited
`JAVA_TOOL_OPTIONS` contains only `-Dfile.encoding=UTF-8`; `JDK_JAVA_OPTIONS` is empty.
See `environment.json` for actual timestamps, free memory and every runtime-library hash.

Commands, from the repository root with JDK 21 selected:

```text
mvn -B -ntp -pl core -am install -DskipTests -Dskip-quality-gates
mvn -B -ntp -o -f core/printer-lab/pom.xml verify
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . core/printer-lab/target/baseline-final-run
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar archive core/printer-lab/target/baseline-final-run core/printer-lab/results/baseline-5a121b67
mvn -B -ntp -o -pl core -am -Dci=true license:check
```

The offline lab build reused already downloaded pinned dependencies. When network access is
needed, Maven uses normal trusted certificates; no TLS verification was disabled. Future
artifact preparation should use the documented `-Dci=true` variant to avoid incidental source
formatting. Core source and POMs remained identical to the measured commit.

The final architecture/source outputs were checked for deterministic repetition. The real
workload smoke exercise checked concurrent printer calls and output compilation; diagnostic
and interrupted development runs remain scratch artifacts and are excluded from this archive.

## Throughput, allocation and scaling

One operation prints an entire workload batch: 11 documents for `fixtures`, one generated
document for each other workload. These are mean batches/second, not lines/second.

| Workload | 1 worker | 2 workers | 4 workers | 8 workers | 8/1 speedup | 8-worker efficiency |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| fixtures | 773.78 | 1587.86 | 2448.62 | 3470.40 | 4.485 | 56.06% |
| flat128 | 4690.46 | 9793.11 | 13950.34 | 17832.05 | 3.802 | 47.52% |
| flat1024 | 574.39 | 1110.35 | 1654.56 | 1738.86 | 3.027 | 37.84% |
| nested128 | 446.71 | 806.96 | 1292.53 | 1735.27 | 3.885 | 48.56% |

The fixture batch corresponds to approximately 8511.5 documents/s at one worker and
38174.4 documents/s at eight workers. Mean throughput rises across these worker counts,
but the 4-to-8-worker change for `flat1024` is only about 5.1%; its intervals overlap widely.
This is evidence of limited additional throughput in this run, not proof of a particular
hardware or printer bottleneck. Speedup and efficiency are ratios of independently measured
means, without a derived confidence interval for the ratios.

| Workload | Allocated bytes/batch, 1 worker | Allocated bytes/batch, 8 workers |
| --- | ---: | ---: |
| fixtures | 518900 | 521636 |
| flat128 | 134635 | 135907 |
| flat1024 | 1038115 | 1074490 |
| nested128 | 1338442 | 1348337 |

Allocation per batch changes by at most about 3.5% between one and eight workers in these
means. The larger flat source allocates roughly 7.7 times as much as the 128-field source
at one worker, for eight times as many fields. Two sizes do not establish asymptotic complexity.
The nested workload has 1024 fields plus 128 nested declarations and allocates about 1.29
times the flat1024 batch at one worker. These are complete printing costs, including called
library code, allocation of output/range objects and small harness/background contributions.

The sum of pool peaks is about 671–677 MiB at one worker and 684–711 MiB at eight workers.
It includes prepared models, heap, class metadata and compiled code; the individual peaks
need not occur at the same instant. It is not a retained-size estimate for our printer.

Performance precision is limited on this developer laptop. JMH's reported 99.9% confidence
half-width for fixture throughput is 254.41 batches/s at one worker (32.9% of the mean), and
1400.12 batches/s at eight workers (40.3%). Some one-worker fixture iteration series still
rise during measurement; stationarity after the fixed warmup is not established. Background
load, compilation and thermal effects were not separately attributed. All observations are
retained, including slow iterations and forks.

Treat this as the recorded baseline under the fixed v1 protocol, with those uncertainty
limits. For a decision about a small speed difference, repeat both implementations in
alternating order under controlled current conditions. Do not compare a carefully warmed,
quiet candidate run against these historical means and attribute the entire difference to code.

## Sampled time and completed run

| Workload | Batch p50, microseconds | Batch p95, microseconds | Batch p99, microseconds |
| --- | ---: | ---: | ---: |
| fixtures | 1122.304 | 3121.152 | 5116.723 |
| flat128 | 169.472 | 333.312 | 479.432 |
| flat1024 | 1581.056 | 3104.768 | 4702.208 |
| nested128 | 2146.304 | 4218.061 | 8061.256 |

These are sampled service times of whole batches with one worker and no arrival queue.
They are separate runs from the throughput measurements; do not invert one mean to manufacture
the other's percentiles. The original histograms and profiler counters are in `sample-t1.json`.

The accepted capture ran from `2026-09-12T16:35:33.908788200Z` to
`2026-09-12T17:00:37.616856400Z`: **25 minutes 3.708 seconds**, including source analysis
and initial workload preparation. Free physical memory at capture start was 4232384512 bytes.
All 60 forks completed. The 20-combination completeness check passed, all printed workload
files compiled, and all per-worker pre/post-trial stability checks passed. Self-comparison
produced zero performance deltas and identical source/architecture values.

CLI checks also confirmed that changing the protocol identifier rejects comparison and that
a diagnostic smoke run cannot be archived as a complete baseline. All normalized harness
hashes still matched the recorded capture after documentation was completed. Standalone
Maven verification and Palantir formatting checks passed on JDK 21.
CPD's SPDX comment was expanded to the repository's multiline form before the final license
check. The XML declaration and report payload were compared with the original scratch report;
no metric or performance observation was edited. Follow README's header-only normalization,
which avoids the repository formatter's compact-comment delimiter problem.
