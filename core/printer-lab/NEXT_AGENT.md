<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Instructions for the next printer measurement

The user intends to replace the inherited Spoon printer with an implementation containing
only our own printing code. When asked to measure that replacement, repeat this experiment.
Do not redesign the experiment to suit the new implementation.

## Read and preserve

1. Read the repository instructions, [README.md](README.md), [PROTOCOL.md](PROTOCOL.md), and
   [baseline assessment](results/baseline-5a121b67/assessment.md).
2. The baseline production revision is `5a121b67ae240177a250585db3e1d07abfbf42cf`.
   [Its archived evidence](results/baseline-5a121b67/) contains exact source metrics, tool
   versions, library hashes, golden outputs, JMH observations and machine/JVM settings.
3. Keep the lab outside the root reactor, product artifacts, ordinary tests and CI.
   Its existence is temporary; the user will decide when the comparison is complete.
4. Keep `pom.xml`, `corpus.tsv`, and every lab `src/main` file unchanged. Their normalized
   content hashes are part of the comparison protocol. Do not update PMD/JMH/ArchUnit,
   durations, heap, worker counts, templates, flags or correctness checks between variants.

## Establish the replacement boundary

Trace the current serialization supplier and every project method it invokes while printing.
Review all newly extracted helpers, state holders, configuration and result contracts, including
classes outside the old printer package. Exclude parser/sorter setup, post-print formatting,
tests, the lab and third-party implementation bodies. Called library work still contributes
to measured runtime.

Update **only `scope.tsv`** to describe the replacement boundary using the same role names
and full-file/selected-method semantics. Remove obsolete entries, add every replacement
class and local helper, and explain the boundary changes in the candidate assessment.
Do not freeze the old class names or omit a new helper to reduce measured size/complexity.
The ArchUnit audit rejects dependencies on unmeasured project classes, but cannot prove
completeness: inspect inlined constants, reflective access and partial shared helpers manually.
When unrelated shared methods are excluded, keep the relevant declarations explicit.

The timed supplier must still create a fresh printer and serialize the already prepared
model on every call. Preserve this production entry point when implementing the rewrite.
A cached string/result is a different benchmark. Setup continues to parse and sort through
the real production pipeline, once per worker, outside timing.

Review the production diff outside the selected printing boundary as well. Changes to parsing,
sorting or the embedded default configuration can alter prepared model state beyond the
fingerprint's specified fields. Keep those behaviors unchanged for an isolated printer
comparison, or document the additional variable explicitly instead of attributing its effect
to the printer.

## Validate and build

Use the exact baseline HotSpot JDK 21 build on the same machine for direct comparison.
Check both `java -version` and `mvn -version`. Use external power and the same power mode;
close competing builds/tests/debug sessions. Record actual conditions, free memory and any
known background activity. The automatic machine check cannot detect thermal throttling.

Run production correctness checks, including printer E2E tests and the existing helper tests.
Run the ordinary `mvn -B -ntp verify` with JDK 21. Commit the replacement before measuring:
the lab rejects dirty production inputs so each result identifies a committed implementation.

From the repository root:

```text
mvn -B -ntp -pl core -am install -DskipTests -Dskip-quality-gates -Dci=true
mvn -B -ntp -f core/printer-lab/pom.xml clean package
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar smoke . core/printer-lab/target/candidate-smoke
```

If the project version changed, pass `-Djharmonizer.version=NEW_VERSION` to the lab build;
do not edit its POM. `clean` removes this lab's scratch `target/` directory, so archive any
earlier completed run first. Always rebuild the lab after installing a changed core artifact.
Its loaded core JAR must match `core/target` byte for byte.

## Capture without changing the protocol

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . core/printer-lab/target/candidate-run core/printer-lab/results/baseline-5a121b67
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar compare core/printer-lab/results/baseline-5a121b67 core/printer-lab/target/candidate-run core/printer-lab/target/comparison.md
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar archive core/printer-lab/target/candidate-run core/printer-lab/results/candidate-REVISION
```

Use a new output directory for every attempt. A full capture performs 20 combinations:
four workloads at four throughput worker counts, plus four single-worker sample-time runs.
Each combination uses three forks, five one-second warmups and five two-second measurements.
Smoke and interrupted runs cannot be archived or compared as full results.

Before timing, the candidate must reproduce the baseline's input hashes, prepared structural
fingerprints, exact source output and skipped-type ranges. Every printed file must compile.
Each worker checks output and structural stability before and after its trial. A correctness
failure stops the experiment; never regenerate the baseline golden artifact to hide it.

The tool rejects changes to the protocol, runtime dependencies, hardware identity and checked
environment settings. If a change is unavoidable, retain both old and new evidence, state
why direct comparison is invalid, and establish a separately versioned experiment measured
on **both** implementations. Do not silently remove a check. A changed API may require an
adapter, but then rerun both implementations with that identical adapted protocol.

For a consequential performance decision, also rerun the original production revision on
the current machine and alternate baseline/candidate order. Use separate checkouts or
worktrees with the same frozen lab sources; keep complete results for each run. Historical
numbers alone cannot control later OS load or thermal changes. Do not cherry-pick forks.

## Review and retain

Add `assessment.md` to the new archive and copy the generated `comparison.md` beside it.
Include the exact commands, revision, elapsed time, host conditions and correctness outcome.
Retain all licensed JSON/log files copied by `archive`; preserve raw iteration values and
histograms. Do not commit binary dependencies, generated source/classes or scratch `target/`.
Expand only the archived CPD SPDX comment as shown in [README.md](README.md), preserve its
complete XML payload, then run `mvn -B -ntp -pl core -am -Dci=true license:check`. Do not run
`license:format` on the compact exported comment; the configured delimiter scanner can
consume the rest of the report. Compare the XML payload with the scratch original afterwards.

Report these comparisons with the original units:

- Selected physical lines and NCSS, explicit operations, class/file distribution and roles.
- Per-operation Cyclo, Cognitive and NPath distributions and changed hotspots; source counts
  of conditions, loops, ternaries and short-circuit operators. Do not sum NPath values.
- Duplication at the same 50-token CPD threshold and the same PMD diagnostic screens.
- Internal dependencies, concentration of fan-in/fan-out, cycles, Lakos metrics, external
  dependency surface, inheritance and declared state. Retain graphs and actual edge lists.
- Whole batches/second and documents/second with reported JMH intervals; speedup and
  efficiency at 2/4/8 workers; sampled batch p50/p95/p99 in microseconds.
- Allocated bytes/batch, GC counters and pool peaks with the limitations in the protocol.
  Pool peaks are not retained printer size or process RSS. CI overlap is not a hypothesis test.

Separate measurements from judgment. Explain whether ownership of separators, comments,
fragments and skipped ranges becomes easier to follow, naming the affected methods/classes.
More own code can accompany less framework coupling; fewer lines do not prove a better design.
Do not invent a numerical beauty score, estimated maintenance hours or an overall grade.

Record experiment/tool dependency changes in `CHANGELOG.md`, commit the reviewed artifacts,
and follow the user's current publication instructions. The user may later request removal
of this entire lab and the corresponding temporary pointers in both instruction files.
