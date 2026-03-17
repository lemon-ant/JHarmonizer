# AGENTS.md

This file defines repository-wide conventions for coding agents working in this repository.

## Scope

- Applies to the whole repository unless a more specific convention document says otherwise.
- For test-specific details, also read `docs/test-conventions.md`.

## Convention maintenance

- During every task, look for stable conventions that become clear from review feedback or repeated user guidance.
- Keep `AGENTS.md`, `docs/test-conventions.md`, and `.github/copilot-instructions.md` aligned.
  - `AGENTS.md` defines the repository-wide rules.
  - `docs/test-conventions.md` defines the test-specific rules.
  - `.github/copilot-instructions.md` must contain the complete operative rule set from both files so Copilot can follow it without relying on cross-file traversal.
- If a rule is missing, unclear, or no longer accurate in the current docs, update all affected instruction files in the same task.
- If a documented rule is ambiguous, clarify the document rather than relying on unwritten expectations for future sessions.

## General code conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Keep changes surgical and avoid unrelated cleanup.
- Reuse existing project and library utilities before introducing custom helpers.
- Prefer explicit Java types over `var`.
- Reference-returning private methods must declare explicit `@NonNull` or `@Nullable` return annotations.
  - Private method parameters do not need nullability annotations just because the method is private.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- Repository-wide convention: do not introduce Java records in production code or shared test infrastructure; use classes with Lombok instead where appropriate.
  - Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record handling.
- If a utility is shared across processing phases (for example translator and sorter), place it in a neutral package instead of under a phase-specific package.
- Non-obvious build/configuration workarounds (for example temporary dependency overrides for transitive vulnerabilities) must include a nearby comment that explains why the workaround exists, which upstream component requires it, and when it can be removed.

## Test conventions

- See `docs/test-conventions.md`.
