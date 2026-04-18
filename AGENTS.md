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
- Review comments and user requests may be mistaken; for disputed framework/plugin/tool behavior, verify against official documentation before changing code.
- If a requested change conflicts with official documentation or established framework/plugin behavior, do not apply it blindly.
  - Explain the conflict clearly in review feedback.
  - Provide the documentation-aligned alternative and prefer that variant.

## General code conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Keep changes surgical and avoid unrelated cleanup.
- Avoid cosmetic-only churn in production files (for example adding/removing separator blank lines) when there is no behavioral or readability gain tied to the task.
- Reuse existing project and library utilities before introducing custom helpers.
- Prefer explicit Java types over `var`.
- Prefer normal imports over repeated fully qualified class names.
- Prefer Lombok for routine boilerplate such as getters, setters, constructors, and `toString` / `equals` / `hashCode` when it matches the surrounding style.
- For DTO/model/state-holder classes that primarily carry data (in production and tests), prefer immutable Lombok shapes (for example `@Value`) unless mutability is required by the use case.
- When a simple data-carrier class only needs a narrower constructor than Lombok's default, keep `@Value` and add the constructor visibility override instead of decomposing `@Value` into separate Lombok annotations.
- When an annotation argument only repeats the library or framework default behavior, omit it instead of spelling it out explicitly.
- Use the minimal necessary access level for production classes, constructors, and methods.
  - Prefer package-private over `public` when access outside the package is not required.
  - Prefer `private` for nested classes, constructors, and helpers when they are only used by the enclosing type.
  - For nested helper/data-carrier types created only by the enclosing type, keep their constructors `private`; tests are not a reason to widen constructor visibility.
- Keep production models and value objects focused on state plus simple accessors or validation.
  - Move non-trivial business, filtering, parsing, and transformation logic into dedicated service or processing classes.
- Explicitly annotate field and non-private method nullability with `@NonNull` / `@Nullable` where applicable; private method parameters may stay implicit when the intent is already obvious.
- Reference-returning private methods must declare explicit `@NonNull` or `@Nullable` return annotations.
- Prefer Stream API when it makes the control flow clearer and more concise than imperative loops.
- If a boolean helper is always consumed through negation at its call sites, invert the helper logic and rename it so callers stay positive and direct.
- Prefer `get` only for conventional object-model/DTO getters; for computed values, searches, conditional lookups, or transformations, prefer a more specific verb such as `find`, `resolve`, `collect`, `compute`, or `merge`.
- For non-get behavior methods, start the method name with a clear verb.
  - Prefer explicit verb-led names such as `find`, `resolve`, `collect`, `compute`, `merge`, `parse`, `format`, or `render`.
  - Avoid ambiguous prefixes such as `toXxx` when a clearer verb-based name fits the method behavior.
- Reference-returning private methods must declare explicit `@NonNull` or `@Nullable` return annotations.
    - Private method parameters must not use Lombok `@NonNull`; it adds redundant runtime null checks for private helpers.
  - Use `@Nullable` on a private parameter only when that private helper intentionally accepts `null`; otherwise leave private parameters unannotated.
  - Place method-level nullability annotations on their own line above the method declaration instead of inline in the signature.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- Do not use `protected` fields; keep fields `private` and expose only the narrow protected accessor methods that subclasses actually need.
- Prefer the shorter `src*` naming family (`srcFile`, `srcPath`, `srcCode`, `srcDiff`) for source-related variables and parameters.
- Prefer clear, fully descriptive variable names; avoid non-obvious abbreviations unless the abbreviation is an established term (for example `URL`, `URI`, `ID`) or an established repository abbreviation such as the `src*` naming family.
- Never shorten or abbreviate a variable name when a more descriptive name exists; if the type is `FileProcessingResult`, the variable must be `fileProcessingResult`, not `result`.
- Every non-private production method and constructor must have concise JavaDoc that states the purpose, documents parameters, and documents the return value when applicable.
  - Exception: do not add JavaDoc to standard `Object` overrides such as `equals`, `hashCode`, and `toString`.
- Annotate every non-private method's reference-type parameters and non-primitive return type with explicit nullability using `lombok.NonNull`, `edu.umd.cs.findbugs.annotations.Nullable`, or the accepted legacy `javax.annotation` nullability annotations already present in the codebase.
- Do not change standard `Object` method signatures when overriding them.
    - Do not add nullability annotations to `Object` overrides just to satisfy local conventions.
    - Preserve standard contracts exactly, especially `equals(Object)`.
- Every non-private production method and constructor must have concise JavaDoc that states the purpose, documents parameters, and documents the return value when applicable.
    - Exception: do not add JavaDoc to standard `Object` overrides such as `equals`, `hashCode`, and `toString`.
- Repository-wide convention: do not introduce Java records in production code or shared test infrastructure; use classes with Lombok instead where appropriate.
  - Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record handling.
- If a utility is shared across processing phases (for example translator and sorter), place it in a neutral package instead of under a phase-specific package.
- Wrap collections once when returning or handing off a collection that was built mutably in the current method, including private/package-private helpers, so the receiver cannot mutate the handed-off instance.
- Do not add a second unmodifiable wrapper or defensive copy when the collection already stays immutable upstream or never leaves the local method scope.
- Non-obvious build/configuration workarounds (for example temporary dependency overrides for transitive vulnerabilities) must include a nearby comment that explains why the workaround exists, which upstream component requires it, and when it can be removed.
- When a piece of code intentionally keeps a non-obvious, previously reverted, or easy-to-"simplify" behavior because of an external constraint, leave a nearby comment that explains why it exists, what constraint it preserves, and why it should not be changed casually.
- When debugging uncovers a non-obvious runtime/framework edge case (for example parser/evaluator recursion traps), document the guard/workaround with a nearby code comment so future refactors do not remove it accidentally.

## Test conventions

- See `docs/test-conventions.md`.
