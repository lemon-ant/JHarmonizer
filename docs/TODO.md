# JHarmonizer — backlog of design & performance ideas

This file is a living backlog of ideas that we intentionally postpone until **after** the first working version
(build + tests + runnable CLI) is stabilized.

Guiding rule: **do not optimize early**. We capture ideas here to avoid losing them, then revisit them in later versions
when the pipeline is proven end-to-end.

---

## Backlog buckets

To keep planning explicit, this backlog is split into two categories:

1) **Technical debt / stabilization backlog**  
   Items that improve correctness, safety, architecture, or maintainability of existing behavior.

2) **Planned future features (new product functionality)**  
   Items that extend user-visible capabilities in upcoming versions.

---

## Planned future features (new product functionality)

### 1. Compile group sorting once and precompute ordering rule values in `MemberDescriptor` (performance-focused)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: tool runs end-to-end + tests are green
- [ ] Priority context: performance/scalability improvement for large source sets

#### Background
JHarmonizer already has a compiled layer for grouping/classification:
selectors and rule blocks are compiled once into “ready-to-run” predicates, so we can classify `CtTypeMember`s efficiently.

Sorting is still “runtime-heavy”:
- For each group, we rebuild comparator chains based on `OrderingRule`s.
- We compute ordering rule values (alpha key, visibility rank, signature key, etc.) repeatedly.
- We introduced extra wrapper DTOs to hold those values, but they are not integrated into the compiled pipeline.

#### Problem statement
We want sorting to be as “compiled” and deterministic as grouping:
- No repeated comparator construction per group.
- No repeated computation of ordering rule values per member.
- Cleaner separation of concerns: *classification prepares data*; *sorting consumes prepared data*.

#### Proposed solution
**Move sorting compilation to the same stage as selector compilation.**

1) **Extend `MemberDescriptor`** to hold all computed values needed for sorting:
- `sourceStart` / source position data
- `alphaKey`
- `visibilityRank` (or ranks for ASC/DESC derived from a base rank)
- `signatureKey`
- accessor-related facts used by `keepAccessorsTogether` (property name, accessor kind, return/param type keys, etc.)
- any deterministic tie-breaker values currently derived on-the-fly

2) **Compile a `Comparator<MemberDescriptor>` once per compiled member group**, based on:
- group `OrderingRule`s (PRESERVE / ALPHA / SOURCE_ORDER / VISIBILITY_ASC / VISIBILITY_DESC / SIGNATURE)
- stable tie-breakers (e.g., sourceStart, signature, deterministic id) to guarantee deterministic output

3) **Reuse `MemberDescriptor` objects throughout the pipeline**:
- The classification step (group selector) consumes the descriptor to decide membership.
- The ordering step sorts descriptors using the already compiled comparator.
- Finally, the renderer uses the stored reference to the original member to reconstruct text.

#### Design details

#### A. Descriptor-first pipeline
Instead of passing raw `CtTypeMember` around, we create descriptors once:

- Input: `List<CtTypeMember> typeMembers`
- Map: `CtTypeMember -> MemberDescriptor`
- Group selection: uses compiled predicates on `MemberDescriptor`
- Sorting: uses compiled `Comparator<MemberDescriptor>` from the compiled group
- Output: sorted `CtTypeMember` list via `descriptor.originalMember()`

#### B. Where the comparator lives
Store the comparator on the compiled group (or next to it), for example:
- `CompiledMemberGroupSortingBehavior` (or similar) holds:
  - `List<OrderingRule> orderingRules`
  - `boolean keepAccessorsTogether`
  - `Comparator<MemberDescriptor> compiledComparator`

This keeps sorting decisions co-located with other compiled group semantics.

#### C. Keep `MemberDescriptor` group-agnostic
`MemberDescriptor` should store *raw facts* and computed keys, not group-specific decisions.

Example:
- Descriptor stores `visibilityRankBase` (or canonical rank).
- Comparator decides ASC vs DESC by comparing ranks in different directions.

