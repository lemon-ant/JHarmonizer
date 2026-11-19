<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->
# AGENTS.md

This file defines repository-wide conventions for coding agents working in this repository.

## Scope

- Applies to the whole repository unless a more specific convention document says otherwise.

## Convention maintenance

- During every task, look for stable conventions that become clear from review feedback or repeated user guidance.
- Keep `AGENTS.md` and `.github/copilot-instructions.md` aligned.
  - `AGENTS.md` defines the repository-wide rules.
  - `.github/copilot-instructions.md` must contain the complete operative rule set from `AGENTS.md` so Copilot can
    follow it without relying on cross-file traversal.
- If a rule is missing, unclear, or no longer accurate in the current docs, update all affected instruction files in the
  same task.
- If a documented rule is ambiguous, clarify the document rather than relying on unwritten expectations for future
  sessions.
- Review comments and user requests may be mistaken; for disputed framework/plugin/tool behavior, verify against
  official documentation before changing code.
- If a requested change conflicts with official documentation or established framework/plugin behavior, do not apply it
  blindly.
  - Explain the conflict clearly in review feedback.
  - Provide the documentation-aligned alternative and prefer that variant.

## General code conventions

- Prefer the smallest complete change that solves the reviewed problem.
- Keep changes surgical and avoid unrelated cleanup.
- JHarmonizer is consumed as one application; internal module APIs may change freely. Update affected callers in the
  same change instead of adding or retaining overloads or forwarding methods solely for backward compatibility.
  Preserve contracts exposed to application users unless changing them is part of the requested task.
- For mandatory processing steps with one implementation, keep implementation choice inside the owning processor and
  accept the required input and context. Do not expose strategy callbacks solely for test substitutions or hypothetical
  alternatives.
  Keep stateful mandatory helpers as concrete processing objects instead of closures stored in generic functional
  interface fields.
- Use concise, factual technical language in documentation, comments, changelog entries, and commit messages. Omit
  conversational narration, decorative wording, repetition, and unnecessary detail.
- Prefer "source file" over "compilation unit" in high-level descriptions. Retain Spoon terminology when describing
  specific APIs or model identity; distinguish the source file from its parsed Spoon model.
- When creating or substantially revising an algorithm guide, put its main flow diagram first and explain the protected
  behavior with observable examples before implementation details. Distinguish sorting criteria from dependency
  constraints when both are involved.
- Name algorithm stages after their complete operation; describe intermediate selection steps in the details.
  Overview diagrams may use concise scope labels when the accompanying text explains the complete operation.
  Define name components in algorithm guides and distinguish lookup outcomes from returned values. Explain framework
  limitations with concrete parsed-source examples; identify artificial test conditions separately from observed
  parser behavior.
  State the returned name form explicitly: distinguish simple, written, fully qualified, and canonical type names.
  Algorithm examples must exercise the control-flow transitions they describe; account for early returns when
  explaining traversal between scopes or stages.
  In annotation lookup descriptions, distinguish enclosing types, the initial lookup type, and the current lookup type.
  Reserve "scope" for actual Java name visibility instead of using it as a synonym for these types.
- Describe Spoon source positions as ranges or offsets reported by Spoon. Distinguish reported positions from actual
  source boundaries and source-text rewrites.
- Source-derived metadata must be read from compilable Java and Spoon's unmodified parsed AST.
  Throw an exception when an observed source/model shape violates that contract and fail processing for that file.
  Do not recover through defaults or alternate AST representations for malformed input or caller-modified models.
  Unavailable external declarations in no-classpath mode do not by themselves violate this contract.
- Wrap Markdown prose at 120 characters without changing wording or document structure.
  - Preserve tables, code blocks, diagrams, stack traces, and indivisible links when wrapping would change their content
    or Markdown interpretation.
- Create Git commits only when the user explicitly requests a commit for the current task. Authorization from a
  completed task does not carry over to a new task; otherwise leave changes uncommitted.
- Record user-visible features, fixes, compatibility changes, and significant performance changes in `CHANGELOG.md`
  in the same task. Write for users and developers integrating JHarmonizer into builds. Include dependency updates
  affecting compatibility, source processing, or security. Group related changes and omit implementation refactors,
  internal API changes, test additions, and quality-gate details.
- Licensing policy is mandatory for all tracked files in this repository.
  - Every tracked text/source/config/documentation file must include SPDX metadata.
  - Required file-level SPDX lines:
    - `SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>`
    - `SPDX-License-Identifier: Apache-2.0`
  - Allowed omission: repository license body file (`LICENSE`) may keep only the canonical Apache-2.0 legal text without
    SPDX header.
  - Do not introduce additional third-party license headers unless a file is truly imported from a differently licensed
    upstream source.
- Avoid cosmetic-only churn in production files (for example adding/removing separator blank lines) when there is no
  behavioral or readability gain tied to the task.
- Reuse existing project and library utilities before introducing custom helpers.
- Keep generic raw source-offset and whitespace operations in `SrcCodeUtils`. Place Java lexical helpers according to
  their actual consumers; independence from Spoon does not require a global utilities package. Retain token-cursor
  state and annotation-specific parsing in the scanner.
- Use half-open `[start, endExclusive)` ranges in `SrcCodeUtils`; convert Spoon's inclusive end offsets at integration
  boundaries.
