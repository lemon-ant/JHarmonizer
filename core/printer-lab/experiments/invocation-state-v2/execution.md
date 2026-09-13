<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Execution record: 2026-09-13

Production baseline: `5a121b67ae240177a250585db3e1d07abfbf42cf`.
Production candidate: `0f6aa72de70606b241a3e2bc11898c7fb8580199`.
Candidate commit: `Replace inherited Spoon printer with source-fragment printing`, pushed
to `origin/test/printer-e2e-coverage` before laboratory capture.

## Checkouts and frozen inputs

The workspace was `W:/JHarmonizer`. Detached checkouts were created at
`core/printer-lab/target/v2-baseline` and `core/printer-lab/target/v2-candidate`.
The baseline predates the laboratory. Its checkout received the frozen lab's `src/`, POM,
corpus and documentation; `scope.tsv` was restored from `65608f1f`. No `target/` or archived
results were recursively copied. The candidate used the scope committed in `0f6aa72d`.

The [two-line patch](protocol.patch) was checked and applied identically to both checkouts.
It only changes the protocol identifier and the benchmark JavaDoc. The main workspace's
lab POM, corpus and Java sources remain unchanged. Production sources in both checkouts
were unchanged after checkout; candidate diffs were limited to those two lab lines.
The rationale and reproducible boundary are in [README.md](README.md).

Runtime dependencies, corpus/harness hashes and scope hashes are retained in each capture's
`environment.json`. The different own-project JAR hashes are expected after rebuilding the
two revisions; dependency POMs are unchanged. The strict comparator checks all other runtime
libraries and the identical instrument. The supplier lifecycle and equivalent group-metadata
assignment change are the reviewed production changes outside the printing scope.

## JDK, build and verification

All builds and measurements used `C:/Program Files/Microsoft/jdk-21.0.8.9-hotspot` as
`JAVA_HOME` and first on `PATH`, with Maven 3.9.11 and local repository `W:/repository`.
`JAVA_TOOL_OPTIONS` contained only `-Dfile.encoding=UTF-8`; `JDK_JAVA_OPTIONS` was empty.

The candidate's changed module was verified from the main workspace:

```text
mvn -B -ntp -pl core verify -Dci=true
```

Result: BUILD SUCCESS, 755 tests, zero failures/errors, three existing skips. The printer E2E
runner's 75 tests passed, as did the other core tests, coverage gates and SpotBugs (zero bugs
and errors). The skipped tests are two manual snapshot generators and the existing enum
constant dependency TODO. Verification took 18 min 06 s and finished at
`2026-09-13T19:58:16+02:00`. Log: `core/printer-lab/target/replacement-core-verify.log`.
No full-reactor test run was performed.

Each isolated checkout was then built, baseline first and candidate second:

```text
mvn -B -ntp -pl core -am install -DskipTests -Dskip-quality-gates -Dci=true
mvn -B -ntp -f core/printer-lab/pom.xml clean package
```

All four builds succeeded. Core/dependency builds took 1 min 29 s and 1 min 21 s; lab builds
took 23.346 s and 19.878 s. Each lab retained its own copied dependency JARs. Subsequent
installation of the candidate therefore did not replace the baseline benchmark's classes.
These builds completed before accepted timings began.

## Diagnostics and full capture

The run root was `W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d`.
From the baseline and candidate checkout roots respectively:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar smoke . W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/baseline-smoke
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar smoke . W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/candidate-smoke
```

Both diagnostics passed. Their timing scores are excluded. An initial existing contract
diagnostic also checked all 14 candidate documents against the v1 archive; it did not alter
the instrument or golden data. Both full captures reproduced their respective diagnostic
source metrics, architecture, CPD summary, PMD findings and golden artifact by SHA-256.

Full baseline command, from its checkout root:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/baseline-run
```

Full candidate command, from its checkout root, after the baseline completed:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/candidate-run W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/baseline-run
```

The host JVM's smaller heap is separate from each benchmark fork's fixed 1 GiB heap.
Forks used G1, English/US/UTC/UTF-8, GCProfiler and MemPoolProfiler, with no debugger,
JaCoCo or injected agents. The complete per-fork command and iteration series remain in
the archived logs. Workloads ran in fixed order: fixtures, flat128, flat1024, nested128;
throughput at 1/2/4/8 workers, followed by sampled time at one worker.

The hardware was an Intel Core i7-6700HQ (four physical/eight logical CPUs), with
17036767232 bytes RAM, Windows 11 Pro 10.0.26100 (JVM reports OS version 10.0).
The active plan was Balanced (`381b4222-f694-41f0-9685-ff5bb260df2e`). Power inspection
reported `PowerOnline=true`, charge 100%, `Charging=false`, `Discharging=true`, and
`Win32_Battery.BatteryStatus=2`; the provider values are recorded without interpreting
the conflicting charging/discharging flags. Free RAM at capture start was 4934295552
bytes for the baseline and 4908433408 for the candidate.

No agent-started builds, tests, debuggers or other benchmarks ran concurrently with accepted
timings. Ordinary IDE/desktop processes remained open; occasional lightweight file/log
inspection continued. No affinity, temperature control, background-load monitor or
stationarity test was added to the frozen protocol. All forks and outliers were retained.
This was one sequential baseline/candidate pair, not an alternating-order experiment.

## Completion and archiving

Both full captures exited successfully and recorded all 20 combinations. Each combination's
raw data contains three forks with five measured iterations. No benchmark exception or
failure was found in the ten logs. Baseline capture ran from
`2026-09-13T18:09:10.841021900Z` to `2026-09-13T18:35:33.416071200Z` (26 min 22.575 s);
candidate capture ran from `2026-09-13T18:35:53.381706800Z` to
`2026-09-13T19:01:51.948266Z` (25 min 58.567 s).

The following commands ran from the candidate checkout root and exited successfully:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar compare W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/baseline-run W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/candidate-run W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/comparison.md
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar archive W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/baseline-run W:/JHarmonizer/core/printer-lab/results/baseline-5a121b67-v2
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar archive W:/JHarmonizer/core/printer-lab/target/measurement-0f6aa72d/candidate-run W:/JHarmonizer/core/printer-lab/results/candidate-0f6aa72d-v2
```

Strict comparison passed protocol, hardware/runtime, dependency/harness/corpus and exact
serialization checks. The existing archive command copied 20 artifacts per revision.
Only each CPD SPDX comment was expanded to the required multiline form; the entire XML
payload starting at `<pmd-cpd` was compared exactly with the scratch original. The remaining
19 files per archive matched their originals by SHA-256. The original v1 archive was not
modified. Assessment tables present existing observations and ordinary differences/ratios;
no alternate complexity, coverage or benchmark instrument was introduced.

Final license validation from the main workspace:

```text
mvn -B -ntp -pl core -am -Dci=true license:check
```

Result: BUILD SUCCESS in 7.756 s at `2026-09-13T21:09:11+02:00`. This checks licenses only;
it does not rerun tests. The patch also passed `git apply --check`. Final diff review confirmed
that production sources/POMs, the main lab instrument/corpus/scope and the original v1 archive
remain unchanged since `0f6aa72d`. Both instruction files retain the same operative lab rules;
module-only test execution and explicit user control over lab removal are documented.
