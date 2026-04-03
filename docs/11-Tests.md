# Testing Strategy

## Overview

Testing the core logic of JHarmonizer can be done in parallel with the development of other components. This includes
both the sorter (reordering logic) and the diff-reporter component.

## Test Coverage Goals

### Sorter Test Cases

- **Valid Java classes**:
  - Standard ordering with fields, methods, constructors, static blocks.
- **Corner Cases**:
  - Inner/nested classes.
  - Static initializers before or after fields.
  - Complex field interdependencies (e.g., constant expressions).
  - Interfaces with default methods.
- **Invalid Inputs**:
  - Corrupted or unparseable Java files.
  - Structural syntax issues.

Tests should validate:
- Successful reordering for valid cases.
- Proper exception handling for invalid files.
- Consistent output according to defined sorting rules.

### Testing Without Fully Working Parser

Although the actual reordering depends on a working parser, the following can be developed **in parallel**:
- Construction of test cases.
- A test runner to process test files and compare results.
- Expected results prepared as `.expected.java` files for comparison.

Stub/mock implementations of the parser can be used to simulate flow during early development.

##  DiffReporter Testing

The `DiffReporter` can and **should** be tested independently:
- Input: `originalCode.java` and `reorderedCode.java`
- Output: Boolean match and human-readable diff.

Unit tests should cover:
- Minor structural differences → accurate diff
- Major differences → clear summary and line-by-line changes

This ensures robustness of the `check()` mode used in CI pipelines.

## Future Considerations

- Runtime performance benchmarks on large codebases.
- Compatibility testing across Java versions.