- Keep parser-stage type-tree traversal, source type nesting, and enclosing-type selection in
  `translator.spoon.SpoonTypeTreeUtils`. Keep annotation-only type-name formatting, prefix matching at segment
  boundaries, and annotation-kind validation in the package-private `translator.spoon.annotation.SpoonTypeNameUtils`.
  Keep only cross-stage main-type selection, effective member boundaries, and explicit source-member traversal in
  `core.spoon.SpoonTypeUtils`.
  Format nesting through recorded type-reference owner links, independently of declaration availability.
  Preserve literal `$` characters in Java identifiers instead of replacing every `$` with a dot.
  Keep inherited member-type lookup and its result model in `SpoonTypeInheritanceUtils`.
  Keep complete annotation type-name resolution, stage precedence, single imports, and final fallback-name selection
  in `AnnotationTypeNameResolver`. Delegate lookup through nested and inherited declarations and source AST-binding
  confirmation (stages 1, 2, and 4) to `AnnotationTypeNameFromDeclarationsResolver`.
  Share `CanonicalAnnotationTypeNameLookupResult` as an independent result model.
  Keep repeatable-container metadata and order-preservation key resolution in `AnnotationDependencyKeyResolver`.
  Distinguish type nesting from inheritance in helper names and documentation.
- Keep Spoon import-name extraction, import-shape checks, and inherited member-type lookup within
  `translator.spoon.annotation`, in `SpoonImportUtils` and `SpoonTypeInheritanceUtils` respectively.
  Type-name extractors must exclude resolved field/method references and unresolved static members. Preserve their
  simple names as lookup blockers: a field/method reference may omit a same-name inherited type imported by that
  declaration. A definite type import takes precedence; other matching static-member names are lookup uncertainty,
  not canonical type-name evidence.
  Prepare scope-independent import names, classifications, and wildcard owners once per source file in a
  `SpoonTypeImportContext` shared by these annotation processors. Retain all real wildcard imports in model order
  and index all available source owners by the original import's identity. Check imports in that order and stop when
  an owner is unavailable; keep per-name owner searches and selected annotation-kind validation in the lookup stages.
  Pass the prepared import context to consumers instead of unpacking its collections at call sites.
  Consumers that need only part of it must extract and retain the required fields in their constructors.
  Retain a computed predicate when a consumer needs that fact but not the underlying collection.
  Derive import-presence predicates from existing collections instead of storing duplicate flags. After matching
  single imports have been handled, unrelated single imports do not block the module's implicit `java.lang` fallback.
  Reject wildcard imports before contextual lookup, including uses with no AST reference.
  Do not assume Spoon's import iteration order matches source-text order; tests must use the original parsed model.
- In annotation lookup/index names, distinguish annotation type declarations (`@interface Tag`) from uses (`@Tag`).
  When an index stores `CtTypeReference` values for uses, name the stored references explicitly.
  Name general Java type-lookup helpers and results without an annotation-specific prefix. Include `Annotation` in
  helper names when their contract is specific to annotation uses.
  Have `CanonicalAnnotationTypeNameLookupResult` source constructors accept `CtType<?>`, require
  `CtAnnotationType<?>` internally, and derive names from the validated declarations. Keep import-based construction
  separate, validate available declarations, and avoid raw-name constructors that bypass these lookup checks.
  Keep `AnnotationTypeNameResolution` a simple immutable DTO with its generated package-private name/confirmation
  constructor and two convenience constructors: a name defaults to unconfirmed; a successful canonical lookup result
  supplies its non-null name with confirmation. Reject a nameless lookup result. Keep name selection and confirmation
  decisions in `AnnotationTypeNameResolver`; do not put declaration lookup, qualification inference, or Spoon-context
  validation in the result DTO.
  Before caching annotation identities or reading repeatable metadata, require known final source declarations to be
  annotation types. Fail on a known wrong-kind declaration; do not constrain intermediate owners or require unavailable
  external declarations.
- In no-classpath annotation lookup, inspect lexical declarations and unavailable inherited scopes before trusting an
  AST reference to a source-declared type. Cache scope-independent facts without moving scope-dependent decisions.
  Include local and anonymous classes in the enclosing-type scope index. Keep that index separate from canonical
  declaration indexes. Annotation interfaces cannot be declared directly or indirectly inside local or anonymous
  classes (JLS 9.6), so legal source annotation declarations remain covered by the top-level/member metadata index.
  Follow inherited intermediate owners and check their own parent chains before resolving the next inherited segment.
  Search inheritance by the requested member name and stop each branch at a declaration that hides its ancestors;
  do not reject a resolved member solely because a hidden ancestor is unavailable.
  In the final fallback-name selection for ordinary source files, retain written qualification independently of
  external annotation-use AST names. Such references can miss unseen same-package types. Confirm canonical names
  from declaration/import evidence or an actual runtime-JDK annotation found for a module descriptor after excluding
  matching single imports and wildcard imports.
  Carry that confirmation with each resolved name; matching a fallback spelling to a canonical-name index must not
  authorize source metadata lookup or promote the use to a confirmed identity.
  `AnnotationTypeNameResolver` always returns a named `AnnotationTypeNameResolution` with canonical-name
  confirmation. Select fallback names inside this resolver; blocked lookup or insufficient context
  retains the unconfirmed original written name. Terminal `UNRESOLVED` must still stop subsequent lookup stages.
  Use the `?.` namespace for every unconfirmed dependency key, including fallback uses and unresolved `@Repeatable`
  container roots. Keep unprefixed names in lookup result models; encode confidence when constructing dependency keys.
  Cache only confirmed canonical annotation type names as keys, mapping them to final dependency keys. Unconfirmed
  terminal containers are values only, and per-use fallback keys remain outside the cache. Determine metadata-name
  confidence from source declarations and exact single-type imports before eager traversal; do not keep a parallel
  confirmed-name set. Match aliases against current input keys and both sides of source repeatable relationships.
  Keep root-key cache access in a `computeIfAbsent` wrapper around a cache-independent computation method. The
  computation must neither access the cache nor call its wrapper; validate declarations and detect cycles locally.
  During source-family preparation, index confirmed terminal identities only after their computation returns.
  Collectors may read the prepared relationship index.
  Prepare source repeatable relationships before collecting group aliases, including repeated annotation types with no
  earlier confirmed use. This discovery preserves defaulted-container order and cannot be replaced by caching only
  observed confirmed uses.
  Keep written-name retention and bounded qualification enrichment in one final stage. After excluding wildcard
  imports, handle module descriptors before the ordinary-file qualified-name guard. Look up the first segment as a
  public top-level `java.lang` type before interpreting an explicit `java.lang.` prefix; follow public declared and
  inherited member types and require the selected final type to be an annotation. Missing or ambiguous paths retain
  the unconfirmed spelling. Use the actual runtime JDK, independently of annotation-use AST presence, and derive the
  canonical name from the selected type rather than concatenating a presumed package prefix.
  Keep the module-context decision and its source/import preconditions in the resolver. Cache public top-level
  `java.lang` types once in `RuntimeTypeUtils`; inspect runtime types without initialization or loading application
  dependencies, and do not read their repeatable metadata for ordering.
  In ordinary files, enrich a simple name only when the AST reference's package matches the current package.
  Such package agreement does not confirm canonical identity: a missing external declaration can leave Spoon with
  a wrong same-package name for an inherited annotation. A type reference does not prove a declaration in this file.
  Named and unnamed ordinary packages both allow unseen same-package homonyms that can hide `java.lang` types.
  Runtime-JDK lookup reflects the running JDK's API, not another project target's API. Keep ordinary-file shadowing
  protection even when runtime lookup finds a matching platform type.
