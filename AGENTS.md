# AGENTS.md

This file defines repository-wide conventions for coding agents working in this repository.

## Scope

- Applies to the whole repository unless a more specific convention document says otherwise.
- For test-specific details, also read `docs/test-conventions.md`.

## General code conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Reuse existing project and library utilities before introducing custom helpers.
- Prefer explicit Java types over `var`.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- For test-only internal data holders and utilities, prefer classes with Lombok annotations over Java records when a simple value/helper type is needed.
- If a helper failure means broken test setup or broken fixture, throw a descriptive exception instead of using assertions.

## Test conventions

- Test method names must contain exactly three underscore-separated camelCase segments:

  `subject_condition_expectedResult`

- Do not use filler words in test method names, including `should`, `when`, `then`, `must`, and similar words that do not add meaning.
- Store fixtures and expected outputs under `src/test/resources/test-cases/**` instead of embedding large inline strings in tests.
- If several tests in one class share expensive or repetitive setup, initialize it once at field level or in one-time setup instead of rebuilding it before every test.
- Prefer existing assertion/library features for file and filesystem checks before adding custom comparison logic.

## Convention maintenance

- If agent/user review feedback reveals a new stable repository convention that is missing here or in `docs/test-conventions.md`, update the relevant convention file as part of the same task.
- If a documented rule is ambiguous, clarify the document rather than relying on unwritten expectations for future sessions.
