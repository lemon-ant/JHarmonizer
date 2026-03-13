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
- Do not introduce Java records anywhere in the repository. Use classes, and prefer Lombok to remove boilerplate where appropriate.

## Test conventions

- See `docs/test-conventions.md`.

## Convention maintenance

- If agent/user review feedback reveals a new stable repository convention that is missing here or in `docs/test-conventions.md`, update the relevant convention file as part of the same task.
- If a documented rule is ambiguous, clarify the document rather than relying on unwritten expectations for future sessions.