- Report annotation ordering diffs with the existing formatting-violation message and diff renderer.
- Preserve relative source order for repeated annotation type identities, including visible occurrences nested in
  explicit containers. Resolve identities across written qualifications; confirmed distinct types sharing a simple name
  remain independently sortable. Different qualified spellings alone do not prove distinct nested types: inherited
  owner aliases can denote the same annotation. When a pair sharing a simple name cannot be distinguished, including
  unresolved short names and wildcard/static imports, conservatively preserve its relative order. Retain original
  type/container keys and add each occurrence's alternative group keys once. Do not expand alternatives transitively
  or merge confirmed distinct homonyms into one dependency family.
  Continue sorting independent annotations in the same source group without warnings.
  Use repeatable metadata from declarations in the current source file as additional dependency evidence, including
  defaulted containers; unavailable external metadata must not disable annotation sorting.
  Document the limitation for defaulted containers without a source-declared repeatable relationship, and the available
  sorting opt-out. A local container declaration alone does not establish that relationship.
- Prefer explicit Java types over `var`.
- Prefer normal imports over repeated fully qualified class names.
- Prefer Lombok for routine boilerplate such as getters, setters, constructors, and `toString` / `equals` / `hashCode`
  when it matches the surrounding style.
- For DTO/model/state-holder classes that primarily carry data (in production and tests), prefer immutable Lombok shapes
  (for example `@Value`) unless mutability is required by the use case.
- When a simple data-carrier class only needs a narrower constructor than Lombok's default, keep `@Value` and add the
  constructor visibility override instead of decomposing `@Value` into separate Lombok annotations.
- For data carriers with more than five constructor parameters, use a named Lombok builder and keep the generated
  constructor private, for example with `@AllArgsConstructor(access = AccessLevel.PRIVATE)`.
- For data carriers with five or fewer constructor parameters, use Lombok's generated constructor and keep positional
  calls aligned with declaration order. Remove builders unless features such as `toBuilder` are required.
  Do not add `staticConstructor` factories that only forward these constructor arguments.
- After field renaming or sorting, update positional constructor calls instead of moving fields back or adding explicit
  constructors to preserve the old argument order. Retain explicit constructors only when needed for validation,
  collection write protection or immutable snapshots, deserialization, or inherited-state initialization.
- When an annotation argument only repeats the library or framework default behavior, omit it instead of spelling it out
  explicitly.
- Use the minimal necessary access level for production classes, constructors, and methods.
  - Prefer package-private over `public` when access outside the package is not required.
  - Prefer `private` for nested classes, constructors, and helpers when they are only used by the enclosing type.
  - For nested helper/data-carrier types created only by the enclosing type, keep their constructors `private`; tests
    are not a reason to widen constructor visibility.
  - Apply the same visibility review to Lombok-generated getters. Keep derived views used only inside a model private;
    test their effects through the production-facing contract.
- Keep production models and value objects focused on state plus simple accessors or validation.
  - Move non-trivial business, filtering, parsing, and transformation logic into dedicated service or processing
    classes.
  - Models and value objects must not call the processors that create or consume them, including static helpers.
    Move shared value derivation and validation into neutral utilities or independent services.
  - Prefer named predicates when a numeric model value encodes a domain condition. Derive them from existing state
    when possible instead of storing a redundant flag.
- For models exposing several views of the same data, accept the representation naturally produced by callers and
  derive the other views internally, eagerly or lazily. Simple partitioning or interleaving belongs in the model when
  it enforces consistency. Do not expose independent builder inputs for these dependent views.
- Keep scanner/parser result models limited to data consumed by downstream production code; retain temporary parsing
  state in local variables or private implementation state.
- Keep processing-wide printer settings in the flow and pass them explicitly to serialization. Parsers and source
  models must not retain printer configuration.
- Keep sorted annotation orders out of `SpoonAstModel`. Sorting processors return sorting data and metadata;
  they must not depend on printers or construct printing inputs. Both `SpoonSorter.SpoonSortingResult` and public
  `SortingResult` retain `sortedSpoonAstModel`, the supplied context after in-place AST sorting, beside the separate
  sorted-order map. The facade forwards the nested result's model; subsequent stages read the public result.
  Flows assemble `SpoonPrintingInput` immediately before serialization, retaining only source groups, working AST,
  source text/path, skipped types, and selected annotation orders.
  Do not copy the parsed model to attach sorting output. The returned model and printing context share mutable Spoon
  nodes rather than providing AST snapshots.
