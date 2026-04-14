# Test conventions

This repository uses a small set of conventions for unit and integration tests.

The goals are:

- Make tests readable at a glance (predictable structure).
- Make failures actionable (clear names and error messages).
- Keep maintenance low (shared helpers, minimal duplication).
- Avoid “false regressions” caused by broken fixtures (fixtures must compile).

If review feedback or repeated task work reveals a stable testing rule that is missing here or not clearly stated, update this document in the same task.
Keep `docs/test-conventions.md`, `AGENTS.md`, and `.github/copilot-instructions.md` aligned:

- `docs/test-conventions.md` is the detailed source for test-specific rules.
- `AGENTS.md` should keep the repository-wide maintenance guidance and point to this document for tests.
- `.github/copilot-instructions.md` must mirror the complete operative test rules from this document so Copilot can follow them without relying on cross-file traversal.
- When a test rule changes here, update the mirrored guidance in `.github/copilot-instructions.md` in the same task, and update `AGENTS.md` too if the maintenance guidance or document responsibilities changed.

## Tooling and libraries

- **JUnit 5** is the test runner.
- **AssertJ** is the assertion library.
  - Do not use `org.junit.jupiter.api.Assertions.*` in new/updated tests.
- Prefer ordinary imports over repeated fully qualified names in tests.
- Prefer using production pipeline building blocks (parsers, converters, compilers, factories) instead of test-only reimplementations.
- When an annotation argument in test code only repeats the library or framework default behavior, omit it instead of spelling it out explicitly.
- When test code overrides standard `Object` methods, preserve the standard signature exactly.
  - Do not add nullability annotations to `Object` overrides in tests.
  - In particular, keep `equals(Object)` unchanged.

## Code reuse and deduplication

When writing or refactoring tests, always look for overlapping code fragments and reuse existing helpers. Duplicating the same snippet across multiple tests is discouraged.

Rules:

- Before copying code into another test, search for an existing shared test utility and reuse it.
- If similar fragments appear in more than one test (or are likely to be reused), extract them into a shared test utility class.
- Re-run this reuse analysis regularly: when adding new tests, when refactoring tests, and during cleanup passes.
- When a test in another package must create a production type with package-private construction, prefer a dedicated test creator/helper in the target package instead of widening the production constructor visibility.

## Naming

### Test methods

- JUnit test method names must follow **exactly 3 segments**:

  `subject_condition_expectedResult`

Scope note:

- This naming rule applies only to executable test methods (for example methods annotated with `@Test`, `@ParameterizedTest`, and other JUnit test-invocation annotations).
- It does **not** apply to lifecycle and helper methods such as `@BeforeEach`, `@BeforeAll`, `@AfterEach`, `@AfterAll`, or private utility methods; name those with normal Java conventions (for example `setUp`, `tearDown`, `createSampleFile`).

Examples:

- `compileConfig_validYaml_produceSingleRootGroup`
- `resolveGroups_nestedMatch_winOverParentGroup`
- `helpCommand_rootHelpRequested_printUsageInformation`

Guidelines:

- `subject` names what is being tested. It usually mirrors the production method, command, or feature name.
- `condition` states only the relevant precondition/input shape.
- `expectedResult` states the observable outcome.
- Do not use filler words such as `should`, `when`, `then`, `must`, or similar “BDD glue” inside the method name.
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
- Insert **exactly one** empty line **between blocks**.
  - In particular, there must be a blank line **before** `// When` and a blank line **before** `// Then` (and before combined blocks such as `// When / Then`).
- Do not insert an empty line at the very beginning of the method body before `// Given`.
- Keep each block contiguous and focused.

Notes:

- In some tests, it is valid to **merge blocks** when it improves readability.
  - Common case: very small tests may use `// Given / When` together if separating them would add noise.
  - Another common case: non-exception assertion tests may use `// When / Then` together when the action and assertion fit naturally in one block (e.g., `assertThat(value).isEqualTo(...)`).
  - This is allowed as long as the combined block stays contiguous and clear.

### Exception tests

When a test verifies that a method throws an exception, **always** split the action and the assertion into separate `// When` and `// Then` blocks.

- `// When` block: capture the thrown exception using `catchThrowable(...)` from AssertJ.
- `// Then` block: assert on the captured throwable using `assertThat(thrown)`.

Use `catchThrowableOfType(ExceptionType.class, ...)` instead of `catchThrowable(...)` only when the specific exception type needs to be accessed directly (e.g., calling type-specific methods on it) in the `// Then` block.

**Do not** use `// When / Then` with `assertThatThrownBy(...)`.

Good:

```java
// When
Throwable thrown = catchThrowable(() -> SortingUtils.buildItemIndex(null));

// Then
assertThat(thrown).isInstanceOf(NullPointerException.class);
```

Good (typed access required):

