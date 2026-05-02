<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

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

### 18. Replace current printer implementation with a specialized source-preserving printer

#### Status
- [ ] Not implemented (captured as a future major refactor)
- [ ] Revisit after: first stable version is fully validated on real projects
- [ ] Priority context: output correctness + deterministic formatting + runtime performance

#### Background
Current printing logic evolved from overriding/extending an existing generic printer implementation.
Over time, most of the meaningful behavior became custom, while inherited base behavior still leaks into
edge-case formatting decisions.

At the same time, the project strategy is source-preserving where possible:
- reuse source fragments according to the harmonized model,
- avoid full model re-serialization whenever an original fragment can be reused safely.

This source-preserving direction does not align well with a generic printer architecture originally designed
for broad AST serialization use cases.

#### Problem statement
The current printer track has three structural issues:
1) **Architecture mismatch**: extension-overrides on top of a generic printer make behavior harder to reason about;
2) **Output quality risk**: inherited/default printer behavior may still produce undesirable formatting in edge cases;
3) **Inefficient algorithms**: repeated scans over the same member/component collections (for first/next/related element
   lookups) cause avoidable repeated work and poor scalability on larger classes.

#### Proposed solution
Build a **fully specialized JHarmonizer printer** focused only on this tool's needs.

Core principles:
- No dependency on generic “universal” printer behavior for ordering/render decisions.
- Source-preserving by default: compose output from indexed original source fragments guided by the harmonized model.
- Pre-index once, print many: construct all navigation/index structures in a single preparation phase, then render
  in linear (or near-linear) passes without repeated deep rescans.

#### Design direction (future version)

#### A. Two-phase printer pipeline
1) **Preparation/indexing phase**
- build a per-type immutable print context;
- index members, anchors, delimiters, trivia/comment blocks, and adjacency relationships;
- precompute “next printable element”/“first element in group” lookups.

2) **Rendering phase**
- emit output using prepared indices and precomputed traversal order;
- reuse source slices for unchanged fragments;
- apply targeted synthetic rendering only where source reuse is impossible or unsafe.

#### B. Data structures and complexity goals
- Replace repeated list scans with direct maps/index arrays for frequent lookups.
- Ensure each member/component is classified and linked once during preparation.
- Target complexity shift from repeated quasi-`O(n^2)` traversals toward `O(n)`/`O(n log n)` total passes depending on
  configured sorting.

#### C. Scope specialization
- Limit feature surface strictly to JHarmonizer scenarios (class member harmonization output).
- Do not attempt to expose this printer as a generic public formatting framework.

#### Expected benefits
- More predictable and controllable output behavior.
- Better alignment with source-preserving strategy (copy-by-model instead of broad re-serialization).
- Lower runtime overhead on large/complex classes due to single-pass indexing and reduced repeated searches.
- Simpler maintenance: one explicit printer architecture instead of layered override chains.

#### Non-goals
- Do not couple this refactor with unrelated parsing/rule DSL redesign.
- Do not broaden scope into a general-purpose Java formatter.
- Do not require byte-identical output migration in one step; allow staged parity checkpoints with explicit tests.

#### Implementation outline (when revisited)
- [ ] Document current printer responsibilities and inherited behavior still in effect.
- [ ] Define specialized printer contracts (input model, indexed context, rendering steps, invariants).
- [ ] Implement immutable indexing context built in one preprocessing pass.
- [ ] Replace repeated lookup loops with precomputed navigation tables.
- [ ] Implement source-fragment composition pipeline with controlled fallback rendering.
- [ ] Add focused performance benchmarks on representative large classes.
- [ ] Add regression tests for edge-case formatting and comment/trivia preservation.
- [ ] Roll out behind a feature flag, compare outputs, and remove old printer path after parity is proven.

---

### 19. Automatic fully-qualified name optimizer (future version)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: first stable version is fully validated on real projects

#### Background
Java source files sometimes contain fully-qualified type names used inline (for example
`java.util.List<String>` instead of a `List` reference backed by an `import java.util.List;`
declaration).
This is occasionally justified when two types from different packages share the same simple name
and both are referenced in the same file — in that case at most one of them can have a normal
import, and the other must stay fully-qualified to avoid an ambiguous name collision.

Outside of that justified case, inline fully-qualified names are noisy, harder to read, and
inconsistent with the project's code style expectations.

#### Problem statement
When fully-qualified names appear in a file with no naming collision, they should be replaced by:
1. a normal `import` statement for the referenced type, and
2. the short (simple) name at all usage sites.

