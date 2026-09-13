<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Temporary printer measurement laboratory

This independent Maven project compares the inherited and replacement source printers.
It is deliberately absent from the root reactor, production artifacts, normal tests and CI.
Keep the laboratory and reviewed evidence until the user requests their removal. Remove this
directory and its temporary instruction/changelog references together.

Read [PROTOCOL.md](PROTOCOL.md) for exact definitions and limits. An agent repeating the
experiment must follow [NEXT_AGENT.md](NEXT_AGENT.md), including the scope review.

The reusable printer comparison uses the separately versioned
[invocation-state protocol v2](experiments/invocation-state-v2/README.md).
The original v1 instrument and archived evidence remain unchanged.

The completed comparison of `5a121b67` and `0f6aa72d` is in the
[replacement assessment](results/candidate-0f6aa72d-v2/assessment.md), with
[all runtime counters](results/candidate-0f6aa72d-v2/runtime-details.md),
[static inventories](results/candidate-0f6aa72d-v2/static-details.md) and
[execution record](experiments/invocation-state-v2/execution.md).

## Build

Run commands from the repository root. Both `java -version` and `mvn -version` must report
the same **HotSpot JDK 21**. For directly comparable results, use the exact JDK build and
machine recorded in the baseline. Compilation of the project still uses its normal release
targets; the lab and fixture compiler use release 21.

```text
mvn -B -ntp -pl core -am install -DskipTests -Dskip-quality-gates -Dci=true
mvn -B -ntp -f core/printer-lab/pom.xml clean package
```

The first command installs the current core and its prerequisite. It is artifact preparation,
not a correctness check. Run `mvn -B -ntp -pl core verify -Dci=true` when core production code
changes; full-project verification requires the user's explicit request. The second command
builds only this lab and copies its runtime libraries into
`core/printer-lab/target/lib`. Do not reuse an old lab JAR after rebuilding core.

The default core dependency is `1.1.0-SNAPSHOT`. If the project version changes, supply
`-Djharmonizer.version=NEW_VERSION` to the lab build without editing the experiment POM.
JMH 1.37, PMD 7.24.0 and ArchUnit 1.5.0 are pinned independently of future production changes.

## Commands

All result directories must be new. The tool never overwrites an earlier experiment.

Static measurements only:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar metrics . core/printer-lab/target/static-run
```

Diagnostic run: all workloads, eight throughput workers, one sampled-time worker, one fork,
one warmup and one measurement iteration. This checks the harness and is **not a baseline**.

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar smoke . core/printer-lab/target/smoke-run
```

Full capture: static analysis, prepared-output compilation and stability checks, followed by
the frozen JMH matrix. Allow roughly 20–40 minutes on this machine, depending on preparation
and competing activity; retain the actual timestamps instead of relying on this estimate.

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . core/printer-lab/target/baseline-run
```

Repeat against a saved baseline after changing the implementation:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar capture . core/printer-lab/target/candidate-run core/printer-lab/results/baseline-5a121b67
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar compare core/printer-lab/results/baseline-5a121b67 core/printer-lab/target/candidate-run core/printer-lab/target/comparison.md
```

`capture` checks compatibility before timing when a baseline is supplied. `compare` checks
it again, requires the complete 20-combination matrix and produces a Markdown comparison.
Individual measured values and all per-fork/per-iteration data remain in the JMH JSON files.

## Files to retain

The scratch run includes generated source files, compiler output and CPD excerpts under
`target/`. Keep those as diagnostic artifacts. For a reviewed, committed run, copy these
files into a new directory under `core/printer-lab/results/` using the checked archive command:

```text
java -Xms128m -Xmx512m -jar core/printer-lab/target/printer-lab.jar archive core/printer-lab/target/baseline-run core/printer-lab/results/baseline-5a121b67
```

The command requires a completed full capture and copies:

- `environment.json`, `workloads.json`, `golden.json`;
- `source-metrics.json`, `maintainability-findings.json`, `architecture.json`, `architecture.md`;
- `cpd.xml`, `cpd-summary.json`, `measurements.md`;
- `throughput-t1.json`, `throughput-t2.json`, `throughput-t4.json`, `throughput-t8.json`, `sample-t1.json`;
- the corresponding five `.log` files;

Add a written assessment of the actual results, limitations and measurement conditions.

Before committing, expand only the first SPDX comment in archived `cpd.xml` into this form:

```xml
<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->
```

Keep the XML declaration and the entire `<pmd-cpd>...</pmd-cpd>` payload unchanged. The
repository license plugin requires standalone comment delimiters. Do not use `license:format`
on the compact exported comment: its configured end-marker detection consumes the remaining
XML when `-->` shares a line with the license identifier. Then run the existing read-only check:

```text
mvn -B -ntp -pl core -am -Dci=true license:check
```

The licensed JSON envelope has SPDX fields and an unmodified `data` payload. JMH's original
array is stored under `data`; `.raw.json` is the original tool output in the scratch run.
Logs retain the JMH body with two SPDX comment lines prepended. These transformations do
not round or recalculate the original measurements.
The lab's Git attributes preserve JMH's trailing histogram spacing in archived logs while
keeping normal whitespace checks for source and documentation.

Do not commit `target/`, binary dependencies, generated classes, or the prepared corpus.
The committed manifests, generator templates and golden artifact are sufficient to reproduce
the inputs and verify their exact contents. No task writes to `src/test/resources`.
