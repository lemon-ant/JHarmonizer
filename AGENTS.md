# Agent conventions

This file contains repository-wide conventions for coding agents. Test-specific rules live in `docs/test-conventions.md`.

## Default maintenance rule

- During every task, look for stable conventions that become clear from review feedback or repeated user guidance.
- If a rule is missing, unclear, or no longer accurate in the current docs, update `AGENTS.md` and/or `docs/test-conventions.md` in the same task.

## Coding conventions

- Keep changes surgical and avoid unrelated cleanup.
- Do not introduce Java records in production code or shared test infrastructure; prefer regular classes with Lombok where appropriate.
  - Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record handling.
- If a utility is shared across processing phases (for example translator and sorter), place it in a neutral package instead of under a phase-specific package.

## Testing conventions

- Follow `docs/test-conventions.md` for test naming, structure, fixtures, and helper usage.
- Prefer resource fixtures under `src/test/resources/test-cases/**` over large inline YAML/Java strings in tests.
