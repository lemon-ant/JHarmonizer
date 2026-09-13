<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Baseline reproduction under protocol v2

Production revision: `5a121b67ae240177a250585db3e1d07abfbf42cf`.
Protocol: [printer-lab-v2-invocation-state](../../experiments/invocation-state-v2/README.md).
The original [v1 archive](../baseline-5a121b67/assessment.md) is unchanged.

This is a fresh complete capture of the same inherited printer, built in a detached
checkout. The identical two-line protocol/JavaDoc patch used for the candidate changes no
numerical measurement or timed statement. All 20 combinations completed: 60 forked JVMs,
three forks per combination, five one-second warmups and five two-second measurements.
See [measurements.md](measurements.md) and the original JMH JSON/logs in this directory.

## Reproduction and validation

The initial v2 diagnostic reproduced the v1 source metrics, architecture, CPD summary and
golden artifact without a Git diff. Full capture and diagnostic SHA-256 values then matched
for source metrics, architecture, CPD summary, PMD findings and golden output. This verifies
repeatability of the static instrument; diagnostic timings are excluded from the comparison.

All 14 input/model/output/range contracts matched the archived corpus. Outputs compiled
with JDK 21 and passed ten repeat serializations. Worker setup and teardown checks passed
throughout the full run. The inherited production source and dependency POMs are unchanged
between this revision and the candidate's parent `d026ca93`.

The static conclusions of the original assessment remain applicable: 1000 selected lines,
276 NCSS, 41 explicit operations, summed Cyclo 106, summed Cognitive 87, no PMD findings,
no CPD groups at 50 tokens, and no internal class cycles. Detailed old/new inventories and
judgment are in the [candidate assessment](../candidate-0f6aa72d-v2/assessment.md).

## Execution conditions

Capture started `2026-09-13T18:09:10.841021900Z` and completed
`2026-09-13T18:35:33.416071200Z`: 26 min 22.575 s, including preparation.
The candidate was started afterwards, not concurrently. The host was the same i7-6700HQ
machine as the v1 capture, with four physical cores, eight logical CPUs, Windows 11 and
Microsoft HotSpot 21.0.8+9-LTS. The power plan was Balanced; external power was available
and battery charge was 100%. Physical memory was 17036767232 bytes; the lab observed
4934295552 free bytes at start. Exact runtime flags, dependency and harness hashes are in
[environment.json](environment.json).

No concurrent agent-started build, test, debugger or benchmark ran during measurement.
Ordinary desktop/IDE processes remained open, with occasional lightweight log inspection.
CPU affinity and temperature were not controlled or measured. No absence of OS background
activity or thermal throttling is claimed. Commands and build/test validation are documented
in the [v2 execution record](../../experiments/invocation-state-v2/execution.md).

## Historical context

The following are independently measured throughput means (batches/s). They are context,
not the baseline used for the replacement's primary comparison. `fixtures` has 11 files
per batch; the other workloads have one.

| Workload | Threads | V1 archive | Fresh v2 | Change % | Reported mean CIs overlap |
| --- | ---: | ---: | ---: | ---: | --- |
| fixtures | 1 | 773.776 | 729.620 | -5.707 | true |
| flat128 | 1 | 4690.462 | 4627.382 | -1.345 | true |
| flat1024 | 1 | 574.386 | 497.883 | -13.319 | true |
| nested128 | 1 | 446.708 | 362.436 | -18.865 | true |
| fixtures | 8 | 3470.400 | 2915.618 | -15.986 | true |
| flat128 | 8 | 17832.048 | 15721.098 | -11.838 | true |
| flat1024 | 8 | 1738.864 | 1740.199 | 0.077 | true |
| nested128 | 8 | 1735.273 | 1451.562 | -16.350 | true |

The historical capture ran on 2026-09-12; this pair ran on 2026-09-13. The fresh means are
usually lower, while the intervals above overlap. Overlap is not a significance test.
This variation supports using the fresh baseline and reporting observed uncertainty rather
than quoting a historical ratio as an intrinsic property of the code.

Raw iteration series retain trends after the fixed warmup. For example, fixtures at four
workers, third fork, rose from 1002.476 to 3177.806 batches/s across its five measurement
iterations. The single-worker `flat128` sample includes a 2650800.128 us observation;
its cause was not determined and the sample was retained. No fork, iteration or tail sample
was discarded. The protocol does not establish steady-state stationarity, and one sequential
old/new pair does not eliminate time-order effects.
