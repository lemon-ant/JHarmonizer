# GitHub Copilot instructions for JHarmonizer

- Read `AGENTS.md` before making changes. It contains the repository-wide coding conventions.
- Read `docs/test-conventions.md` before adding or updating tests.
- Prefer the smallest complete change that solves the task. Avoid unrelated cleanup.
- Reuse existing project and library utilities before introducing new helpers.
- In Java code, prefer explicit types over `var`.
- Do not introduce Java records in production code or shared test infrastructure.
- If a workaround is non-obvious, document why it exists, which upstream dependency or tool requires it, and when it can be removed.
- This repository uses JUnit 5 and AssertJ for tests.
- Test method names must follow the 3-part convention `subject_condition_expectedResult`.
- Structure tests with contiguous `// Given`, `// When`, and `// Then` blocks, separated by exactly one blank line.
- Prefer fixtures under `src/test/resources/test-cases/**` over large inline source strings. Keep `valid/**/*.java` fixtures compilable.
- Build and validate with JDK 21. The standard repository command is `mvn -B -ntp verify`.
