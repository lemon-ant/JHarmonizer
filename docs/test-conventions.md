# Test conventions

This repository uses a small set of conventions for unit and integration tests.

The goals are:

- Make tests readable at a glance (predictable structure).
- Make failures actionable (clear names and error messages).
- Keep maintenance low (shared helpers, minimal duplication).
- Avoid “false regressions” caused by broken fixtures (fixtures must compile).

## Tooling and libraries

- **JUnit 5** is the test runner.
- **AssertJ** is the assertion library.
  - Do not use `org.junit.jupiter.api.Assertions.*` in new/updated tests.
- Prefer using production pipeline building blocks (parsers, converters, compilers, factories) instead of test-only reimplementations.

## Code reuse and deduplication

When writing or refactoring tests, always look for overlapping code fragments and reuse existing helpers. Duplicating the same snippet across multiple tests is discouraged.

Rules:

- Before copying code into another test, search for an existing shared test utility and reuse it.
- If similar fragments appear in more than one test (or are likely to be reused), extract them into a shared test utility class.
- Re-run this reuse analysis regularly: when adding new tests, when refactoring tests, and during cleanup passes.

## Naming

### Test methods

- JUnit test method names must follow **exactly 3 segments**:

  `methodName_condition_expectedResult`

Examples:

- `compileConfig_validYaml_shouldProduceSingleRootGroup`
- `resolveGroups_nestedMatch_shouldWinOverParentGroup`
- `describeMembers_recordType_shouldNotReturnImplicitMembers`

Guidelines:

- `methodName` must start with a verb (e.g., `compile`, `resolve`, `describe`, `render`, `convert`).
- Avoid vague words (`works`, `ok`, `smoke1`). Prefer intent-revealing words.
- Keep the condition minimal but specific.

### Test classes

- Prefer `<ProductionClassName>Test` for unit tests.
- Prefer `<FeatureOrScenarioName>Test` for integration tests that cover a pipeline.
- If you need multiple scenarios, prefer `@Nested` classes instead of splitting into many test classes.

## Structure

### Given / When / Then blocks

- Each test body must be split into contiguous blocks using comments:

  - `// Given`
  - `// When`
  - `// Then`

Rules:

- Do not insert empty lines **inside** a block.
- Do not insert an empty line at the very beginning of the method body before `// Given`.
- Keep each block contiguous and focused.

Recommended skeleton:

```java
@Test
void resolveGroups_nestedMatch_shouldWinOverParentGroup() {
    // Given
    ...

    // When
    ...

    // Then
    ...
}
```

### Parameterized tests

- Use parameterized tests when it reduces repetition and improves readability.
- The method naming rule (3 segments) still applies.

## Fixtures and resources

### Where fixtures live

- Store fixtures under:

  `src/test/resources/test-cases/**`

- Use explicit scenario folder names (avoid generic `example/`).

### valid/ vs invalid/

- Use folder naming to communicate intent:
  - `valid/` — must compile and be compilable by the build gate.
  - `invalid/` — may intentionally not compile (only for negative tests).

### Fixtures must compile (build-time gate)

- All `valid/**/*.java` fixtures must compile as part of the build.
- Prefer fixtures that are self-contained and depend only on the JDK.
- Prefer single-file fixtures. If multiple files are required, keep them in the same scenario folder.

### Reading resources

- Prefer classpath-based access (`ClassLoader.getResourceAsStream`) over filesystem paths.
- Use shared helpers (e.g., `TestCaseResourceUtils`) to read resources.

Recommended API shape:

- Keep resource identifiers as typed values where feasible (`URL`), not raw strings.
- Keep resource paths absolute (start with `/`).
- If you need to resolve a file under a directory, resolve via a dedicated helper, not via deprecated URL constructors.

### Shared test setup and one-time initialization (avoid repeated work)

If multiple tests in the same test class use the same expensive or repetitive setup (e.g., parsing, compilation, model construction, large object graphs, heavy calculations, or any other non-trivial preparation), initialize it **once** at the test-class level instead of re-creating it in every test.

Preferred options (in order):

- Use `private static final` constants for immutable, shareable objects created once.
- Use `private final` fields when per-instance initialization is sufficient and the object is safe to share across tests in the class.
- Use `@BeforeAll` to perform one-time initialization that cannot be expressed as a simple field initializer.
    - If `@BeforeAll` must be non-static, use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`.

Important rules:

- Do **not** share mutable objects across tests if the code under test may modify them.
    - In that case, keep a single immutable “base” representation and create a fresh copy per test, or initialize the mutable object in `@BeforeEach`.
- Avoid duplicating the same setup snippet across multiple tests in the same class.
    - If repeated setup appears, refactor it into a shared field initializer or a dedicated setup method.
- Keep the `// Given` section focused on *test-specific* inputs; common setup belongs to fields / `@BeforeAll` / `@BeforeEach`.

## Assertions and test utilities

### Assertions

- Use assertions only for validating the **test contract** and expected results.
- Prefer AssertJ (`assertThat(...)`).

### Test utilities must throw exceptions (not assert)

Test utilities exist to remove duplication and improve readability. They are not a place for test verdicts.

Rules:

- Test utility methods must not call `fail(...)` or use assertions for control flow.
- If a test utility cannot proceed (missing resource, ambiguous match, invalid input), it must throw a descriptive runtime exception:
  - `IllegalArgumentException` for invalid inputs.
  - `IllegalStateException` for unexpected setup/state (e.g., “expected exactly one match, got 0/2”).
  - `UncheckedIOException` for I/O problems.

This keeps the failure type meaningful:

- helper failure = broken test setup / broken fixture (exception)
- assertion failure = broken product behavior (assertion)

### Lombok for test utilities

For internal **test-only** utility classes, prefer Lombok for null checks:

- Use `@NonNull` on parameters instead of `Objects.requireNonNull(...)`.
- Use `@UtilityClass` for pure utility classes.

Example:

```java
import lombok.UtilityClass;
import lombok.NonNull;

@UtilityClass
final class SpoonTestCaseUtils {

    static CtType<?> parseMainTypeFromJavaFixture(@NonNull java.net.URI javaFixtureResourceUri) {
        ...
    }
}
```

### Optional-returning helpers

If a helper returns `Optional<T>`, that is part of the test logic.

Recommended pattern:

- `findXxx(...)` returns `Optional<T>`.
- `requireXxx(...)` returns `T` and throws `IllegalStateException` if missing/ambiguous.

The test decides which one to use:

- presence/absence is a product expectation → assert it in the `// Then` block
- absence is a broken fixture/setup → use `requireXxx(...)`

## Temporary files and debug output

- Tests must not write into `src/test/resources`.
- Use `@TempDir` (JUnit 5) or write into `target/`.

## Formatting

- Avoid inserting empty lines between closely related constant declarations.
  - Use a blank line only to separate semantic groups.
- Keep `// Given` immediately after the opening brace.
- In test utility classes, group fields/constants by meaning; do not insert a blank line after every field “just because”.

## Code style in tests

- Prefer fully descriptive variable names (avoid `i`, `tmp`, `m`, etc.).
- Prefer Stream API when it makes the flow clearer (filter → map → collect).
- Keep helpers small and single-purpose.
