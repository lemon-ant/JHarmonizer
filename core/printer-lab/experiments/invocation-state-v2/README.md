<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Production serialization experiment v2

Protocol identifier: `printer-lab-v2-invocation-state`.
Extends [v1](../../PROTOCOL.md) for the reusable printer introduced in `0f6aa72d`.
All v1 settings, metrics and correctness checks remain in force except the requirement
to construct a new outer printer object on every call.

## Reason and measured boundary

V1 requires fresh printer construction. The replacement retains immutable configuration in
`SpoonSrcPrinter` and constructs `Serialization`, its buffer and skipped-range map per call.
The production supplier performs serialization every time; it does not cache output.
The user requested this reusable design, so forcing another lifecycle for measurement would
change the implementation being evaluated.

V2 measures the existing production serialization supplier with fresh invocation state.
The baseline constructs its printer per call; the replacement reuses its configured service.
This compares the delivered implementations, including their allocation lifecycle. It does
not isolate inheritance removal from other internal changes.

The numerical instrument is unchanged. [protocol.patch](protocol.patch) changes only the
protocol identifier and the benchmark's JavaDoc in isolated checkouts. It changes no timed
statement, preparation step, assertion, metric, dependency, workload or JMH option. Apply the
identical patch to both implementations and retain their recorded harness hashes.
The original lab sources and the archived v1 evidence remain unchanged.

## Correctness and scope

The baseline is production revision `5a121b67ae240177a250585db3e1d07abfbf42cf`; the candidate
is `0f6aa72d`. Production sources and dependency POMs did not change between the archived
baseline and the parent of the candidate. The candidate's changes outside the printing
scope are the supplier lifecycle and the equivalent group-metadata assignment refactor.
Review these explicitly alongside the structural fingerprint checks.

The initial candidate check reproduced all 14 archived documents exactly: input hashes,
prepared-model fingerprints, output text and skipped ranges. Each output compiled with
JDK 21 and survived ten repeated serializations. The footer contract change is outside this
frozen corpus; its coverage remains in the printer E2E tests. Do not claim that the corpus
proves output identity for every source file.

Keep exact equality checks between full v2 captures. Do not normalize output, replace the
archived golden artifact or suppress a mismatch. Use the original scope manifest for the
baseline and the reviewed replacement scope for the candidate.

## Reproduction

Create detached checkouts for both production revisions under the lab's `target/`. The
baseline revision predates the lab: copy the frozen lab from `0f6aa72d`, excluding `target/`
and results, and restore `scope.tsv` from `65608f1f`. Apply `protocol.patch` at each checkout
root. Keep all production files unchanged and verify the diff before building.

Use the same Microsoft HotSpot JDK `21.0.8+9-LTS`, Maven 3.9.11 and machine as v1. From each
checkout root, sequentially:

```text
mvn -B -ntp -pl core -am install -DskipTests -Dskip-quality-gates -Dci=true
mvn -B -ntp -f core/printer-lab/pom.xml clean package
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar smoke . NEW_SMOKE_DIRECTORY
```

Run the full baseline capture, followed by the candidate capture against that completed
baseline. Run no concurrent build, test, debugger or other agent-started measurement.

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . NEW_BASELINE_DIRECTORY
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . NEW_CANDIDATE_DIRECTORY BASELINE_DIRECTORY
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar compare BASELINE_DIRECTORY CANDIDATE_DIRECTORY COMPARISON_FILE
```

The matrix remains four workloads, throughput at 1/2/4/8 workers, sampled time at one
worker, three forks, five one-second warmups and five two-second measurements: 60 JVMs
per full capture. Heap, GC, profilers, encoding, locale and result consumption remain v1.

Archive both completed captures with the existing `archive` command. Follow the v1 CPD
header-only license normalization, preserve raw observations and record exact commands,
conditions, timings, results and scope changes in the assessments. Compare all metrics;
use the fresh baseline for runtime conclusions and retain the historical numbers as context.
One sequential baseline/candidate pair does not eliminate time-order or thermal effects.

## Completed comparison

The full 2026-09-13 pair passed all 20 combinations per revision (120 forked JVMs total),
including exact corpus/output checks. See the [baseline reproduction](../../results/baseline-5a121b67-v2/assessment.md),
[replacement assessment](../../results/candidate-0f6aa72d-v2/assessment.md),
[all runtime metrics](../../results/candidate-0f6aa72d-v2/runtime-details.md),
[static inventories](../../results/candidate-0f6aa72d-v2/static-details.md) and
[execution record](execution.md). The original v1 archive is retained separately.