- Return requested member-relocation diagnostics plus independent `annotationsReordered` and `membersReordered` flags
  from sorting in `SortingResult`. `membersReordered` covers Spoon declaration order, including top-level types;
  `annotationsReordered` covers source annotation fragments and is derived from the sparse sorted-order map rather
  than stored separately. Derive `isReordered()` as their OR without storing another flag. Flows use this aggregate for
  status and sorting checks instead of recomputing changes from the AST.
- Sort each freshly parsed shared AST only once per flow. Claim its shared `CtCompilationUnit` before the first sorting
  attempt, including unchanged and failed attempts; model views and clones of consumed units must not reset the claim.
  Detect final-order changes while collecting sorted output, after grouping and dependency repair, instead of traversing
  the completed AST again. Compute detailed member diagnostics only when declarations changed and the caller needs the
  report.
- Preserve annotation source-group order during sorting; only annotation order inside each group may change.
  Keep parsed groups and gaps unchanged. Store only changed orders in
  `sortedAnnotationSrcFragmentsByGroupSrcStart` returned by sorting results, mapping each original group start to
  existing annotation fragments in sorted order. The flow passes this same map to the printing input. Missing entries
  retain source order; derive the annotation-change flag from map non-emptiness instead of comparing groups again.
- When one timing statistic covers consecutive operations, measure them in a single `StopWatch.measure` call instead of
  summing separate measurements.
- Keep annotation source models in the standalone `AnnotationSrcGroup` class beside the scanner in
  `translator.spoon.annotation`, with nested annotation, gap, and private base-fragment types.
  Keep group/gap/layout constructors and the annotation-fragment builder package-private; keep the fragment's full
  constructor private. Keep gap types and their list getter package-private; sorting consumes only annotation fragments.
  Construct source models once in the scanner; do not expose gap/group copying APIs.
- Keep annotation ordering keys and semantic constraint metadata in `AnnotationDescriptor`. Annotation fragments retain
  the descriptor plus source ranges and text; gap fragments retain prepared content and whitespace values.
  Ordering algorithms remain in the sorter.
- Measure annotation declaration length in UTF-16 code units from original token spellings, including Unicode escapes.
  Sum measured token and internal-comment lengths; exclude inter-token whitespace and outside comments, retain
  text-block indentation normalization, and use translated names for identities.
- Capture annotation and gap fragments with their source ranges and comment/separator ownership during scanning.
  Prepare fixed gap content and moved-gap layouts for separator requirements zero, one, and two in the scanner.
  Each layout records whether to prepend the selected annotation's prepared separators and which leading whitespace
  to use. Preserve exact separator strings, including mixed line endings. Gaps retain their original code and ranges.
  Select the prepared layout for the final annotation/destination-gap pair during rendering; separator emission is
  not a per-annotation constant. Keep preparation linear instead of materializing all possible pairings.
  Sorting determines only annotation order; it must not prepare gap text, reconstruct source models, or analyze source.
  Group construction retains unmodifiable views of both source-order lists and requires one gap per annotation.
  Producers must not modify handed-off lists; the scanner starts fresh lists for each group. Each group lazily
  interleaves and caches its original mixed sequence and original replacement code.
  `AnnotationSrcGroupRenderer` joins the selected order with prepared gap layouts, using the original gap verbatim
  for its original annotation. It must not read source text or decide comment ownership. `SrcPrinterOutput` delegates
  this joining and owns source-range copying and group replacement for all source files.
- Explicitly annotate field and non-private method nullability with `@NonNull` / `@Nullable` where applicable; private
  method parameters may stay implicit when the intent is already obvious.
- Reference-returning private methods must declare explicit `@NonNull` or `@Nullable` return annotations.
  - Private method parameters must not use Lombok `@NonNull`; it adds redundant runtime null checks for private helpers.
  - Use `@Nullable` on a private parameter only when that private helper intentionally accepts `null`; otherwise leave
    private parameters unannotated.
  - Place method-level nullability annotations on their own line above the method declaration instead of inline in the
    signature.
- Prefer Stream API when it makes the control flow clearer and more concise than imperative loops.
- Prefer `Objects.equals` for nullable value comparisons instead of explicit null-check branches.
  Enforce required non-null values separately so two absent values cannot satisfy a presence requirement.
- When a mutable stream result is needed, collect directly into the required collection instead of copying an
  intermediate collection.
- Keep `Optional` search results as `Optional` when checking presence; do not convert them to `null` solely for that
  check.
- Prefer `Optional.ofNullable(...).map(...).orElse(...)` for nullable lookup, transformation, and fallback chains when
  this removes intermediate null checks and improves readability.
- Prefer a non-null `Optional<T>` over nullable returns for package-level lookup APIs with an expected absent result.
- If a boolean helper is always consumed through negation at its call sites, invert the helper logic and rename it so
  callers stay positive and direct.
- Prefer `get` only for conventional object-model/DTO getters; for computed values, searches, conditional lookups, or
  transformations, prefer a more specific verb such as `find`, `resolve`, `collect`, `compute`, or `merge`.
- For non-get behavior methods, start the method name with a clear verb.
  - Prefer explicit verb-led names such as `find`, `resolve`, `infer`, `collect`, `compute`, `merge`, `parse`,
    `format`, or `render`.
  - Avoid ambiguous prefixes such as `toXxx` when a clearer verb-based name fits the method behavior.
  - For helpers that extract or resolve a value, name that operation and its result; document validation separately.
  - Prefer `infer*` for constructing names from contextual rules without looking up declarations; reserve `find*`
    for actual searches.
  - Prefer `find*` for utility searches, `extract*`, `read*`, or `format*` for inspecting existing representations,
    and `compute*` for derived values. Reserve `resolve*` for decisions in domain logic, such as establishing bindings
    or identities; accepting context alone does not make a utility search a resolution step. Keep the caller's
    interpretation, such as a lookup candidate, out of extraction utility names.
  - Name private validation helpers with `validate*` and return `void` when they only check supplied values.
