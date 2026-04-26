<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer — test conventions

This file defines the test-specific coding conventions for the JHarmonizer repository.
For repository-wide conventions, see `AGENTS.md`.

---

## Goals

- Make tests readable at a glance with predictable structure.
- Make failures actionable with clear names and error messages.
- Keep maintenance low through shared helpers and minimal duplication.
- Avoid false regressions caused by broken fixtures; fixtures must compile.

---

## Tooling and libraries

- JUnit 5 is the test runner.
- AssertJ is the assertion library.
- Do not use `org.junit.jupiter.api.Assertions.*` in new or updated tests.
- Prefer ordinary imports over repeated fully qualified names in test code.
- Prefer using production pipeline building blocks such as parsers, converters, compilers, and factories instead of test-only reimplementations.
- When an annotation argument in test code only repeats the library or framework default behavior, omit it instead of spelling it out explicitly.
- When test code overrides standard `Object` methods, preserve the standard signature exactly; do not add nullability annotations to `Object` overrides in tests.
- Test code and test resources follow the same repository SPDX policy with the required file-level lines.

---

## Code reuse and deduplication

- Before copying code into another test, search for an existing shared test utility and reuse it.
- If similar fragments appear in more than one test, or are likely to be reused, extract them into a shared test utility class.
- Re-run this reuse analysis when adding tests, refactoring tests, and during cleanup passes.
- When tests in a different package need to instantiate a production type with package-private construction, prefer a dedicated test creator/helper in the target package instead of widening production visibility.

---

## Naming

- JUnit test method names must follow exactly 3 segments: `subject_condition_expectedResult`.
- This naming rule applies only to executable test methods (for example `@Test`, `@ParameterizedTest`, and other JUnit invocation annotations).
- It does not apply to lifecycle and helper methods such as `@BeforeEach`, `@BeforeAll`, `@AfterEach`, `@AfterAll`, or private utilities; name those with normal Java conventions such as `setUp` or `tearDown`.
- `subject` names what is being tested and usually mirrors the production method, command, or feature name.
- `condition` states only the relevant precondition or input shape.
- `expectedResult` states the observable outcome.
- Do not use filler words such as `should`, `when`, `then`, or `must` inside the method name.
- Avoid vague words such as `works`, `ok`, or `smoke1`; prefer intent-revealing words.
- Keep the condition minimal but specific.
- Prefer `<ProductionClassName>Test` for unit tests.
- Prefer `<FeatureOrScenarioName>Test` for integration tests that cover a pipeline.
- If you need multiple scenarios, prefer `@Nested` classes instead of splitting into many test classes.

---

## Structure

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

---

## Fixtures and resources

- Store fixtures under `src/test/resources/test-cases/**`.
- Use explicit scenario folder names instead of generic names such as `example/`.
- Prefer resource fixtures under `src/test/resources/test-cases/**` over large inline YAML or Java strings embedded directly in test classes.
- Do not keep non-trivial multi-line textual fixtures such as YAML, JSON, XML, Java source, or long expected-output snippets inline in test code.
- Do not write large fixture content as inline string literals and then persist it to temp files during test setup.
- Store original, expected, and config fixture files under `src/test/resources/test-cases/**`, then copy or read them via test resource helpers.
- Load them through shared helpers such as `TestCaseResourceUtils`.
- Only tiny one-off inline snippets are acceptable when extracting a file would hurt readability.
- For formatter-focused fixtures under `src/test/resources/test-cases/**`, keep `input/` resources valid but intentionally not already formatted like `expected/`, so the scenario demonstrates a real formatter rewrite.
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
- When using `ClassLoader.getResourceAsStream` (preferred), do not use a leading `/`; classpath resource names are always relative to the classpath root.
- Only when using `Class#getResourceAsStream` should the path start with `/` to indicate an absolute classpath resource.
- If you need to resolve a file under a directory, resolve it via a dedicated helper, not via deprecated URL constructors.