#### D. Optional phase-2: generic descriptor
As a follow-up (not part of this item), consider:
- `MemberDescriptor<M>` where `M` is the underlying AST member type (currently `CtTypeMember`).
- Graph units / bundles / representative members could become generic too.

This is **explicitly deferred** until the non-generic version proves beneficial.

#### Expected benefits
- Performance: one-time descriptor construction + one-time per-group comparator compilation.
- Cleaner architecture: sorting logic becomes “compiled config” rather than ad-hoc runtime plumbing.
- Consistency: grouping and sorting follow the same “compile once, run many” model.
- Better testability: comparator behavior can be unit-tested using synthetic descriptors.

#### Non-goals
- Do not refactor the entire dependency-graph subsystem as part of this item.
- Do not generalize away from Spoon in the first implementation of this idea.
- Do not change output semantics (only reduce repeated work and improve structure).

#### Implementation outline (when we revisit this)
- [ ] Identify current “ordering rule values wrapper” DTO(s) and list the computed values required.
- [ ] Extend `MemberDescriptor` to include those values + a reference to the original member.
- [ ] Update the descriptor factory to compute keys once (single pass).
- [ ] Add `Comparator<MemberDescriptor>` compilation to the compiled group stage.
- [ ] Refactor group sorting to sort descriptors using the compiled comparator.
- [ ] Ensure deterministic tie-breakers remain identical to the current behavior.
- [ ] Add unit tests:
  - [ ] comparator correctness for each `OrderingRule`
  - [ ] stable tie-breaking
  - [ ] `keepAccessorsTogether` scenarios

---

### 2. Add a type-based selector to rule lines (field type / method return type)

#### Status
- [ ] Not implemented (explicitly deferred to the next product version)
- [ ] For the first working version, match special fields (e.g., logger fields) **by name only**

#### Background
The current selector model supports:
- kind / access / modifiers
- name matchers (EXACT / REGEX)
- annotation matchers (EXACT / REGEX; FQCN-or-simple)

This is enough for many “layout” rules, but it cannot express common real-world grouping needs like:
- “all `Logger` fields”
- “all methods returning `Optional<T>` / `Stream<T>` / `CompletableFuture<T>`”
- “fields of type `Pattern` / `ObjectMapper` / `Clock`”, etc.

#### Problem statement
Without a type selector, some default grouping rules are forced to rely on naming conventions, which:
- is less precise (false positives / negatives),
- is inconsistent across projects,
- makes configuration harder to reason about.

#### Decision for the first working version
To avoid scope creep and ensure we ship a working end-to-end tool:
- **Do not implement type-based matching in this version.**
- In Default Rule, match `serialVersionUID` and logger fields **by name** (EXACT / REGEX) within the “static final fields” subgroup.

#### Proposed solution (next version)
Introduce a new selector atom for rule lines: **type matcher**.

- Match styles: EXACT / REGEX (same as name/annotation matchers).
- Accept both **FQCN and simple name** (same approach as annotation matchers).
- Target mapping:
  - FIELD → declared field type
  - METHOD → return type
  - RECORD_COMPONENT → component type (optional)
  - TYPE declarations → qualified name (optional)
  - CONSTRUCTOR / initializer blocks → no match (type matcher never matches)

#### Design details (next version)

#### A. Type normalization
To keep matching stable:
- compare on erasure (ignore generic arguments),
- optionally apply boxing/unboxing normalization where appropriate,
- support matching both qualified and simple type names.

#### B. YAML token model
The DSL should document:
- `type.exact: "org.slf4j.Logger"` / `type.exact: "Logger"`
- `type.regex: ".*Logger"` etc.

#### Expected benefits
- Much more expressive configuration for real projects.
- Cleaner defaults (logger fields detected by type, not naming convention).
- Fewer “special-case” groups that rely on heuristics.

#### Non-goals
- Do not introduce full semantic typing (imports resolution, type inference) in selectors.
- Do not change current selector semantics for name/annotation.

