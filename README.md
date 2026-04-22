<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer

JHarmonizer is a Java source harmonization tool that keeps class member layout deterministic and readable.
It parses Java source, resolves grouping/sorting rules, applies dependency-safe reordering, and formats output.

## Current status

- Core pipeline is available: parse -> classify -> sort -> print -> format.
- Declaration-order dependency graph is implemented for direct initializer/read-write scenarios and accessor bundling.
- Comment-based opt-out directives are supported for file scope and type scope.
- Advanced inter-procedural dependency tracing for field initializers through called methods is **not implemented yet**.

## Roadmap (next versions)

- Compile sorting behavior once per group and precompute reusable sort keys in member descriptors.
- Add selector matching by type (field type / method return type).
- Add explicit enum constant ordering strategies.
- Add support for corner-cases with explicit declaring-type instance forward references during class initialization (currently parked as known failing E2E scenario).
- Add automatic parameter-order harmonization for overriding methods and constructors: base/forwarded parameters first, extension parameters after them.
- Add redundant-modifier cleanup pass that removes semantically useless Java modifiers (for example implicit interface/class member modifiers) while preserving behavior.
- Add inter-procedural initializer dependency analysis:
  - if a field default expression calls a method, inspect method body reads/writes;
  - recursively follow nested method calls inside the same declaring type;
  - build declaration dependencies based on transitively accessed fields;
  - handle recursion/cycles safely and conservatively.

Details and implementation notes are tracked in [docs/TODO.md](docs/TODO.md).

## Opt-out directives

JHarmonizer supports two native comment directives:

- `@jharmonizer:fully-off`
- `@jharmonizer:sort-off`

Directive matching is case-insensitive, so variants such as `@JHarmonizer:Fully-Off` are also recognized.

Supported comment forms:

- line comments: `// @jharmonizer:fully-off`, `// @jharmonizer:sort-off`
- block comments: `/* @jharmonizer:fully-off */`, `/* @jharmonizer:sort-off */`

Supported scopes:

- file scope
- type scope

### File scope

Place a directive in the compilation-unit preamble:

- before `package`
- between `package` and `import`
- between `import` statements and the first top-level type
- at the beginning of a file without `package`

Behavior:

- `@jharmonizer:fully-off` — fully disable harmonization for the file (no sorting, no formatting, no import fixing)
- `@jharmonizer:sort-off` — disable sorting only, but still run formatting and import fixing

### Type scope

Place a directive immediately before a top-level or nested type declaration.

Behavior:

- `@jharmonizer:fully-off` — fully disable harmonization for that type subtree and preserve its original source text
- `@jharmonizer:sort-off` — disable sorting only for that type subtree, but still format it together with the rest of the file

### Unsupported placements and tokens

The following are intentionally unsupported and ignored with a warning:

- member-level directives for fields, methods, constructors, or initializer blocks
- region-based `off/on` markers
- `@jharmonizer:on`, `@jharmonizer:format-off`, `@jharmonizer:format-on`, and similar variants
- directives inside method bodies or inside Javadoc

## Known formatter edge case: non-deterministic reflow across repeated runs

Palantir formatter can produce non-idempotent output for some long or heavily wrapped constructs. In observed
cases this affects **both comment indentation and code layout** between consecutive runs; some cases involve
trailing `//` comments attached to wrapped expressions, while others affect wrapped expressions more generally.

Concrete examples:

```java
// pass 1
assertEquals(
    2L,
    recovery.getMaxTransactionId()); // transaction ID is still 2 because that's what was written to the
                                     // journal

// pass 2
assertEquals(
    2L,
    recovery.getMaxTransactionId()); // transaction ID is still 2 because that's what was written to the
// journal
```

```java
// pass 1
factory.setShutdownQuietPeriod(
        Duration
                .ZERO); // Quiet period not necessary since sending threads will have completed before shutting
                        // down event sender

// pass 2
factory.setShutdownQuietPeriod(
        Duration.ZERO); // Quiet period not necessary since sending threads will have completed before shutting
        // down event sender
```


```java
// pass 1
@WritesAttribute(
        attribute = "http.headers.XXX",
        description =
                "Each of the HTTP Headers that is received in the request will be added as an attribute, prefixed"
                        + " with \"http.headers.\" For example, if the request contains an HTTP Header named"
                        + " \"x-my-header\", then the value will be added to an attribute named"
                        + " \"http.headers.x-my-header\""),

// pass 2
@WritesAttribute(
        attribute = "http.headers.XXX",
        description =
                "Each of the HTTP Headers that is received in the request will be added as an attribute, prefixed"
                            + " with \"http.headers.\" For example, if the request contains an HTTP Header named"
                            + " \"x-my-header\", then the value will be added to an attribute named"
                            + " \"http.headers.x-my-header\""),
```

```java
// pass 1
@Around(
        "within(org.apache.nifi.web.dao.ProcessGroupDAO+) && execution(void enableComponents(String,"
                + " org.apache.nifi.controller.ScheduledState, java.util.Set<String>)) && args(groupId, state,"
                + " componentIds)")
public void enableComponentsAdvice(

// pass 2
@Around("within(org.apache.nifi.web.dao.ProcessGroupDAO+) && execution(void enableComponents(String,"
        + " org.apache.nifi.controller.ScheduledState, java.util.Set<String>)) && args(groupId, state,"
        + " componentIds)")
public void enableComponentsAdvice(
        ProceedingJoinPoint proceedingJoinPoint, String groupId, ScheduledState state, Set<String> componentIds)
        throws Throwable {
```

This behavior is upstream in Palantir formatter and not introduced by JHarmonizer.

Workarounds:

1. For inline comments: avoid long trailing `//` comments; move long notes to a standalone line (or short block comment) above the statement.
2. For long annotation/string concatenations: prefer extracting long literals/expressions into named constants (or helper variables/methods) so formatter has fewer fragile wrap points.
3. Keep annotation argument values and fluent/pointcut expressions shorter per line where practical to reduce wrap oscillation risk.
4. If the file still oscillates between formatter runs, use `// @jharmonizer:sort-off` (keeps formatting but disables sorting) or `// @jharmonizer:fully-off` (disables harmonization) as a temporary mitigation.

## Maven/archetype template placeholders (`package ${package};`)

Files that still contain template placeholders such as `package ${package};` are not valid Java source yet. In this
situation the Palantir formatter cannot parse them; JHarmonizer leaves such files unmodified, marks them with an
ERROR result, and logs a warning, but the overall run continues and the CLI still exits with status 0.

For template sources you have two supported options:

1. Add `// @jharmonizer:fully-off` as the first line of that template file to skip harmonization for the whole file
   (recommended when you want clean runs without formatter warnings/errors).
2. Leave the file without opt-out and accept that each run will report a per-file ERROR and warning for that file
   because the formatter cannot parse the non-Java template placeholders; the file will be skipped and left unmodified.
## Build

```bash
mvn -B -ntp verify
```

## Sorting performance benchmark (JMH)

To measure **only sorting performance** (without parsing/serialization/formatting in measured time), use the
`benchmark-sort` Maven profile in `core`.

The benchmark:

- uses fixtures from `src/test/resources/test-cases/core/e2e/reorder/**/input/*.java`
  and `src/test/resources/test-cases/core/e2e/regression/**/input/*.java`;
- parses fixtures once during JMH setup;
- clones AST models and runs only `SpoonSorter.sortCompilationUnitRecursively(...)` in benchmarked operations.

Run with defaults (4 threads, batch size 1000, warmup 2, measure 5):

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

Output:

- console summary from JMH;
- machine-readable JSON results at `core/target/sorting-benchmark.json`.