The optimizer should perform this replacement automatically so that files converge toward the
canonical import-plus-short-name form.

#### Proposed solution (future)
Introduce an import normalizer pass that:
- detects all inline fully-qualified type references in the AST;
- for each reference, checks whether the simple name is already used by another imported type
  from a different package (collision check);
- if no collision exists, rewrites the reference to its simple name and inserts the corresponding
  import declaration into the file's import section;
- applies the same conflict-aware collision check in reverse when resolving ambiguities
  (for example two types named `Result` from different packages in the same file — one import is
  kept, the other stays fully-qualified);
- integrates with the existing import-section ordering rules so newly added imports land in the
  correct position.

#### Non-goals
- Do not attempt to resolve types that are not available on the configured classpath — this pass
  should operate only on types whose simple names are unambiguously resolvable in context.
- Do not change the behavior of intentionally fully-qualified references used in annotations,
  suppression strings, or other contexts where the FQN form is semantically required.

#### Implementation outline (when revisited)
- [ ] Enumerate all inline fully-qualified type references in the parsed AST.
- [ ] Build per-file simple-name collision map from existing imports and other FQN occurrences.
- [ ] For each FQN with no simple-name collision, replace all occurrences with the short name.
- [ ] Insert the corresponding import declaration via the import-section pipeline.
- [ ] Add dedicated E2E fixtures: one with a real collision (FQN must be preserved), one without
      (FQN must be replaced), and one mixed-case file.
- [ ] Expose a config option to disable this pass for projects that intentionally use FQNs.

### 20. Override-aware parameter ordering harmonizer for methods and constructors (future version)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: baseline member ordering and dependency passes are stable

#### Background
In inheritance-heavy code, overriding methods and constructor chains often evolve incrementally.
New parameters are added over time, and teams can end up with mixed parameter orders where
forwarded/base parameters are interleaved with extension-specific parameters.

That makes signatures harder to read and review: callers cannot quickly see which arguments
are part of the inherited contract and which ones are local extensions.

#### Problem statement
For overridden methods and constructor delegation chains, parameter order should follow
a deterministic convention:
1. parameters that map to the overridden or delegated/base signature come first, preserving base order;
2. parameters that extend behavior in the overriding method or delegating constructor declaration come after them.

When this convention is violated, JHarmonizer should be able to detect and automatically rewrite
the declaration and all in-scope call sites affected by the reordering.

#### Proposed solution (future)
Introduce an override-aware parameter ordering pass that:
- resolves method override relations (including interface inheritance) and constructor delegation
  chains (`this(...)` / `super(...)`) inside the compilation unit scope;
- builds a base-parameter mapping for each candidate method/constructor;
- verifies whether declaration parameter order is `[base-mapped..., extension...]`;
- when not compliant, rewrites the declaration signature to the canonical order;
- rewrites corresponding invocation argument order for call sites that can be updated safely
  in the same harmonization run;
- reports conservative warnings (without auto-fix) when safe full rewrite is not guaranteed
  because of unresolved external call sites or ambiguous overload impacts.

#### Non-goals
- Do not attempt whole-repository/global refactors in the first iteration when symbol resolution
  is incomplete; prefer safe local/project-scoped fixes first.
- Do not reorder parameters for APIs explicitly annotated/configured as order-frozen compatibility
  surfaces.

#### Implementation outline (when revisited)
- [ ] Define matching strategy for base-parameter mapping (name + type + position fallback).
- [ ] Implement constructor-chain analyzer for delegated parameter flow (`this`/`super`).
- [ ] Implement override signature analyzer for inherited method contracts.
- [ ] Add canonical reorder transformer for declaration signatures.
- [ ] Add call-site argument reorder transformer with safety gating.
- [ ] Add dry-run diagnostics mode (`report-only`) before enabling auto-fix by default.
- [ ] Add focused E2E fixtures for: clean reorder, ambiguous overload, and external-callsite-only cases.

### 21. Redundant Java modifier cleanup pass (future version)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: core rewrite passes are stable and formatter interactions are verified

#### Background
Java has multiple contexts where some modifiers are implied by language rules and therefore
add no semantic value when written explicitly.

Common examples of truly implied modifiers:
- `public` on interface members (methods, constants, nested types), where interface member visibility is implicitly public;
- `public abstract` on interface methods (both are implicit unless using `default`/`static`/`private`);
- `public static final` on interface fields (all interface fields are implicitly constants);
- `static` on nested interfaces declared inside interfaces (implicitly static).