- For new or substantially revised methods, put the broadest context first, then the owner or target, followed by names,
  selection criteria, and options. Preserve framework override signatures and Lombok DTO constructor declaration order.
- Keep each helper within the responsibility expressed by its name. Sequence sibling processing stages in their caller;
  each stage may delegate its own substeps. Avoid extraction that adds result or state handling without improving
  readability. Split processing classes by coherent responsibilities, document the boundary, and pass required context
  explicitly.
  Name coordinators after their overall result and subordinate processors after their operation or lookup mechanism.
  Make member-type lookup names distinguish direct declarations from superclass/superinterface hierarchy traversal.
- Do not retain state-holder classes or distort control flow solely to satisfy method-count checks.
  Reuse suitable utilities or split them by coherent responsibility before suppressing `PMD.TooManyMethods`.
  A narrowly scoped suppression is allowed for a cohesive utility class without a sensible split; document the reason.
  Keep complexity checks enabled and reduce complexity through helpers instead of raising thresholds or suppressing
  them.
- Prefer static imports for frequently used assertion/helper methods when repeated type-qualified calls add noise.
- Do not use `protected` fields; keep fields `private` and expose only the narrow protected accessor methods that
  subclasses actually need.
- Prefer the shorter `src*` naming family (`srcFile`, `srcPath`, `srcCode`, `srcDiff`) for source-related variables and
  parameters.
- Prefer clear, fully descriptive variable names; avoid non-obvious abbreviations unless the abbreviation is an
  established term (for example `URL`, `URI`, `ID`) or an established repository abbreviation such as the `src*` naming
  family.
- Lambda parameters are also variables and must follow the same naming rule — use clear, descriptive names; never use
  single-character abbreviations such as `m`, `s`, `e`, or `t` for lambda parameters.
- Make a variable's domain role explicit when its type is generic, for example `annotationOwner` or
  `annotationComparator`. Name maps by both values and key meaning, such as `annotationDescriptorsBySrcStartOffset`.
  For nested collections, document each level's element meaning and the scope of one call; distinguish per-item data
  from collections covering a whole source file.
- Name annotation criterion lists `annotationOrderingRules` across strict and flexible input/unified models.
  The YAML property remains `annotations-ordering`.
- When collection ordering matters to its use, reflect that order in the variable name.
- Prefer `Set` over `List` for collections of unique values whose iteration order is irrelevant to their consumers.
- When an index must cover every requested key, reject missing entries with a diagnostic exception instead of
  substituting empty/default values. Validate this invariant once before dependent lookups where practical.
- Distinguish immutable source-order group views from separately stored annotation orders selected by sorting.
  Reserve sorted-order names for sequences already sorted by the comparator; rendering joins these views explicitly.
- Name helper and data-carrier types after the entities they represent, for example `AnnotationSrcFragment`, so their
  purpose remains clear at import and usage sites outside the enclosing class.
- Every non-private production method and constructor must have concise JavaDoc that states the purpose, documents
  parameters, and documents the return value when applicable.
  - Exception: do not add JavaDoc to standard `Object` overrides such as `equals`, `hashCode`, and `toString`.
- Annotate every non-private method's reference-type parameters and non-primitive return type with explicit nullability
  using `lombok.NonNull` or `org.jspecify.annotations.Nullable`.
- Do not change standard `Object` method signatures when overriding them.
  - Do not add nullability annotations to `Object` overrides just to satisfy local conventions.
  - Preserve standard contracts exactly, especially `equals(Object)`.
- Use `@UtilityClass` for classes that contain only static utility methods and should never be instantiated; this
  applies to both production code and test utilities.
- Repository-wide convention: do not introduce Java records in production code or shared test infrastructure; use
  classes with Lombok instead where appropriate.
  - Java fixtures under `src/test/resources/test-cases/**` may still use records when a scenario explicitly tests record
    handling.
- Organize production classes by their owning processing stage. Keep source parsing and printing in `translator`,
  ordering in `sorter`, and formatting in `formatter`.
  Group the source annotation scanner and its name, repeatable-container, and dependency-key preparation helpers in
  `translator.spoon.annotation`; they prepare the model before sorting. Keep their internal helpers package-private.
  Reserve neutral packages such as `core.spoon` and `core.utilities` for helpers with actual production consumers
  in different processing stages. Review both whole classes and individual methods against their actual consumers;
  generic names and possible future reuse do not justify a neutral API. Keep helpers consumed within one stage as close
  as possible to their owning processors, even when shared by several processors or independent of Spoon.
  Models may stay beside their producers to restrict initial construction with package-private access, even when read
  by several stages. Cross-stage consumption alone does not require placing a model in a neutral package.
  As an explicit exception, keep `RelocationDetector` and its package-private `LongestIncreasingSubsequenceUtils`
  beside the shared `MemberRelocation` model in `core.spoon`, so the model constructor remains package-private.
  `Sorter` is the detector's only production caller; this co-location preserves construction access rather than
  indicating cross-stage use of the detector.
  Shared utilities must not depend on stage-specific processors or helpers. Extract genuinely shared operations into
  the neutral package and update callers instead of adding forwarding wrappers.
- Wrap collections once when returning or handing off a collection that was built mutably in the current method,
  including private/package-private helpers, so the receiver cannot mutate the handed-off instance.
  When handing collections to a model, enforce this protection in the model constructor instead of requiring callers
  to pre-wrap them.
