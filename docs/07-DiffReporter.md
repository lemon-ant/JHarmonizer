# DiffReporter

## Purpose

In **check mode**, a dedicated component — `DiffReporter` — is required to compare the **original Java source code with the reordered version**. It serves two key purposes:

1. **Comparison**: Determines whether the original and transformed code are identical.
2. **Diagnostics**: If differences exist, generates a readable **diff output** suitable for terminal or log output, highlighting the changes clearly.

## Usage

`DiffReporter` is invoked from within the `check(...)` method to:
- Validate whether reordering is necessary.
- Raise an exception when mismatches are found, including a detailed diff report.

## Possible Implementations

1. **Existing Java libraries**:
   - [`google-diff-match-patch`](https://github.com/google/diff-match-patch)
   - [`java-diff-utils`](https://github.com/java-diff-utils/java-diff-utils)

2. **Reviewing Palantir Java Formatter**:
   - Although `palantir-java-format` does not expose a standalone diff component, its internal logic in **`check` 
   mode** can serve as a valuable **source of inspiration**. Investigating how it compares the formatted output
   with the original may offer reusable strategies.

3. **Custom implementation**:
   - Line-by-line comparison with highlighted differences;
   - Optional highlighting at the character level;
   - Configurable verbosity (e.g., control display of whitespace-only differences or empty lines).

## Next Steps

- Conduct a technical research sweep:
  - Benchmark available libraries in terms of performance, Unicode support, and diff quality.
  - Explore the comparison logic used in `palantir-java-format`.
  - If necessary, implement a minimal internal `DiffReporter` prioritizing readability and extensibility.
