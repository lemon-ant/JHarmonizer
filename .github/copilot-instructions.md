# GitHub Copilot instructions for JHarmonizer

## Scope and maintenance

- Read `AGENTS.md` before making changes. It contains the repository-wide coding conventions.
- Read `docs/test-conventions.md` before adding or updating tests.
- Keep `.github/copilot-instructions.md`, `AGENTS.md`, and `docs/test-conventions.md` aligned.
- `AGENTS.md` defines the repository-wide rules.
- `docs/test-conventions.md` defines the test-specific rules.
- This file must contain the complete operative rule set from both files so Copilot can follow it without relying on cross-file traversal.
- When any rule changes in `AGENTS.md` or `docs/test-conventions.md`, update this file in the same task.
- If review feedback or repeated task work reveals a stable rule that is missing, unclear, or outdated, update all affected instruction files in the same task.
- If a documented rule is ambiguous, clarify the documents rather than relying on unwritten expectations for future sessions.

## Repository-wide conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Keep changes surgical and avoid unrelated cleanup.
- Reuse existing project and library utilities before introducing custom helpers.
- Prefer explicit Java types over `var`.
- Reference-returning private methods must declare explicit `@NonNull` or `@Nullable` return annotations.
- Private method parameters do not need nullability annotations just because the method is private.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- Do not introduce Java records in production code or shared test infrastructure; use classes with Lombok instead where appropriate.
- Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record handling.
- If a utility is shared across processing phases, place it in a neutral package instead of under a phase-specific package.
- Non-obvious build/configuration workarounds must include a nearby comment that explains why the workaround exists, which upstream component requires it, and when it can be removed.
- Build and validate with JDK 21. The standard repository command is `mvn -B -ntp verify`.

## Test conventions

### Goals

- Make tests readable at a glance with predictable structure.
- Make failures actionable with clear names and error messages.
- Keep maintenance low through shared helpers and minimal duplication.
- Avoid false regressions caused by broken fixtures; fixtures must compile.

### Tooling and libraries

- JUnit 5 is the test runner.
- AssertJ is the assertion library.
- Do not use `org.junit.jupiter.api.Assertions.*` in new or updated tests.
- Prefer using production pipeline building blocks such as parsers, converters, compilers, and factories instead of test-only reimplementations.

### Code reuse and deduplication

- Before copying code into another test, search for an existing shared test utility and reuse it.
- If similar fragments appear in more than one test, or are likely to be reused, extract them into a shared test utility class.
- Re-run this reuse analysis when adding tests, refactoring tests, and during cleanup passes.

### Naming

- JUnit test method names must follow exactly 3 segments: `subject_condition_expectedResult`.
- `subject` names what is being tested and usually mirrors the production method, command, or feature name.
- `condition` states only the relevant precondition or input shape.
- `expectedResult` states the observable outcome.
- Do not use filler words such as `should`, `when`, `then`, or `must` inside the method name.
- Avoid vague words such as `works`, `ok`, or `smoke1`; prefer intent-revealing words.
- Keep the condition minimal but specific.
- Prefer `<ProductionClassName>Test` for unit tests.
- Prefer `<FeatureOrScenarioName>Test` for integration tests that cover a pipeline.
- If you need multiple scenarios, prefer `@Nested` classes instead of splitting into many test classes.

### Structure

- Each test body must be split into contiguous blocks using comments:
  - `// Given`
  - `// When`
  - `// Then`
- Do not insert empty lines inside a block.
- Insert exactly one empty line between blocks.
- There must be a blank line before `// When` and before `// Then`, and before combined blocks such as `// When / Then`.
- Do not insert an empty line at the very beginning of the method body before `// Given`.
- Keep each block contiguous and focused.
- It is valid to merge blocks when it improves readability.
- Exception tests may use `// When / Then` together because the assertion captures both the action and the expectation.
- Very small tests may use `// Given / When` together if separating them would add noise.
- Combined blocks are allowed only when they stay contiguous and clear.
- Use parameterized tests when they reduce repetition and improve readability.
- The 3-segment method naming rule still applies to parameterized tests.
- Do not introduce a `// Given` block for a single obvious local variable assignment.
- If setup is trivial and self-explanatory, omit `// Given` entirely or use a combined block.
- Use `// Given` only when it groups multiple setup statements or improves readability.

### Fixtures and resources