- Do not add a second unmodifiable wrapper or defensive copy when an input already has the required read-only ownership
  contract, or when a collection remains local. Passing a local collection to a helper that only reads it and does not
  retain it is not an ownership handoff.
- Immutable models accepting caller-owned collections must retain an immutable snapshot, not a read-only view of mutable
  input. Reuse immutable instances where supported, for example through `List.copyOf`.
  - Exception: annotation ordering criteria in strict and flexible YAML/unified configurations use unmodifiable views
    without defensive copies. Callers must not mutate supplied lists after construction; do not add tests that assume
    snapshot isolation for these criteria.
  - Exception: annotation source groups retain unmodifiable views without copying. Callers must not mutate the supplied
    lists after handoff; the scanner uses fresh lists for subsequent groups.
  - Exception: sorting results and `SpoonPrintingInput` retain sorted annotation-order maps and fragment lists already
    made read-only by their producers. The printing input also retains existing read-only source/opt-out views.
    Producers must not modify handed-off collections; do not wrap or copy them again.
    When SpotBugs cannot infer this verified read-only handoff, suppress only `EI_EXPOSE_REP2` on the relevant
    constructor with a justification naming the producer's protection.
  - Exception: annotation dependency-key sets, resolver identity indexes, and prepared import contexts use unmodifiable
    views without defensive copies. Producers must not modify handed-off collections. Reuse already unmodifiable
    inputs when contents do not change; allocate new collections for transformed contents.
  - Exception: member-relocation diagnostics in `SortingResult` and `MemberRelocation` use unmodifiable views without
    defensive copies. Producers must not mutate handed-off lists. Test returned-list write protection and processing
    behavior; do not simulate caller mutations forbidden by this ownership contract.
- Reject null entries in YAML configuration collections during deserialization. Configure the YAML mapper centrally;
  custom collection deserializers must enforce the same rule. Preserve nullable optional properties.
- When a mutable buffer is reused or cleared after handoff, retain an immutable snapshot; an unmodifiable view still
  reflects subsequent buffer mutations.
- Non-obvious build/configuration workarounds (for example temporary dependency overrides for transitive
  vulnerabilities) must include a nearby comment that explains why the workaround exists, which upstream component
  requires it, and when it can be removed.
- Document non-obvious regular expressions next to the pattern, including matching rules and examples that match or
  fail.
- Explain non-obvious parser/model branches with nearby examples of the input or AST shape and the resulting behavior.
- For stateful token parsers, document the expected current token on entry and exit, including cursor movement when no
  result is returned.
- For parser errors about the current token, read its offset from the cursor at the failure site instead of passing
  diagnostic-only offsets through parsing methods.
- When a piece of code intentionally keeps a non-obvious, previously reverted, or easy-to-"simplify" behavior because of
  an external constraint, leave a nearby comment that explains why it exists, what constraint it preserves, and why it
  should not be changed casually.
- When debugging uncovers a non-obvious runtime/framework edge case (for example parser/evaluator recursion traps),
  document the guard/workaround with a nearby code comment so future refactors do not remove it accidentally.
- Build and validate with JDK 21.
- Preserve the root `.mvn` directory: shared quality-gate paths use `maven.multiModuleProjectDirectory`. Validate
  changes to shared build-resource paths from both the repository root and the affected module directory.
- Automatically run tests only for the module being changed; for changes spanning modules, limit test runs to those
  modules.
  - If prerequisite modules need to be built, skip their tests unless they are also being changed.
  - The user runs full-project verification (`mvn -B -ntp verify`) when needed. Agents may run it only when explicitly
    requested for the current task.
- Repository build, test, coverage, and quality-gate tooling must be cross-platform and based on Java/JVM tools or Maven
  plugins. Do not add PowerShell, Bash, or batch scripts for these tasks.
- Use the existing Maven plugins and JaCoCo for coverage collection, reports, and supported threshold checks. Document
  tool limitations and unreachable branches instead of introducing custom coverage-report parsers or counters.
- Keep one-off measurement and diagnostic artifacts under `target/`; do not turn them into permanent repository tools
  unless explicitly requested.

## Test conventions

### Goals

- Make tests readable at a glance with predictable structure.
- Make failures actionable with clear names and error messages.
- Keep maintenance low through shared helpers and minimal duplication.
- Avoid false regressions caused by broken fixtures; fixtures must compile.

### Tooling and libraries

- JUnit 5 is the test runner.
- AssertJ is the assertion library.
- Do not use `org.junit.jupiter.api.Assertions.*` in new or updated tests.
- Prefer ordinary imports over repeated fully qualified names in test code.
- Prefer using production pipeline building blocks such as parsers, converters, compilers, and factories instead of
  test-only reimplementations.
- When an annotation argument in test code only repeats the library or framework default behavior, omit it instead of
  spelling it out explicitly.
- When test code overrides standard `Object` methods, preserve the standard signature exactly; do not add nullability
  annotations to `Object` overrides in tests.
- Test code and test resources follow the same repository SPDX policy with the required file-level lines listed above.

### Code reuse and deduplication

- Before copying code into another test, search for an existing shared test utility and reuse it.
- If similar fragments appear in more than one test, or are likely to be reused, extract them into a shared test utility
  class.
- Re-run this reuse analysis when adding tests, refactoring tests, and during cleanup passes.
- Before adding a scenario test, trace the existing runner's fixture discovery, configuration loading, compilation,
  output comparison, and repeated-processing checks.
- Express source-output scenarios through the existing E2E fixtures before adding Java test methods:
  - Use numbered scenario directories with `input/<Name>.java`, `expected/<Name>.java`, and `config.yml` under
    `core/e2e/reorder/` or `core/e2e/printer/scenarios/`; regression scenarios use `core/e2e/regression/` and may omit
    the config.
  - These paths are relative to `src/test/resources/test-cases/`. The shared runner discovers inputs and matches
    expected files by name.