Such modifiers add noise and make signatures longer without improving readability.

Note: `final` on methods in `final` classes is **not** a truly implied modifier under Java language
rules — removing it can affect generated bytecode and reflection-visible modifier flags.
Any cleanup of non-implied modifiers such as this must be treated as a separate, opt-in
policy/behavior-change pass rather than part of the core implied-modifier removal described here.

#### Problem statement
When Java modifiers are redundant in context and removing them does not change meaning,
JHarmonizer should automatically remove them to keep declarations concise and idiomatic.

#### Proposed solution (future)
Introduce a modifier-normalization pass that:
- classifies declarations by context (top-level type, nested type, interface member, class member, etc.);
- computes the set of semantically required modifiers versus redundant ones per declaration kind;
- removes only modifiers proven to be redundant under Java language rules for the target source level;
- skips contexts where modifier removal could alter API compatibility signals or team policy intent
  unless explicitly enabled by configuration;
- emits diagnostics in report-only mode to let teams preview removals before auto-fix rollout.

#### Non-goals
- Do not remove modifiers that are technically redundant but intentionally retained for external style
  policy unless configuration opts into that strict cleanup.
- Do not rewrite declarations when parser/symbol info is insufficient to prove semantic equivalence.

#### Implementation outline (when revisited)
- [ ] Build a declaration-context matrix of Java-implied modifiers by language level.
- [ ] Implement a safe redundant-modifier detector for each declaration category.
- [ ] Add configuration flags (report-only / auto-fix / allow-style-opinionated removals).
- [ ] Add E2E fixtures for interface methods/fields, nested interfaces, and final-class method cases.
- [ ] Add regression coverage to ensure no behavior/API changes from modifier cleanup.

---

### 22. Automatic constant naming convention enforcer (`UPPER_SNAKE_CASE`) (future version)

#### Status
- [ ] Not implemented (explicitly deferred)

#### Problem statement
Codebases frequently contain constants whose names do not follow the standard Java naming convention
(`UPPER_SNAKE_CASE`). Manually renaming them is tedious and error-prone, especially when a constant
is referenced from multiple classes across the project.

#### Proposed solution (phased)

**Phase 1 — private constants (low risk, actionable now)**

Private constants are only referenced within the same top-level declaration, including nested / inner
classes, anonymous classes, and lambdas that can legally access the member, so JHarmonizer can rename
them safely using source-local AST rewriting:
- detect `private static final` fields whose current name does not match `UPPER_SNAKE_CASE`;
- derive the canonical `UPPER_SNAKE_CASE` name via a configurable naming strategy
  (camelCase → UPPER_SNAKE_CASE conversion, collision resolution);
- rewrite the field declaration and all references within that declaration scope (initializers, method
  bodies, annotations, nested types, and lambda bodies) in a single atomic pass.

**Phase 2 — package-private constants (medium risk)**

Requires cross-file analysis limited to the same compilation unit / package boundary:
- collect all same-package call sites before rewriting;
- apply a consistent rename across all affected files in a single pass;
- skip when any call site is ambiguous or unresolvable in no-classpath mode.

**Phase 3 — protected and public constants (high risk, requires full project compilation)**