#### Implementation outline (when we revisit this)
- [ ] Extend the unified rule-line model to carry a type matcher (optional).
- [ ] Implement compilation of the matcher into a predicate on `CtTypeMember`.
- [ ] Introduce a type name extraction utility (field type / method return type).
- [ ] Add tests for:
  - [ ] EXACT FQCN vs simple-name matching
  - [ ] REGEX matching
  - [ ] generic erasure behavior
  - [ ] deterministic behavior across different source forms

---

### 3. Handle enum constants ordering explicitly (future work)

#### Status
- [ ] Not implemented (explicitly deferred to the next product version)
- [ ] Current behavior: enum constants remain **as-is** (original source order)

#### Background
Enum constants are not regular fields and have strict placement rules in Java source:
they appear at the top of an enum body, before other members.

The current Default Rule does not model enum constants explicitly.
In practice, this means:
- we do not attempt to re-order enum constants,
- they are preserved in their original order.

#### Problem statement
Projects often want a deterministic enum constant order, or at least a documented policy.
Without an explicit rule:
- enum constants may remain inconsistent across files,
- users cannot express “keep as-is” vs “sort constants” as a configurable choice.

#### Proposed solution (next version)
Add explicit enum-constant handling with a configurable strategy.

Two initial strategies to support:
1) **PRESERVE** — keep enum constants in original order (current behavior).
2) **ALPHA** — sort constants alphabetically, with a direction:
   - ALPHA_ASC (A → Z)
   - ALPHA_DESC (Z → A)

Placement rule (still mandatory):
- enum constants must be printed before other enum members.

#### Expected benefits
- Deterministic ordering for enums when desired.
- Clear, documented behavior instead of an implicit “we ignore them”.

#### Non-goals
- Do not attempt to reorder enum constants based on initializer complexity or “length”.
- Do not introduce semantic grouping of enum constants in the first iteration.

#### Implementation outline (when we revisit this)
- [ ] Model enum constants as a dedicated member kind in classification.
- [ ] Ensure enum constants are emitted before other enum members in rendering.
- [ ] Implement `PRESERVE`, `ALPHA_ASC`, `ALPHA_DESC` strategies.
- [ ] Add unit tests:
  - [ ] preserve stability
  - [ ] alpha ordering in both directions
  - [ ] interaction with dependency constraints (if any are later introduced)


---

### 4. Inter-procedural initializer dependencies (field default expression -> method calls)

#### Status
- [ ] Not implemented (verified against current dependency providers)
- [ ] Deferred to a future version because it is complex and can significantly increase analysis cost

#### Why this is needed
Current declaration dependency detection handles direct field references found in initializer-like AST roots
(field initializer, init blocks, enum constant initializer, etc.).

A missing case:
- field `A` default expression calls method `m()`;
- `m()` reads field `B` (or calls `m2()` that reads `B`);
- therefore `A` is implicitly order-dependent on `B`, even if `B` is not referenced directly in `A` initializer expression.

If we ignore this case, we may reorder members in a way that is unsafe for initialization semantics.

#### Verified current behavior (why this item stays TODO)
- Dependency providers currently collect dependencies from direct field access scanning in initializer roots.
- No provider in the graph builder performs inter-procedural traversal of method bodies from initializer call sites.
- No call-graph / recursion-aware traversal is present in dependency provider chain.

Conclusion: indirect dependencies through called methods are not modeled yet.

#### Proposed solution (future)
Add a new declaration dependency provider for initializer call chains:

1) For initializer-like dependent members, find method invocations in the initializer AST.
2) Resolve called methods that belong to the same declaring type.
3) Traverse method bodies to collect field reads relevant to initialization ordering.
4) Recursively follow nested same-type method calls to build transitive dependencies.
5) Add `DECLARATION_DEPENDENCY` edges from referenced provider fields to the original dependent member.

#### Safety / complexity requirements
- Recursion and cycles:
  - maintain a visited call stack per traversal to avoid infinite recursion;
  - strongly connected call components should not crash analysis.