```java
// When
SpoonModelBuildException thrown = catchThrowableOfType(
        SpoonModelBuildException.class, () -> parser.build(srcFile));

// Then
assertThat(thrown.getSrcPath()).isEqualTo(Path.of("Sample.java"));
assertThat(thrown).hasMessageContaining("boom").hasCause(cause);
```

Bad:

```java
// When / Then
assertThatThrownBy(() -> SortingUtils.buildItemIndex(null))
        .isInstanceOf(NullPointerException.class);
```

Recommended skeleton:

```java
@Test
void resolveGroups_nestedMatch_winOverParentGroup() {
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
- Prefer resource fixtures under `src/test/resources/test-cases/**` over large inline YAML/Java strings embedded directly in test classes.
- Do not keep non-trivial multi-line textual fixtures (for example YAML, JSON, XML, Java source, or long expected-output snippets) inline in test code.
- Do not write large fixture content as inline string literals and then persist it to temp files during test setup.
  - Store original/expected/config fixture files under `src/test/resources/test-cases/**` and copy/read them in tests.
- For formatter-focused fixtures under `src/test/resources/test-cases/**`, keep `input/` resources valid for the parser/compiler but intentionally not already formatted like `expected/`.
  - The scenario should require a real formatter rewrite instead of passing because `input/` already matches `expected/`.
  - Store them under `src/test/resources/test-cases/**` and load them via shared helpers such as `TestCaseResourceUtils`.
  - Only tiny one-off snippets that stay obviously readable inline are acceptable.

### valid/ vs invalid/

- Use folder naming to communicate intent:
  - `valid/` — must compile and be compilable by the build gate.
  - `invalid/` — may intentionally not compile (only for negative tests).

### Fixtures must compile (build-time gate)

- All `valid/**/*.java` fixtures must compile as part of the build.
- Prefer fixtures that are self-contained and depend only on the JDK.
- Prefer single-file fixtures. If multiple files are required, keep them in the same scenario folder.
- When a fixture verifies ordering inside one logical group, prefer including multiple declarations of the same kind and cover secondary ordering rules (for example visibility and alphabetical order) where the language allows it.

### Reading resources

- Prefer classpath-based access (`ClassLoader.getResourceAsStream`) over filesystem paths.
- Use shared helpers (e.g., `TestCaseResourceUtils`) to read resources.
- If a regression test verifies the built-in default configuration, load the real embedded `default-config.yml` through the production default-loading path instead of duplicating it in test fixtures or inline YAML.

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

### Constants grouping (readability)

Keep a small amount of shared test state as regular fields at the top of the class when that remains easy to scan.

- Good candidates to keep at the top:
  - `@TempDir` fields
  - one or two obvious shared constants that help the first test read naturally

If a test class contains many shared constants (URLs, resource paths, source fragments, pre-parsed models, etc.) and they start cluttering the top of the file, group them into a nested class instead of stacking a long constant block before the tests:

```java
private static final class Constants {
    private static final URL FIXTURE_URL = ...;
}
```

Rules:

- Prefer a nested `Constants` class once the constant list is long enough that it pushes test methods noticeably down the file or makes the start of the class hard to scan.
- Keep the `Constants` nested class at the end of the test class to keep the beginning focused on test methods.
- Do not move everything into `Constants` mechanically; keep only the cluttering shared constants there, while ordinary test fields such as `@TempDir` stay near the top.

## Assertions and test utilities

### Do not test test utilities

- Do not add dedicated unit tests whose only purpose is to test test-only utility classes or helper methods.
- Validate test utilities indirectly through the real unit/integration tests that use them.
- If a test utility becomes complex enough to deserve direct behavioral tests, that is a signal to move the logic into production code or to simplify the helper.

### Placement of test utilities

- Place **private static** helper methods and **test-only utility** code at the **end of the test class**, after all test methods (and after nested `Constants`, if present).
- The top of the test class should stay focused on test scenarios, not helper implementation details.


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

- Use `@NonNull` on non-private parameters instead of `Objects.requireNonNull(...)`.

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
- Never shorten or abbreviate a variable name when a more descriptive name exists; if the type is `FileProcessingResult`, the variable must be `fileProcessingResult`, not `result`.
- Prefer Stream API when it makes the flow clearer (filter → map → collect).
- Keep helpers small and single-purpose.


---

### Avoid meaningless Given blocks

Do not introduce a `// Given` block for a single obvious local variable assignment.

Bad (adds noise without improving readability):

```java
// Given
DeclarationModifier declarationModifier = DeclarationModifier.SEALED;
```

If the setup is trivial and self-explanatory, omit `// Given` entirely or use a combined block:

```java
DeclarationModifier declarationModifier = DeclarationModifier.SEALED;

// When / Then
assertThat(declarationModifier.isApplicableTo(TargetCategory.TYPE)).isTrue();
```

`// Given` must be used only when it groups multiple setup statements or improves readability.