---

## Shared test setup and one-time initialization

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

---

## Constants grouping

- Keep a small amount of shared test state as regular fields at the top of the class when that remains easy to scan.
- Good candidates to keep at the top include `@TempDir` fields and one or two obvious shared constants that help the first test read naturally.
- If a test class contains many shared constants and they start cluttering the top of the file, group them into a nested `Constants` class instead of stacking a long constant block before the tests.
- Prefer a nested `Constants` class once the constant list is long enough that it pushes test methods noticeably down the file or makes the start of the class hard to scan.
- Keep the `Constants` nested class at the end of the test class.
- Do not move everything into `Constants` mechanically.
- Keep only the cluttering shared constants there, while ordinary test fields such as `@TempDir` stay near the top.

---

## Assertions and test utilities

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
- Use `@NonNull` on non-private parameters instead of `Objects.requireNonNull(...)`.
- Use `@UtilityClass` for pure utility classes.
- For test DTO/model/state-holder classes that mainly carry data, prefer immutable Lombok (`@Value`) unless mutation is required for the scenario.
- Private helper method parameters must not use Lombok `@NonNull`; use `@Nullable` only when the helper intentionally accepts `null`.
- If a private test helper returns a reference type, annotate the return contract explicitly with `@NonNull` or `@Nullable`.
- If a helper returns `Optional<T>`, treat that as part of the test logic.
- Prefer `findXxx(...)` returning `Optional<T>`.
- Prefer `requireXxx(...)` returning `T` and throwing `IllegalStateException` if missing or ambiguous.
- If presence or absence is a product expectation, assert it in the `// Then` block.
- If absence is a broken fixture or setup, use `requireXxx(...)`.

---

## Temporary files and formatting

- Tests must not write into `src/test/resources`.
- Use `@TempDir` (JUnit 5) or write into `target/`.
- Avoid inserting empty lines between closely related constant declarations.
- Use a blank line only to separate semantic groups.
- Keep `// Given` immediately after the opening brace.
- In test utility classes, group fields and constants by meaning; do not insert a blank line after every field by default.

---

## Code style in tests

- Prefer fully descriptive variable names instead of names such as `i`, `tmp`, or `m`.
- Prefer Stream API when it makes the flow clearer.
- Keep helpers small and single-purpose.

---

## JUnit 5 visibility rules

JUnit 5 discovers test and lifecycle methods through reflection and does not require `public` visibility.
Keeping test classes and annotated methods package-private reduces noise and aligns with the minimal-access-level principle.

### Conventions (applied when writing or reviewing test code now)

- JUnit 5 test classes should use package-private visibility by default (no access modifier).
- JUnit 5 test methods annotated with `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, or `@TestTemplate` should use package-private visibility by default.
- JUnit 5 lifecycle methods annotated with `@BeforeEach`, `@AfterEach`, `@BeforeAll`, or `@AfterAll` should use package-private visibility by default.
- `@Nested` test classes should use package-private visibility by default.
- Helper methods, constants, and helper nested classes that are only used inside one test class must remain `private`.
- Shared test utilities that are reused across test classes in the same package may remain package-private.
- `public` must not be added to test classes or annotated test/lifecycle methods unless there is a documented technical reason (for example, a test framework extension that requires public access by reflection from outside the package).
- `private` must not be applied to JUnit test or lifecycle methods; JUnit Jupiter must be able to discover them.

### Rationale

- JUnit Jupiter discovers tests via reflection and works with package-private methods since JUnit 5.
- `public` on test classes and annotated methods is purely cosmetic; it adds no functionality and creates misleading signals about API surface.
- Enforcing package-private visibility keeps test classes consistent with the general minimal-access-level rule.

### Future automated enforcement

This convention will eventually be enforced automatically by JHarmonizer.
See `docs/TODO.md` — item **23. JUnit 5 test visibility normalizer** for the planned implementation.