- Store fixtures under `src/test/resources/test-cases/**`.
- Use explicit scenario folder names instead of generic names such as `example/`.
- Prefer resource fixtures under `src/test/resources/test-cases/**` over large inline YAML or Java strings embedded directly in test classes.
- Use `valid/` for fixtures that must compile and be compilable by the build gate.
- Use `invalid/` for fixtures that may intentionally not compile in negative tests.
- All `valid/**/*.java` fixtures must compile as part of the build.
- Prefer fixtures that are self-contained and depend only on the JDK.
- Prefer single-file fixtures.
- If multiple files are required, keep them in the same scenario folder.
- When a fixture verifies ordering inside one logical group, prefer including multiple declarations of the same kind and cover secondary ordering rules such as visibility and alphabetical order where the language allows it.
- Prefer classpath-based resource access via `ClassLoader.getResourceAsStream` over filesystem paths.
- Use shared helpers such as `TestCaseResourceUtils` to read resources.
- If a regression test verifies the built-in default configuration, load the real embedded `default-config.yml` through the production default-loading path instead of duplicating it in test fixtures or inline YAML.
- Keep resource identifiers as typed values where feasible, such as `URL`, not raw strings.
- Keep resource paths absolute, starting with `/`.
- If you need to resolve a file under a directory, resolve it via a dedicated helper, not via deprecated URL constructors.

### Shared test setup and one-time initialization

- If multiple tests in the same test class use the same expensive or repetitive setup, initialize it once at the test-class level instead of recreating it in every test.
- Prefer `private static final` constants for immutable, shareable objects created once.
- Prefer `private final` fields when per-instance initialization is sufficient and the object is safe to share across tests in the class.
- Use `@BeforeAll` for one-time initialization that cannot be expressed as a simple field initializer.
- If `@BeforeAll` must be non-static, use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`.
- Do not share mutable objects across tests if the code under test may modify them.
- In that case, keep a single immutable base representation and create a fresh copy per test, or initialize the mutable object in `@BeforeEach`.
- Avoid duplicating the same setup snippet across multiple tests in the same class.
- If repeated setup appears, refactor it into a shared field initializer or a dedicated setup method.
- Keep the `// Given` section focused on test-specific inputs; common setup belongs to fields, `@BeforeAll`, or `@BeforeEach`.

### Constants grouping

- Keep a small amount of shared test state as regular fields at the top of the class when that remains easy to scan.
- Good candidates to keep at the top include `@TempDir` fields and one or two obvious shared constants that help the first test read naturally.
- If a test class contains many shared constants and they start cluttering the top of the file, group them into a nested `Constants` class instead of stacking a long constant block before the tests.
- Prefer a nested `Constants` class once the constant list is long enough that it pushes test methods noticeably down the file or makes the start of the class hard to scan.
- Keep the `Constants` nested class at the end of the test class.
- Do not move everything into `Constants` mechanically.
- Keep only the cluttering shared constants there, while ordinary test fields such as `@TempDir` stay near the top.

### Assertions and test utilities

- Do not add dedicated unit tests whose only purpose is to test test-only utility classes or helper methods.
- Validate test utilities indirectly through the real unit and integration tests that use them.
- If a test utility becomes complex enough to deserve direct behavioral tests, move the logic into production code or simplify the helper.
- Place private static helper methods and test-only utility code at the end of the test class, after all test methods and after nested `Constants`, if present.
- Keep the top of the test class focused on test scenarios, not helper implementation details.
- Use assertions only for validating the test contract and expected results.
- Prefer AssertJ with `assertThat(...)`.
- Test utility methods must not call `fail(...)` or use assertions for control flow.
- If a test utility cannot proceed because of missing resources, ambiguous matches, or invalid input, it must throw a descriptive runtime exception.
- Use `IllegalArgumentException` for invalid inputs.
- Use `IllegalStateException` for unexpected setup or state.
- Use `UncheckedIOException` for I/O problems.
- For internal test-only utility classes, prefer Lombok for null checks.
- Use `@NonNull` on parameters instead of `Objects.requireNonNull(...)`.
- Use `@UtilityClass` for pure utility classes.
- If a helper returns `Optional<T>`, treat that as part of the test logic.
- Prefer `findXxx(...)` returning `Optional<T>`.
- Prefer `requireXxx(...)` returning `T` and throwing `IllegalStateException` if missing or ambiguous.
- If presence or absence is a product expectation, assert it in the `// Then` block.
- If absence is a broken fixture or setup, use `requireXxx(...)`.

### Temporary files and formatting

- Tests must not write into `src/test/resources`.
- Use `@TempDir` (JUnit 5) or write into `target/`.
- Avoid inserting empty lines between closely related constant declarations.
- Use a blank line only to separate semantic groups.
- Keep `// Given` immediately after the opening brace.
- In test utility classes, group fields and constants by meaning; do not insert a blank line after every field by default.

### Code style in tests

- Prefer fully descriptive variable names instead of names such as `i`, `tmp`, or `m`.
- Prefer Stream API when it makes the flow clearer.
- Keep helpers small and single-purpose.
