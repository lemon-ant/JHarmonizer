# Test conventions

This repository uses a small set of conventions for unit and integration tests.

## Naming

- JUnit test method names must follow `methodName_condition_expectedResult` (exactly 3 segments).

## Structure

- Each test body must be split into contiguous blocks using comments:
  - `// Given`
  - `// When`
  - `// Then`
- Do not insert empty lines inside a block.
- Do not insert an empty line at the very beginning of the method body before `// Given`.

## Assertions and test utilities

- Use assertions only for validating the test contract and expected results.
- Test utility methods must not call `fail(...)` or use assertions for control flow.
  - If a test utility cannot proceed (missing resource, ambiguous match, invalid input), it must throw a descriptive
    runtime exception (typically `IllegalArgumentException` or `IllegalStateException`).

## Formatting

- Avoid inserting empty lines between closely related constant declarations.
  - Use a blank line only to separate semantic groups.
