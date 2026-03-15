# AGENTS.md

This file defines repository-wide conventions for coding agents working in this repository.

## Scope

- Applies to the whole repository unless a more specific convention document says otherwise.
- For test-specific details, also read `docs/test-conventions.md`.

## Convention maintenance

- During every task, look for stable conventions that become clear from review feedback or repeated user guidance.
- If a rule is missing, unclear, or no longer accurate in the current docs, update `AGENTS.md` and/or `docs/test-conventions.md` in the same task.
- If a documented rule is ambiguous, clarify the document rather than relying on unwritten expectations for future sessions.

## General code conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Keep changes surgical and avoid unrelated cleanup.
- Reuse existing project and library utilities before introducing custom helpers.
- Prefer explicit Java types over `var`.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- Do not use `protected` fields; keep fields `private` and expose only the narrow protected accessor methods that subclasses actually need.
- Prefer the shorter `src*` naming family (`srcFile`, `srcPath`, `srcCode`, `srcDiff`) for source-related variables and parameters.
- Repository-wide convention: do not introduce Java records in production code or shared test infrastructure; use classes with Lombok instead where appropriate.
  - Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record handling.
- If a utility is shared across processing phases (for example translator and sorter), place it in a neutral package instead of under a phase-specific package.

## Test conventions

- See `docs/test-conventions.md`.