- Conservative mode:
  - unresolved/dynamic dispatch cases should degrade safely (do not produce unsound reorderings).
- Performance:
  - cache per-method analyzed field reads;
  - avoid repeated traversal for the same method.
- Scope control:
  - first iteration can be limited to same-type, non-overridden method resolution.

#### Non-goals (first iteration)
- Full inter-class call graph.
- Precise runtime dispatch modeling across inheritance hierarchies.
- Side-effect inference beyond field read/write dependencies required for declaration safety.

#### Implementation outline (when we revisit this)
- [ ] Add `InitializerMethodCallDependencyProvider` to dependency providers chain.
- [ ] Implement same-type call resolution utility for Spoon method invocations.
- [ ] Implement recursion-safe transitive method traversal and field-read extraction.
- [ ] Add memoization/cache for per-method dependency summaries.
- [ ] Add tests:
  - [ ] direct method call from field initializer to field read
  - [ ] nested method-call chain (`a -> m1 -> m2 -> field`)
  - [ ] recursive method self-call / mutual recursion stability
  - [ ] unresolved invocation fallback behavior
  - [ ] interaction with existing direct initializer dependencies


### 5. Explicit-declaring-type instance forward-reference corner-case (parked failing E2E)

#### Status
- [ ] Not implemented (known failing scenario discovered in E2E)
- [ ] Temporarily parked from active `*.java` fixtures until dependency analysis is fixed

#### Case summary (what exactly happens)
Scenario class (`ExplicitTypeInstanceReferrerForwardReference...`) uses this pattern:
- static field `zzz` is initialized via `new Sample().aaa`;
- instance field `aaa` reads `Sample.bravo + 1`;
- static field `bravo` is declared later.

The value observed at runtime depends on declaration order and class-init timing:
- in the input variant, `zzz` is evaluated before `bravo` initializer runs, so `aaa` observes default static value and `zzz == 1`;
- in the reordered variant, `aaa` is moved above `zzz`, which changes initialization sequence assumptions and breaks expected semantics.

#### Why current implementation fails
Current dependency handling covers direct declaration dependencies and several local initializer cases, but does not yet fully protect this mixed pattern:
- explicit declaring-type field access from instance initializer;
- cross interaction between instance construction and static initialization ordering;
- required constraints to keep runtime-observable values stable after reordering.

As a result, sorter can produce an order that is syntactically valid but semantically different at runtime.

#### What to implement (plan)
1. Re-enable this scenario as an active `.java` E2E fixture and confirm it fails reproducibly in `REORDER + CHECK` flow.
2. Extend dependency extraction for field initializers to model explicit declaring-type static reads inside instance initializers used by static field initialization chains.
3. Add conservative ordering constraints so members participating in such chains are not moved across unsafe boundaries.
4. Re-run full E2E + compile + runtime assertions; then keep scenario active as regression guard.

#### Corner-case family to cover next
- Same pattern with primitive default values (`0`, `false`, `\u0000`) and `null`.
- Longer chains (`static -> new instance -> instance field -> static field -> helper method`).
- Mixed `this.` and `TypeName.` references in one initializer graph.
- Cycles with SCC bundling to ensure no unstable reorder loops.