- Reuse the shared runner's compilation, processing, full-output comparison, and `CHECK_FAIL_FAST` fixed-point check. Do
  not duplicate these steps in scenario-specific test code.
- Distinguish fixture compilation from output compilation: Maven compiles `valid/**/*.java` inputs, while the E2E runner
  compiles original and processed sources. Neither requires an additional per-scenario compiler invocation or separate
  compilation of an identical expected file.
- Keep separate Java tests only for contracts the fixture runner cannot express, such as source offsets, immutable
  result state, virtual sources, or invalid helper inputs. For line endings and whitespace variants, transform fixtures
  minimally and reuse the shared processing assertions.
- When tests in a different package need to instantiate a production type with package-private construction, prefer a
  dedicated test creator/helper in the target package instead of widening production visibility.

### Naming

- JUnit test method names must follow exactly 3 segments: `subject_condition_expectedResult`.
- This naming rule applies only to executable test methods (for example `@Test`, `@ParameterizedTest`, and other JUnit
  invocation annotations).
- It does not apply to lifecycle and helper methods such as `@BeforeEach`, `@BeforeAll`, `@AfterEach`, `@AfterAll`, or
  private utilities; name those with normal Java conventions such as `setUp` or `tearDown`.
- `subject` names what is being tested and usually mirrors the production method, command, or feature name.
- `condition` states only the relevant precondition or input shape.
- `expectedResult` states the observable outcome.
- Do not use filler words such as `should`, `when`, `then`, or `must` inside the method name.
- Avoid vague words such as `works`, `ok`, or `smoke1`; prefer intent-revealing words.
- Keep the condition minimal but specific.
- Prefer `<ProductionClassName>Test` for unit tests.
- Prefer `<FeatureOrScenarioName>Test` for integration tests that cover a pipeline.
- Make test class names and main Java fixture type/file names describe the tested behavior without relying on package or
  scenario-directory names.
- Use distinct main fixture names for different scenarios; keep each `input/` and `expected/` pair aligned.
- If you need multiple scenarios, prefer `@Nested` classes instead of splitting into many test classes.

### Structure

- Split test bodies with multiple logical phases into contiguous blocks using comments:
  - `// Given`
  - `// When`
  - `// Then`
- Omit phase comments when the entire test is a single block, including direct calls to shared scenario checks.
- Keep `// When` limited to the tested invocation and assignment of its result. Put preparation, previous-call history,
  snapshots, and expected values in `// Given`; put assertions in `// Then`.
- Do not insert empty lines inside a block.
- Insert exactly one empty line between blocks.
- There must be a blank line before `// When` and before `// Then`, and before combined blocks such as `// When / Then`.
- Do not insert an empty line at the very beginning of the method body before `// Given`.
- Keep each block contiguous and focused.
- It is valid to merge blocks when it improves readability.
- Exception tests with setup may use `// When / Then` because the assertion captures both the action and the
  expectation.
- Very small tests may use `// Given / When` together if separating them would add noise.
- Combined blocks are allowed only when they stay contiguous and clear.
- Use parameterized tests when they reduce repetition and improve readability.
- The 3-segment method naming rule still applies to parameterized tests.
- Do not introduce a `// Given` block for a single obvious local variable assignment.
- If setup is trivial and self-explanatory, omit `// Given` entirely or use a combined block.
- Use `// Given` only when it groups multiple setup statements or improves readability.

### Fixtures and resources

- Store fixtures under `src/test/resources/test-cases/**`.
- Use explicit scenario folder names instead of generic names such as `example/`.
- Prefer resource fixtures under `src/test/resources/test-cases/**` over large inline YAML or Java strings embedded
  directly in test classes.
- Do not keep non-trivial multi-line textual fixtures such as YAML, JSON, XML, Java source, or long expected-output
  snippets inline in test code.
- Do not write large fixture content as inline string literals and then persist it to temp files during test setup.
- Store original, expected, and config fixture files under `src/test/resources/test-cases/**`, then copy or read them
  via test resource helpers.
- Store those fixtures under `src/test/resources/test-cases/**` and load them through shared helpers such as
  `TestCaseResourceUtils`.
- Only tiny one-off inline snippets are acceptable when extracting a file would hurt readability.
- For formatter-focused fixtures under `src/test/resources/test-cases/**`, keep `input/` resources valid but
  intentionally not already formatted like `expected/`, so the scenario demonstrates a real formatter rewrite.
- Use `valid/` for fixtures that must compile and be compilable by the build gate.
- Use `invalid/` for fixtures that may intentionally not compile in negative tests.
- All `valid/**/*.java` fixtures must compile as part of the build.
- Prefer fixtures that are self-contained and depend only on the JDK.
- Prefer single-file fixtures.
- If multiple files are required, keep them in the same scenario folder.
- When a fixture verifies ordering inside one logical group, prefer including multiple declarations of the same kind and
  cover secondary ordering rules such as visibility and alphabetical order where the language allows it.
- Prefer classpath-based resource access via `ClassLoader.getResourceAsStream` over filesystem paths.
- Use shared helpers such as `TestCaseResourceUtils` to read resources.
- If a regression test verifies the built-in default configuration, load the real embedded `default-config.yml` through
  the production default-loading path instead of duplicating it in test fixtures or inline YAML.
- Keep resource identifiers as typed values where feasible, such as `URL`, not raw strings.
- When using `ClassLoader.getResourceAsStream` (preferred), do not use a leading `/`; classpath resource names are
  always relative to the classpath root.
- Only when using `Class#getResourceAsStream` should the path start with `/` to indicate an absolute classpath resource.
- If you need to resolve a file under a directory, resolve it via a dedicated helper, not via deprecated URL
  constructors.

### Shared test setup and one-time initialization

