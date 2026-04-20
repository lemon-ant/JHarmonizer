<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

#  Formatter Wrapper

## Purpose

The formatter wrapper integrates the [Palantir Java Formatter](https://github.com/palantir/palantir-java-format) into
the `JHarmonizer` toolchain to ensure clean and consistent Java code output **after deserialization** of an AST structure.

This step is **critical** because deserialized Java code may lose indentation, import order, and other formatting features. Formatter ensures:

- Correct indentation
- Removal and ordering of imports
- Application of consistent code style (Palantir, Google, AOSP)

## Role in the Pipeline

```text
Java source (.java)
    ↓
Parse to AST model
    ↓
Sort and transform AST
    ↓
Serialize AST back to Java code
    ↓
Format output using Palantir formatter
```

## Formatter Usage

The wrapper internally delegates to this verified method from `com.palantir.javaformat.java.Formatter`:

```java
/**
 * Formats an input string (a Java compilation unit) and fixes imports.
 *
 * Fixing imports includes ordering, spacing, and removal of unused import statements.
 *
 * @param input the input string
 * @return the output string
 * @throws FormatterException if the input string cannot be parsed
 */
public String formatSourceAndFixImports(String input) throws FormatterException
```

## Configuration Parameters

Formatter style is configured via `JavaFormatterOptions.Style`, with three supported styles:

```java
public enum Style {
    /** The default Palantir Java Style configuration. */
    PALANTIR(2, 120),

    /** The default Google Java Style configuration. */
    GOOGLE(1, 100),

    /** The AOSP-compliant configuration. */
    AOSP(2, 100);
}
```

These parameters must be passed through the `Configuration` model and injected into the formatter via our `FormatterWrapper`.

## Summary

This wrapper is essential to finalize formatted, readable Java source code that adheres to a unified style. It should be executed only **after** AST transformation and serialization are complete.

## Known limitation: non-deterministic wrapping and reflow in Palantir formatter

Palantir Java Formatter has known edge cases where formatting is **not idempotent** for some wrapped constructs,
including long trailing `//` comments on wrapped expressions.

Observed behavior (matches the attached screenshots):

1. First formatter pass can wrap a long trailing comment and indent the continued comment line under the original
   comment token.
2. Second formatter pass can realign that continued comment line under a wrapped code fragment.
3. In more complex cases, the second pass can also reflow adjacent code differently (for example collapsing and
   then re-expanding chained or wrapped invocations around the same trailing comment area).
4. The resulting source differs between runs even when code semantics do not change.

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

This behavior originates in the upstream formatter engine and is **not** caused by JHarmonizer sorting or rendering.
JHarmonizer delegates formatting to Palantir formatter as-is.

Practical guidance for users:

- For inline comments: avoid long trailing `//` comments; move long notes to a standalone line (or short block comment)
  above the statement.
- For long annotation/string concatenations: prefer extracting long literals/expressions into named constants (or helper
  variables/methods) so formatter has fewer fragile wrap points.
- Keep annotation argument values and fluent/pointcut expressions shorter per line where practical to reduce wrap
  oscillation risk.
- If a file still oscillates between formatter runs, use `// @jharmonizer:sort-off` (keeps formatting but disables
  sorting) or `// @jharmonizer:fully-off` (disables harmonization) as a temporary mitigation.

## Known limitation: template placeholders are not Java grammar

Palantir formatter expects parseable Java compilation units. Template placeholders like:

```java
package ${package};
```

are not valid Java grammar before external resource/template processing, so formatter invocation fails for such files.

When your sources contain template placeholders, you have two supported options:

1. Put `// @jharmonizer:fully-off` on the first line to skip harmonization for that file.
2. Keep processing enabled. The formatter will fail for files containing placeholders, JHarmonizer will report an `ERROR` for each such file, and the rest of the pipeline will continue to run.