#### Very short note on temporary parking mechanism
Current fixture is parked via escaped extension only as a temporary unblock step.
Parked fixture files (excluded from automatic E2E sweep):
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/ExplicitTypeInstanceReferrerForwardReferenceEscapedKnownIssueSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/ExplicitTypeInstanceReferrerForwardReferenceEscapedKnownIssueSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/F01StaticNewInstanceTypeFieldInt0EscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/F01StaticNewInstanceTypeFieldInt0EscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/F02StaticNewInstanceTypeFieldBoolEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/F02StaticNewInstanceTypeFieldBoolEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/F03StaticNewInstanceTypeFieldNullEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/F03StaticNewInstanceTypeFieldNullEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/F04StaticNewInstanceTypeFieldLongChainEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/F04StaticNewInstanceTypeFieldLongChainEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/input/FQ01OuterThisAnonInitEscapedSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/10-field-initializer-explicit-type-forward-chain/expected/FQ01OuterThisAnonInitEscapedSample.~java`
Long-term target: remove parking, keep the case active, and make the pipeline pass.

Related parked lazy-context fixture (separate backlog track):
- `core/src/test/resources/test-cases/core/e2e/reorder/11-lazy-initializer-contexts/input/LazyMethodReferenceContextEscapedPendingImplementationSample.~java`
- `core/src/test/resources/test-cases/core/e2e/reorder/11-lazy-initializer-contexts/expected/LazyMethodReferenceContextEscapedPendingImplementationSample.~java`

---

### 6. Adaptive static-import optimizer (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Why this is needed
Large type-qualified calls and constants can reduce readability when repeated often,
while excessive static imports can make source harder to scan.

#### Proposed solution (next version)
Introduce an import optimizer that:
- detects frequent static candidates (constants and static methods),
- adds/removes static imports automatically,
- keeps usage explicit for infrequent candidates,
- enforces a configurable upper threshold for static import count per file.

#### Candidate scoring (initial direction)
- frequency of usage in file;
- qualified-name length savings (prefer long owner class names);
- optional allow/deny patterns by package/type/member.

#### Safety requirements
- never exceed configured static-import budget;
- preserve deterministic output between runs;
- avoid collisions/ambiguity when multiple static members share names.

#### Implementation outline (when revisited)
- [ ] Add config model for static-import budget and scoring strategy.
- [ ] Build per-file usage index for static members.
- [ ] Add deterministic add/remove planner for static imports.
- [ ] Add collision resolution rules and regression tests.

---

### 7. Annotation ordering policies (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
When declarations have many annotations, order quickly becomes inconsistent and noisy in diffs.

#### Proposed solution (next version)
Add configurable annotation ordering modes:
- `ALPHA` (alphabetical by normalized annotation name),
- `LENGTH_ASC` / `LENGTH_DESC`,
- combined policy (primary by length, secondary alphabetical tie-breaker).

#### Scope
- types, methods, constructors, fields, parameters, record components.

#### Implementation outline (when revisited)
- [ ] Add annotation-order strategy to config DSL.
- [ ] Normalize names (simple vs qualified) before comparison.
- [ ] Preserve relative order for exact ties to keep output stable.
- [ ] Add fixture coverage for all supported declaration kinds.

---

### 8. Collapse single-value annotation array braces (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
For annotation attributes declared as arrays, Java allows omitting braces when only one value is passed.
Keeping braces in single-value cases adds visual noise and creates avoidable diff churn.

#### Proposed solution (next version)
Add an automatic rewrite that transforms single-element annotation array arguments into a concise form
without redundant braces when syntax and semantics stay equivalent.

#### Implementation outline (when revisited)
- [ ] Detect annotation array arguments with exactly one element.
- [ ] Rewrite to brace-less single-value form where Java grammar permits it.
- [ ] Skip cases where formatting/printing would become ambiguous.
- [ ] Add round-trip fixtures to verify semantic equivalence and stable output.

---

### 9. Constants ordering expansion (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Additional future scope
- explicit ordering policies for regular constants groups beyond current defaults;
- shared configurable policy layer so constants ordering rules stay consistent.

#### Implementation outline (when revisited)
- [ ] Add constants-group ordering options with stable tie-breakers.
- [ ] Add deterministic ordering tests for constants-heavy classes.

---

### 10. Record-member ordering policies (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Record components/fields can become inconsistently ordered across files and modules,
but this area should be configurable separately from enum/constants ordering.

#### Proposed solution (next version)
Add dedicated ordering strategies for record members:
- alphabetical ordering;
- declaration-preserving mode;
- optional multi-key ordering with deterministic tie-breakers.

#### Safety requirements
- preserve Java record semantics and generated member contracts;
- keep deterministic output across repeated runs;
- avoid rewriting that changes runtime behavior or binary compatibility unexpectedly.

#### Implementation outline (when revisited)
- [ ] Add record-member ordering strategy options to config.
- [ ] Implement stable comparator with clear tie-break hierarchy.
- [ ] Add focused fixtures for records with multiple components.

---

### 11. Static-candidate conversion analyzer (instance -> static) (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Some members are instance methods/fields but do not depend on instance state (or depend on very little),
so they can be promoted to static for clarity and explicit dependencies.

#### Proposed solution (next version)
Add dependency-graph analysis to detect members that can safely become static:
- detect methods with no `this`/instance-field dependency;
- detect fields that are instance-declared but semantically static candidates;
- optionally support controlled refactor mode for near-static methods by extracting required instance data into parameters.

#### Safety requirements
- preserve behavior (including override/inheritance constraints);
- skip members where static conversion breaks API or framework contracts;
- provide conservative mode by default.

#### Implementation outline (when revisited)
- [ ] Build instance-dependency classifier on top of member dependency graph.
- [ ] Add refactoring guardrails (override checks, reflective usage risk flags).
- [ ] Add optional rewrite mode for parameterized utility extraction.
- [ ] Add compile + behavior regression fixtures.

---

### 12. Package dependency cycle detector (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Packages inside one application/module should form a clean hierarchy.
Cyclic package dependencies are a design smell and should be detected early.

#### Proposed solution (next version)
Add an analyzer that builds package-level dependency graph and reports cycles:
- construct directed graph from inter-package type references;
- detect strongly connected components (SCC);
- report cycle chains with actionable refactoring hints.

#### Safety requirements
- support exclusions for generated code and known external bridge packages;
- keep analysis deterministic and reproducible in CI;
- provide configurable severity (warning/error) and fail-on-cycle mode.

#### Implementation outline (when revisited)
- [ ] Add package-graph extractor from existing AST/model pipeline.
- [ ] Implement SCC detection and cycle reporting formatter.
- [ ] Add config for include/exclude packages and severity mode.
- [ ] Add fixtures with single-cycle and multi-cycle package graphs.

---

### 13. Visibility minimization pass (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Projects often keep broader visibility than necessary (`public`/`protected`) for classes,
constructors, methods, and fields that are only used in narrower scopes.

#### Proposed solution (next version)
Introduce a configurable pass that lowers visibility to the minimal safe level:
- classes / nested classes,
- constructors,
- methods,
- fields.

#### Safety requirements
- do not break external API/public contracts when project is a library;
- honor framework entry points and reflection-based usage allowlists;
- produce an explicit report of every reduced-visibility change.

#### Implementation outline (when revisited)
- [ ] Add symbol-usage analysis across module boundaries.
- [ ] Add configurable API boundary definitions.
- [ ] Implement safe downgrade planner with dry-run mode.
- [ ] Add regression tests for framework-specific entry points.

---

### 14. Nullability interfaces / annotations enforcer (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Teams need consistent nullability annotation coverage,
but manual enforcement is repetitive and drifts over time.

#### Proposed solution (next version)
Add a nullability policy analyzer/fixer with configurable minimum scope:
- `public` only, or
- `public + package-private`, etc.

The analyzer validates and auto-fixes missing/incorrect nullability annotations for fields, methods,
and method parameters according to configured scope.

#### Safety requirements
- honor existing repository conventions and exclusions;
- avoid changing `Object` override signatures/behavior;
- support warning-only mode before auto-fix mode.

#### Implementation outline (when revisited)
- [ ] Add configurable nullability scope policy to DSL.
- [ ] Implement nullability inference/validation + fix planner.

---

### 15. Lombok policy enforcer and optimizer (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
When Lombok is available, projects often keep a mix of manual boilerplate and partial Lombok usage,
which increases maintenance cost and style drift.

#### Proposed solution (next version)
Add a dedicated Lombok analyzer/fixer:
- detect Lombok availability and supported annotations per module;
- replace eligible boilerplate with Lombok annotations;
- remove redundant manual code and redundant Lombok annotations.

#### Safety requirements
- preserve behavior and generated API contracts;
- honor project-level exclusions and style constraints;
- support warning-only mode before auto-fix mode.

#### Implementation outline (when revisited)
- [ ] Detect Lombok availability and supported annotations per module.
- [ ] Build rule set for safe boilerplate-to-Lombok migrations.
- [ ] Add semantic-preservation tests for migrated classes.

---

### 16. Flexible logging-level controls for CLI, wrappers, and public API (future version)

#### Status
- [ ] Not implemented (captured as a future product improvement)
- [ ] Current behavior:
  - CLI supports `-v/--verbose` (raises root logging to `DEBUG`)
  - startup override is possible via backend-specific Logback configuration replacement
  - there is no unified JHarmonizer-level logging settings surface shared across CLI, wrappers, and API

#### Problem statement
Logging verbosity control is currently split between CLI flags and backend-specific startup wiring.
Integrators need a first-class, documented product surface to control logging consistently for:
- CLI invocations in CI and local tooling,
- external wrappers (for example Maven/Gradle/IDE integrations),
- public `SrcProcessor` API embedding.

#### Proposed solution (next version)
Introduce a normalized logging settings model and expose it consistently across entry points:
- CLI:
  - `--log-level=<TRACE|DEBUG|INFO|WARN|ERROR>`
  - optional subsystem-scoped overrides (for example `--log-level.<subsystem>=<level>`)
- wrappers/plugins:
  - equivalent configuration keys mapped to the same normalized settings
- public API:
  - explicit logging options in bootstrap/config DTOs for processor creation

#### Expected benefits
- Consistent behavior regardless of launch path (CLI, wrapper, or direct API).
- Less dependence on backend-specific configuration files for common verbosity scenarios.
- Easier support/debug workflows with reproducible logging setup.

#### Non-goals
- Do not add runtime hot-reload/dynamic log reconfiguration in the first step.
- Do not hard-couple product-level config to one backend implementation detail.

#### Implementation outline (when revisited)
- [ ] Define backend-agnostic logging settings model in shared/core config.
- [ ] Add CLI options for global and subsystem-level logging overrides.
- [ ] Add wrapper-side mapping to the same normalized model.
- [ ] Add public `SrcProcessor` bootstrap/config API support.
- [ ] Implement precedence rules (defaults < file < wrapper/API/CLI override).
- [ ] Add tests for merging/precedence and deterministic output.
- [ ] Document end-user recipes for CLI and embedding scenarios.

---

### 17. Git-aware changed-files processing mode (future version)

#### Status
- [ ] Not implemented (captured as a future performance-focused feature)
- [ ] Current behavior: processing scopes are filesystem/path-driven, not VCS-diff-driven

#### Problem statement
For large repositories, reprocessing the full source tree on every run is expensive.
In common CI and pre-commit workflows we usually need to process only files that actually changed in Git,
for example relative to:
- working tree vs index,
- current branch vs merge-base with target branch,
- last successful push/tag/commit baseline.

#### Proposed solution (next version)
Introduce an optional Git-aware file discovery mode that limits sorting/checking/formatting to changed files only.

Initial capability set:
- detect changed files from configurable Git comparison baselines;
- filter candidates to supported source file types;
- pass only changed files into existing processing flows.

#### Open design research (required before implementation)
- Evaluate practices used by similar formatting/linting utilities for incremental processing.
- Compare baseline strategies and failure modes:
  - `HEAD` vs working tree,
  - merge-base against target branch,
  - “since last push” style heuristics.
- Define deterministic fallback behavior when Git metadata is unavailable (archive, shallow checkout, detached environments).

#### Safety requirements
- Never silently skip changed source files because of baseline ambiguity.
- Keep deterministic behavior between local runs and CI for the same baseline.
- Provide an explicit opt-out/full-scan mode for reproducibility and troubleshooting.

#### Implementation outline (when revisited)
- [ ] Add a VCS scope config/CLI option (`full-scan` vs `changed-only`).
- [ ] Implement baseline resolver (working tree, merge-base, explicit commit/tag).
- [ ] Add changed-file collector with path normalization and include/exclude support.
- [ ] Integrate collector output with current `SrcFilesHandler`/flow entry points.
- [ ] Add tests for:
  - [ ] local modified/untracked/staged file scenarios;
  - [ ] branch-diff scenario using merge-base;
  - [ ] no-Git metadata fallback behavior;
  - [ ] deterministic selection order and diagnostics output.

---

## Technical debt / stabilization backlog

### 1. Blank-final nearest-provider edge cases still not covered by active E2E

#### Status
- [ ] Not fully implemented in active E2E coverage

#### Remaining gap
- true multi-candidate-writer nearest-guaranteed-provider selection across multiple initialization members;
- valid Java cannot express this shape without violating final-assignment rules, so active compile/run E2E fixtures cannot model it directly.

#### Backlog direction
- keep this as a dedicated dependency-analysis improvement track;
- design a safe representation strategy for this family (without weakening valid-Java guarantees).

---

### 2. Spoon comment attachment gaps for non-type and comment-only units

#### Status
- [ ] Open investigation / upstream tracking not yet completed

#### Verified current behavior
- For some `package-info.java` and `module-info.java` sources, Spoon does not reliably expose leading file-scope
  opt-out comments through attached `CtComment` nodes.
- For comment-only sources, Spoon can produce `TYPE_DECLARATION` unit type with no declared types and no
  recoverable file-scope comments from AST traversal.

#### Temporary workaround in code
- `JHarmonizerOptOutResolver` keeps an AST-first strategy.
- For known unreliable unit shapes, file-level opt-out directives are parsed from raw source comments using
  a lexer-like fallback.

#### Follow-up actions
- [ ] Create/confirm upstream Spoon issue(s) with minimal reproducible samples for:
  - [ ] package declarations with leading file comments;
  - [ ] module declarations with leading file comments;
  - [ ] comment-only compilation units.
- [ ] Re-evaluate and remove/limit raw-source fallback after upstream fix is available and verified.

---

### 3. Spoon partial evaluator NPE on method-reference expressions in no-classpath mode

#### Status
- [ ] Open investigation / upstream issue not yet created

#### Verified current behavior
- In no-classpath parsing, Spoon may throw `NullPointerException` from partial evaluation while inspecting
  field-initializer expressions containing method references.
- Reproduced with expression shape similar to:
  `Map.of(WHOLE_CONFIG_KEY, WholeConfigDifferentiator::getByteBufferDifferentiator)`.

#### Representative stack trace (captured)
```text
java.lang.NullPointerException: Cannot invoke "spoon.reflect.reference.CtTypeReference.getTypeDeclaration()"
because the return value of "spoon.reflect.reference.CtFieldReference.getDeclaringType()" is null
    at spoon.support.reflect.eval.VisitorPartialEvaluator.visitFieldAccess(VisitorPartialEvaluator.java:428)
    at spoon.support.reflect.eval.VisitorPartialEvaluator.visitCtFieldRead(VisitorPartialEvaluator.java:397)
    at spoon.support.reflect.eval.VisitorPartialEvaluator.visitCtInvocation(VisitorPartialEvaluator.java:529)
```

#### Temporary workaround in code
- `DeclaringTypeFieldReferenceUtils.findPartiallyEvaluatedExpression(...)` treats partial-evaluation runtime
  failures as non-foldable expressions, logs warning context, and returns `Optional.empty()`.

#### Follow-up actions
- [ ] Create upstream Spoon issue with a minimal reproducible sample based on method-reference field initializer.
- [ ] Attach the captured stack trace and no-classpath context to the upstream report.
- [ ] Revisit local fallback scope after upstream fix becomes available.

---