- If multiple tests in the same test class use the same expensive or repetitive setup, initialize it once at the
  test-class level instead of recreating it in every test.
- Reserve `private static final` constants for simple fixed values such as strings, numbers, enums, and small literal
  collections. Immutability alone does not make prepared test data a constant.
- Initialize fixed fixture locations (`Path` or `URL`) as `private static final` constants through existing resource
  helpers. Do not introduce `@BeforeAll` or mutable fields solely to resolve a location.
- Prefer `private final` fields when per-instance initialization is sufficient and the object is safe to share across
  tests in the class.
- Use `@BeforeAll` for expensive or multi-step preparation such as loading fixtures, compiling configuration, parsing,
  sorting, or serializing source, even if this work could be hidden behind a method call in a field initializer. Store
  the prepared state in ordinary test fields, preferably instance fields when the test lifecycle allows it.
- If `@BeforeAll` must be non-static, use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`.
- When `@MethodSource` providers consume state prepared in an instance `@BeforeAll`, make those providers non-static as
  well.
- Do not share mutable objects across tests if the code under test may modify them.
- In that case, keep a single immutable base representation and create a fresh copy per test, or initialize the mutable
  object in `@BeforeEach`.
- Avoid duplicating the same setup snippet across multiple tests in the same class.
- If repeated setup appears, refactor it into a shared field initializer or a dedicated setup method.
- Keep the `// Given` section focused on test-specific inputs; common setup belongs to fields, `@BeforeAll`, or
  `@BeforeEach`.

### Constants grouping

- Keep a small amount of shared test state as regular fields at the top of the class when that remains easy to scan.
- Good candidates to keep at the top include `@TempDir` fields and one or two obvious shared constants that help the
  first test read naturally.
- If a test class contains many shared constants and they start cluttering the top of the file, group them into a nested
  `Constants` class instead of stacking a long constant block before the tests.
- Prefer a nested `Constants` class once the constant list is long enough that it pushes test methods noticeably down
  the file or makes the start of the class hard to scan.
- Keep the `Constants` nested class at the end of the test class.
- Do not move everything into `Constants` mechanically.
- Keep computed fixtures and prepared test state out of `Constants`, even when the result is a string or another
  immutable value; initialize that state in setup and store it in ordinary test fields.
- Keep only the cluttering shared constants there, while ordinary test fields such as `@TempDir` stay near the top.

### Assertions and test utilities

- Do not add dedicated unit tests whose only purpose is to test test-only utility classes or helper methods.
- Validate test utilities indirectly through the real unit and integration tests that use them.
- If a test utility becomes complex enough to deserve direct behavioral tests, move the logic into production code or
  simplify the helper.
- Place private static helper methods and test-only utility code at the end of the test class, after all test methods
  and after nested `Constants`, if present.
- Keep the top of the test class focused on test scenarios, not helper implementation details.
- Use assertions only for validating the test contract and expected results.
- Prefer AssertJ with `assertThat(...)`.
- Test utility methods must not call `fail(...)` or use assertions for control flow.
- If a test utility cannot proceed because of missing resources, ambiguous matches, or invalid input, it must throw a
  descriptive runtime exception.
- Use `IllegalArgumentException` for invalid inputs.
- Use `IllegalStateException` for unexpected setup or state.
- Use `UncheckedIOException` for I/O problems.
- For internal test-only utility classes, prefer Lombok for null checks.
- Use `@NonNull` on non-private parameters instead of `Objects.requireNonNull(...)`.
- Use `@UtilityClass` for pure utility classes.
- For test DTO/model/state-holder classes that mainly carry data, prefer immutable Lombok (`@Value`) unless mutation is
  required for the scenario.
- Private helper method parameters must not use Lombok `@NonNull`; use `@Nullable` only when the helper intentionally
  accepts `null`.
- If a private test helper returns a reference type, annotate the return contract explicitly with `@NonNull` or
  `@Nullable`.
- If a helper returns `Optional<T>`, treat that as part of the test logic.
- Prefer `findXxx(...)` returning `Optional<T>`.
- Prefer `requireXxx(...)` returning `T` and throwing `IllegalStateException` if missing or ambiguous.
- If presence or absence is a product expectation, assert it in the `// Then` block.
- If absence is a broken fixture or setup, use `requireXxx(...)`.

### Temporary files and formatting

- Tests must not write into `src/test/resources`.
- Use `@TempDir` (JUnit 5) or write into `target/`.
- Avoid inserting empty lines between closely related constant declarations.
- Use a blank line only to separate semantic groups.
- Keep `// Given` immediately after the opening brace.
- In test utility classes, group fields and constants by meaning; do not insert a blank line after every field by
  default.

### Code style in tests

- Prefer fully descriptive variable names instead of names such as `i`, `tmp`, or `m`.
- Prefer Stream API when it makes the flow clearer.
- Keep helpers small and single-purpose.

### JUnit 5 visibility rules

JUnit 5 discovers test and lifecycle methods through reflection and does not require `public` visibility.

- JUnit 5 test classes should use package-private visibility by default (no access modifier).
- JUnit 5 test methods annotated with `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, or `@TestTemplate`
  should use package-private visibility by default.
- JUnit 5 lifecycle methods annotated with `@BeforeEach`, `@AfterEach`, `@BeforeAll`, or `@AfterAll` should use
  package-private visibility by default.
- `@Nested` test classes should use package-private visibility by default.
- Helper methods, constants, and helper nested classes that are only used inside one test class must remain `private`.
- Shared test utilities that are reused across test classes in the same package may remain package-private.
- `public` must not be added to test classes or annotated test/lifecycle methods unless there is a documented technical
  reason (for example, a test framework extension that requires public access by reflection from outside the package).
- `private` must not be applied to JUnit test or lifecycle methods; JUnit Jupiter must be able to discover them.
