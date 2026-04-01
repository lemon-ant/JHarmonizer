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

## Known limitation: template placeholders are not Java grammar

Palantir formatter expects parseable Java compilation units. Template placeholders like:

```java
package ${package};
```

are not valid Java grammar before external resource/template processing, so formatter invocation fails for such files.

When your sources contain template placeholders, you have two supported options:

1. Put `// @jharmonizer:fully-off` on the first line to skip harmonization for that file.
2. Keep processing enabled. The formatter will fail for files containing placeholders, JHarmonizer will report an `ERROR` for each such file, and the rest of the pipeline will continue to run.