Full cross-module rename requires classpath-aware symbol resolution (see the "Inter-procedural
initializer dependencies" entry and future full-project compilation ideas):
- `protected` members may be referenced from subclasses in other packages, so same-package-only
  analysis is insufficient;
- requires complete symbol information for all transitive call sites;
- should be offered only in an explicit opt-in mode with a report-only preview;
- deferred until JHarmonizer supports full project-wide AST analysis.

#### Safety requirements
- treat rename as a no-op when the derived `UPPER_SNAKE_CASE` name already collides with another
  field in the same type;
- never rewrite a constant whose name is referenced via reflection or used as a string literal
  (including annotation string elements and configuration string literals); add heuristic guards
  for these cases;
- always produce a rename report listing every changed identifier and its call sites;
- support report-only mode so teams can preview changes before committing;
- keep each phase independently toggleable in configuration.

#### Implementation outline (when revisited)
- [ ] Implement `camelCase`/`PascalCase` → `UPPER_SNAKE_CASE` name derivation utility (shared, neutral package).
- [ ] Add collision detection within the same type for the derived name.
- [ ] Implement Phase 1 rewriter: private constant + in-class reference rename.
- [ ] Add configuration flags: `rename-private-constants`, `rename-package-constants`, `rename-protected-constants`, `rename-public-constants`, `report-only`.
- [ ] Add E2E fixtures for simple rename, collision avoidance, and multi-reference scenarios.
- [ ] Extend to Phase 2 once cross-file local analysis is proven stable.
- [ ] Extend to Phase 3 after full project-wide AST/classpath analysis is available.

---

### 23. JUnit 5 test visibility normalizer (future version)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: core rewrite passes are stable and the modifier-normalization infrastructure from item 21 is in place

#### Background
JUnit Jupiter discovers test and lifecycle methods through reflection and does not require `public`
visibility. Keeping `public` on test classes and annotated methods is purely cosmetic — it adds noise,
widens apparent API surface, and conflicts with the general minimal-access-level principle.

The intended convention is:
- JUnit 5 test classes → package-private by default (no modifier).
- Methods annotated with `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate` → package-private.
- Lifecycle methods annotated with `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll` → package-private.
- `@Nested` test classes → package-private.
- Helper methods, constants, and helper nested classes used only inside one test class → remain `private`.
- Shared test utilities reused across test classes in the same package → may remain package-private.
- `public` removed from test classes and annotated methods unless a documented technical reason requires it.
- `private` must never be applied to JUnit test or lifecycle methods; they must remain discoverable by JUnit Jupiter.

#### Problem statement
Many existing test classes and annotated methods carry redundant `public` modifiers.
JHarmonizer should automatically normalize these in test sources to reduce noise and enforce
the minimal-access-level convention without changing test semantics.

#### Proposed solution (future)
Introduce a dedicated test-source visibility cleanup rule that:
- applies only to `src/test/java` sources, never to production code;
- detects JUnit Jupiter annotations: `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`,
  `@TestTemplate`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, `@Nested`;
- removes redundant `public` from top-level test classes and annotated test/lifecycle methods;
- removes redundant `public` from `@Nested` inner test classes;
- does not touch `private` helpers or non-JUnit entry points unless a separate helper-visibility rule is introduced;
- never makes test or lifecycle methods `private`;
- is disabled by default until the behavior is fully validated;
- is enabled only for test sources via an explicit configuration flag.

#### Scope and design options (to decide when implementing)
- Part of the JHarmonizer restructuring pipeline as a test-only rewrite pass.
- A dedicated test-source cleanup rule group, independently toggleable.
- Or a separate optional rule that can be composed with the main pipeline.

#### Implementation outline (when revisited)
- [ ] Determine whether this fits best as a modifier-normalization sub-pass of item 21 restricted to test sources, or as a standalone rule.
- [ ] Implement annotation detector for all JUnit Jupiter test and lifecycle annotations.
- [ ] Implement visibility normalizer that removes `public` from supported declarations in test sources.
- [ ] Add configuration flag: disabled by default; opt-in via explicit test-source-visibility cleanup flag.
- [ ] Add E2E fixtures for:
  - `public` test class converted to package-private;
  - `public` `@Test` method converted to package-private;
  - `public` lifecycle method (`@BeforeEach`, `@AfterEach`, etc.) converted to package-private;
  - `public` `@Nested` class converted to package-private;
  - `private` helper method left unchanged;
  - `public` helper method without JUnit annotation not changed (unless a separate helper-visibility rule is introduced);
  - production class not changed;
  - invalid case not produced — JUnit entry points must not be made `private`.

---

### 24. Vendor-format configuration adapters (IntelliJ IDEA, Eclipse, EditorConfig, Spotless)

#### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: configurator + compiled-config layering is fully stable and at least one
  external integration explicitly requests it

#### Background
JHarmonizer's configurator already separates four layers (Vendor → Unified → Merge → Compiled);
see [`docs/02-Configurator.md`](02-Configurator.md). The Vendor layer is the only layer that is
format-specific; everything downstream — overlay merging, compilation, sorter / formatter
consumption — already operates on the vendor-independent `UnifiedConfig`.

Today the only Vendor implementation is the JHarmonizer YAML schema. The architecture is
deliberately set up so that other vendor configuration formats (formats that influence
**formatting and member ordering** in real Java projects) can be ingested by writing only an
adapter, without touching merge / compile / runtime code.

#### Problem statement
Real-world Java projects often already carry vendor-specific style/order configuration that
lives outside of JHarmonizer:

- **IntelliJ IDEA** — `.idea/codeStyleConfig.xml`, `.idea/codeStyles/Project.xml`, the
  "Arrangement" rules, naming-convention rules and `@Order` annotations from the IDE.
- **Eclipse / Eclipse JDT** — `org.eclipse.jdt.core.prefs`, exported Eclipse formatter
  profiles (`*.xml`) and clean-up profiles.
- **EditorConfig** — `.editorconfig` properties that affect indentation, line endings, and a
  small subset of formatter-relevant settings.
- **Spotless** — `spotless` Maven/Gradle plugin configurations that pin a formatter style.
- Possibly others (Checkstyle/PMD configurations that constrain ordering, etc.).

Forcing teams to maintain a separate `jharmonizer.yml` that duplicates these settings is a
real adoption friction. The goal is to ingest the parts of these formats that map onto
`UnifiedConfig` so JHarmonizer can act as a single source of truth without rewriting the
project's existing IDE/CI configuration.

#### Proposed solution (future)
For each supported vendor format, add a thin adapter pair:

- a vendor-specific loader / deserializer (`<Vendor>ConfigLoader`) that turns the input file
  into a strongly-typed vendor model (e.g. `IdeaCodeStyleConfig`, `EclipseFormatterProfile`,
  `EditorConfigDocument`, `SpotlessConfigSnapshot`);
- a vendor-specific converter (`<Vendor>2UnifiedConverter`) that maps the vendor model onto
  `UnifiedConfig` / `FlexibleUnifiedConfig`.

Once a vendor adapter produces a `UnifiedConfig` (or `FlexibleUnifiedConfig` overlay), the
rest of the pipeline — `UnifiedConfigMerger`, `Unified2CompiledModelCompiler`, `SrcProcessor`
— consumes it unchanged.

Discovery and ordering of overlays (which vendor file is loaded first, how they merge with
the JHarmonizer YAML overlay, which one wins on a conflict) is part of the design that needs
to be revisited together with the actual ingestion code. Some properties of those formats
have no JHarmonizer equivalent and should be silently ignored; some have lossy or
opinionated mappings that need to be documented per adapter.

#### Non-goals
- Round-tripping vendor configurations: JHarmonizer ingests these formats one way; we do
  **not** plan to write back into `.idea/*.xml`, Eclipse profiles, or `.editorconfig`.
- Ingesting formats whose only relevance is unrelated to formatting / member ordering
  (build configuration, linter rule sets, dependency tools).

#### Implementation outline (when revisited)
- [ ] Inventory which fields each target vendor format actually exposes that map onto
  `UnifiedConfig` (formatter style, blank-line policy, member ordering, header lines, ...).
- [ ] Decide a stable conflict-resolution order between vendor overlays and the JHarmonizer
  YAML overlay.
- [ ] Implement adapters one vendor at a time, starting with the lowest-risk format.
  Suggested order: `.editorconfig` → IntelliJ IDEA → Eclipse → Spotless.
- [ ] Add a "lossy mapping" doc per adapter that lists which vendor settings are honoured,
  approximated, or ignored.
- [ ] Add E2E fixtures for each adapter under `core/src/test/resources/test-cases/**`.

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

### 3. Spoon partial evaluator runtime failures in no-classpath mode (`NullPointerException` + class-loading edge case)

#### Status
- [ ] Open investigation / upstream issue not yet created

#### Verified current behavior
- In no-classpath parsing, Spoon may throw `NullPointerException` from partial evaluation while inspecting
  field-initializer expressions containing method references.
- Reproduced with expression shape similar to:
  `Map.of(WHOLE_CONFIG_KEY, WholeConfigDifferentiator::getByteBufferDifferentiator)`.
- In self-type class-literal initializer shapes, Spoon partial evaluation may throw
  `spoon.support.SpoonClassNotFoundException` while attempting classpath lookup for the currently processed source type.
- Reproduced by fixture:
  `core/src/test/resources/test-cases/core/e2e/regression/01-self-type-class-literal-logger-factory`.

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
- [ ] Create upstream Spoon issue (or linked follow-up) for self-type class-literal partial-evaluation class-loading failure.
- [ ] Attach the captured stack trace and no-classpath context to the upstream report.
- [ ] Revisit local fallback scope after upstream fix becomes available.

---
### 4. Spoon incorrect `sourceStart` for first explicit enum member after constant/lambda region

#### Status
- [ ] Open investigation / upstream issue not yet created
- [ ] Local workaround implemented and covered by dedicated regression fixture

#### Verified current behavior
- For a specific enum shape, Spoon may report incorrect `sourceStart` for the **first explicit member** after
  enum constants (for example a method declared right after constant declarations).
- Observed in regression fixture:
  `core/src/test/resources/test-cases/core/e2e/regression/02-enum-lambda-body-member-boundary`.
- In this case, the reported start can be shifted into the preceding enum constant/lambda body zone, which breaks
  member boundary slicing for source-preserving printing.

#### Temporary workaround in code
- `EnumMemberStartCorrectionResolver` applies targeted correction for enum member starts.
- Correction flow:
  - finds the earliest explicit enum member;
  - searches declaration-prefix pattern inside the extracted source fragment;
  - shifts start offset when Spoon start points before the real declaration.

#### Follow-up actions
- [ ] Create upstream Spoon issue with a minimal enum reproducer from regression `02-enum-lambda-body-member-boundary`.
- [ ] Attach before/after `sourceStart` observations for affected members and expected boundary behavior.
- [ ] Re-evaluate and simplify/remove local correction once Spoon fix is released and validated.

---

### 5. Spoon partial evaluator `StackOverflowError` on anonymous-class chains during expression evaluation

#### Status
- [ ] Open investigation / upstream issue not yet created
- [ ] Local guard implemented and covered by dedicated regression fixture

#### Verified current behavior
- Spoon partial evaluation may recurse into anonymous-class initializer structures and crash with
  `StackOverflowError`.
- Reproduced by fixture:
  `core/src/test/resources/test-cases/core/e2e/regression/03-string-selector-anonymous-chain-overflow`.
- Repeating stack includes `VisitorPartialEvaluator` with `CtNewClassImpl.accept(...)` recursion patterns.

#### Temporary workaround in code
- `DeclaringTypeFieldReferenceUtils.findPartiallyEvaluatedExpression(...)` skips partial evaluation for expressions
  containing `CtNewClass` nodes.
- This avoids triggering recursion trap and preserves pipeline stability by falling back to non-partially-evaluated
  analysis path.

#### Follow-up actions
- [ ] Create upstream Spoon issue with minimal reproducer from regression `03-string-selector-anonymous-chain-overflow`.
- [ ] Attach captured stack trace and note no-classpath/e2e context where recursion occurs.
- [ ] Revisit local `CtNewClass` guard scope after upstream fix is available.

---


### 6. Open upstream Palantir formatter issue for non-deterministic trailing comment/code reflow

#### Status
- [ ] Not filed yet
- [ ] Collect minimal reproducer from the observed two-pass formatting diffs
- [ ] File upstream issue in Palantir Java Formatter tracker

#### Why this exists
In specific wrapped-expression scenarios with long trailing `//` comments, one formatting run and the next
formatting run can produce different output:
- wrapped comment continuation line indentation can change;
- adjacent wrapped code layout can also change between passes.

This is an upstream formatter idempotency issue, not a JHarmonizer sorting/rendering issue, but we should track the
upstream ticket and link it from our documentation once created.

#### Follow-up actions
- [ ] Create minimal Java snippets that reproduce both comment-reflow and code-reflow variants.
- [ ] Open issue in `palantir/palantir-java-format` with exact formatter version and reproducers.
- [ ] Add the upstream issue link to `docs/05-Formatter.md` and `README.md` after filing.

---

### 7. Spoon comment misattribution after member reorder (non-idempotent blank line)

#### Status
- [ ] Open investigation / upstream issue not yet created
- [ ] Local workaround implemented and covered by regression fixture `10-non-idempotent-blank-line-in-field-group-after-sort`

#### Verified current behavior
- When a type contains a member with a trailing inline `// comment` and members are reordered,
  Spoon misattributes the trailing comment to the next member in the **original** source order.
- The misattributed comment has `endLine < member.getLine()` (it ends before the member it is attached to),
  so it looks like a genuine leading comment of that member.
- With `blank-line-before-comment: true`, this causes a spurious blank line to be inserted before
  the first member after reorder (manifesting as a blank line directly after the type opening brace `{`).
- On the second pass, members are already in sorted order, so misattribution no longer occurs —
  making the output non-idempotent.
- Reproduced by regression fixture `10-non-idempotent-blank-line-in-field-group-after-sort`
  with a minimal 3-field 1-import class.

#### Representative reproducer (minimal)
```java
import java.util.concurrent.ExecutorService;

public abstract class TestListener {
    private volatile ExecutorService executor; // keep volatile
    private volatile Object conn;              // keep volatile
    private final Object config;
}
```

After one JHarmonizer pass on original source, Spoon attributes `// keep volatile` from `conn`
to `config`, triggering a spurious blank line before `config` (the first member post-sort).

#### Temporary workaround in code
- `SpoonTypeMemberUtils.hasLeadingCommentOnSeparateLine(CtTypeMember, Set<Integer>)` filters out
  comments whose start line coincides with the last source line of any other member declaration.
  Trailing inline `// comments` always appear on the last source line of their original member,
  so this filter excludes misattributed ones while keeping genuine leading comments.

#### Follow-up actions
- [ ] Create upstream Spoon issue with the minimal 3-field reproducer from
  `core/src/test/resources/test-cases/core/e2e/regression/10-non-idempotent-blank-line-in-field-group-after-sort`.
- [ ] Document: comment attachment rule that causes the misattribution, Spoon version observed, and
  no-classpath parsing context.
- [ ] Link upstream issue from `README.md` of regression test 10 once filed.
- [ ] Re-evaluate and simplify/remove local workaround after upstream fix is available and verified.

---

### Spoon javadoc misattribution across nested-type boundary (non-idempotent blank line after `{`)

#### Status
- [ ] Open investigation / upstream issue not yet created
- [ ] Local workaround implemented and covered by regression fixture `11-non-idempotent-blank-line-after-nested-interface-sort`

#### Verified current behavior
- When a nested type (e.g. `interface Builder`) has its own javadoc comment and is printed with no
  blank line between its opening `{` and its first inner member, Spoon misattributes that javadoc
  to the first inner member instead of to the enclosing nested type.
- The misattributed javadoc has `endLine < member.getLine()` (it ends before the inner member it is
  attached to), so it looks like a genuine leading comment of that inner member.
- With `blank-line-before-comment: true`, this causes a spurious blank line to be inserted before
  the first inner member on the **second** pass (the first pass produces the no-blank-line layout,
  which then triggers the misattribution on re-parse).
- Reproduced by regression fixture `11-non-idempotent-blank-line-after-nested-interface-sort`
  with a minimal `RegistryService` / `Builder` interface pair.

#### Representative reproducer (minimal)
```java
public interface RegistryService {
    /**
     * The builder.   ← javadoc of Builder; misattributed to build() when no blank line after {
     */
    interface Builder {
        RegistryService build();   ← first inner member; receives spurious blank line on 2nd pass
    }
}
```

After the first JHarmonizer pass, `interface Builder {` is immediately followed by `RegistryService build();`
(no blank line). On the second parse, Spoon crosses the type body boundary and attributes
`/** The builder. */` to `build()`. With `blank-line-before-comment: true`, a blank line is then
inserted before `build()`, which manifests as a blank line directly after `interface Builder {`.

#### Temporary workaround in code
- `SpoonTypeMemberUtils.hasLeadingCommentOnSeparateLine(CtTypeMember, Set<Integer>, int typeDeclarationStartLine)`
  filters out comments whose start line is strictly before the enclosing type's first declaration
  line (`typeDeclarationStartLine`). Such comments originate outside the type body and cannot be
  genuine leading comments of any inner member.

#### Follow-up actions
- [ ] Create upstream Spoon issue with the minimal `RegistryService` / `Builder` reproducer from
  `core/src/test/resources/test-cases/core/e2e/regression/11-non-idempotent-blank-line-after-nested-interface-sort`.
- [ ] Document: the exact condition (no blank line after `{` + enclosing javadoc) that triggers the
  cross-boundary misattribution, the Spoon version observed, and the no-classpath parsing context.
- [ ] Link the upstream Spoon issue from both `README.md` of regression test 11 and this entry once filed.
- [ ] Link this issue to the related upstream bug in the "Spoon comment misattribution after member reorder"
  entry above — both share the same root cause: Spoon's comment scanner crossing AST element boundaries.
- [ ] Re-evaluate and simplify/remove local workaround after upstream fix is available and verified.

---

### 8. File upstream Spoon issue: broken `equals` / `hashCode` contract on `CtTypeMember`

#### Status
- [ ] Open — upstream issue not yet filed
- [ ] See workaround already applied: `RelocationDetector.buildOriginalIndexMap` uses `IdentityHashMap`

#### Problem
Two distinct `CtTypeMember` objects that live in *different* positions in the type hierarchy
(e.g., `void alpha()` in the outer class and `void alpha()` in a nested class) report the same
`hashCode()` and are considered equal by `equals()`.
`CtElementImpl` delegates to `EqualsVisitor`, which performs a purely structural comparison without
considering the owning type or source position.
As a result, any `HashMap<CtTypeMember, …>` keyed on such members silently overwrites the earlier
entry with the later one, producing wrong index lookups.

This caused `RelocationDetector.findRelocations` to return false-positive relocations for files
containing nested types with same-named members, breaking the idempotency check in
`AbstractSrcProcessorScenarioE2ETest.assertFileProcessingIsDeterministic`.

#### Current workaround in JHarmonizer
`RelocationDetector.buildOriginalIndexMap` uses `IdentityHashMap<CtTypeMember, Integer>` instead of
`HashMap` / `LinkedHashMap`. Identity-based maps compare keys by reference (`==`), bypassing the
broken `equals`/`hashCode`, which is correct semantics for live Spoon AST nodes.

#### Follow-up actions
- [ ] Create upstream Spoon issue at https://github.com/INRIA/spoon/issues with a minimal reproducer:
  two nested classes each declaring a method with the same signature — verify that `equals()` returns
  `true` and `hashCode()` collides for the two distinct `CtTypeMember` instances.
- [ ] Attach the root-cause note: `CtElementImpl` → `EqualsVisitor` performs structural-only comparison.
- [ ] Once the upstream fix is released and validated, remove the `IdentityHashMap` workaround in
  `RelocationDetector.buildOriginalIndexMap` and update the comment there.
- [ ] Remove or simplify the "Known upstream issues — Spoon `CtTypeMember`" note from `AGENTS.md` /
  `.github/copilot-instructions.md` once the fix lands.

---

### 9. Review and optimize sorting/comparator/accessor-cluster algorithms and architecture

#### Status
- [ ] Open architectural review item (captured for follow-up cleanup pass)
- [ ] Revisit after: current accessor super-cluster ordering fix lands and stabilizes on `dev`

#### Background
The Spoon-based sorter currently composes ordering through several cooperating pieces:
- `OrderingKey` (member-own ordering values: `srcStart`, `alphaKey`, `alphaSortingRank`, `visibilityRank`).
- `OrderingKeyFactory` (derives own keys, builds accessor super-cluster and per-property representative keys).
- `SortableTypeMember` (carries `ownKey`, `propertyClusterRepresentativeKey`, `superClusterRepresentativeKey`,
  with shared instances for cluster members and self-references for non-clustered members).
- `ComparatorUtils` (precomputed comparator constants; `buildSortableTypeMemberComparator` dispatches by
  representative reference identity; tie-breakers chained via `appendTieBreakers`).
- `GroupMembersOrderer` (decides when to form an accessor super-cluster, using
  `OrderingKeyFactory.MIN_ACCESSORS_FOR_SUPER_CLUSTER`).

This already fixes the original ALPHA non-transitivity bug, but the design grew incrementally and is worth a
dedicated review pass.

#### Review goals
- [ ] Re-examine the accessor super-cluster + property-cluster representative-key approach end-to-end and confirm
  it is the simplest model that preserves transitivity for all comparator combinations
  (PRESERVE / ALPHA / VISIBILITY_ASC / VISIBILITY_DESC / SIGNATURE).
- [ ] Re-check the split between `OrderingKey`, `SortableTypeMember`, `OrderingKeyFactory`, and `ComparatorUtils`.
  Look for opportunities to collapse responsibilities, remove indirections, or align with the descriptor-first
  direction described in "Planned future features → 1. Compile group sorting once".
- [ ] Reconsider whether reference-identity dispatch on representative keys is the right primitive long-term, or
  whether an explicit cluster-id field on the descriptor would be clearer and equally correct.
- [ ] Audit `appendTieBreakers` and the empty-`orderingRules` short-circuit to make sure no comparator chain
  duplicates work on hot paths.
- [ ] Centralize all clustering thresholds (currently `MIN_ACCESSORS_FOR_SUPER_CLUSTER`) and any future tuning
  knobs in one place to prevent silent divergence.
- [ ] Look for redundant per-group comparator construction and per-member key recomputation (overlap with the
  planned descriptor-first refactor).

#### Follow-up actions
- [ ] Schedule an architectural review of `core/src/main/java/io/github/lemon_ant/jharmonizer/core/sorter/spoon/`
  after the current PR stack lands.
- [ ] Capture concrete refactor tasks from the review as separate sub-items here (or promote them into
  "Planned future features → 1. Compile group sorting once" if they fit that track).
- [ ] Add micro-benchmarks for large groups (many accessors + many non-accessors) before/after any algorithmic
  change to guard against regressions.

---
