<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Changelog

All notable changes to JHarmonizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## 1.1.0 - unreleased

### Added

- Russian-language annotation type-resolution guide with the main diagram first, repeatable-order semantics,
  dependency-key construction, lookup stages, precedence rationale, and examples. Clarified Java type-name segments,
  lookup results, inherited annotation names, dependency preparation, and reproduced Spoon 11.5.0 binding limitations.
  Verified late source binding for package-qualified inherited annotation names with an unmodified parsed AST.
- Regression fixtures for repeatable annotation source and reflection order, including qualified names, nonadjacent
  repetitions, explicit containers, and distinct annotation types sharing a simple name.
- Parsed-source regression cases for qualified annotation fallbacks with missing or matching AST references and
  relative spellings that are suffixes of external canonical names. Covered a package-qualified array annotation
  whose declaration is in the same file but whose use is absent from Spoon's AST, reaching stage 5 after `NOT_FOUND`
  at stages 1–4. Added a compiled case where an unseen owner in the same package hides an imported type;
  dependency keys preserve the source-declared repeatable annotation order despite Spoon's incorrect external binding.
  Covered a qualified annotation spelling that matches an ordinary source class's canonical name without selecting
  that class for annotation metadata.
- A compiled-source regression case for an unavailable same-package annotation rebound by Spoon to `java.lang`,
  with the contextual fallback rejecting that binding.
- Global annotation sorting by name, name length, complete declaration length, or argument text, with ascending criteria
  and ordered tie-breakers. Defaults to declaration length, name length, alphabetical name, then alphabetical argument
  text; an empty list preserves source order. Argument comparison includes parameter names and assignments, excluding
  comments and inter-token whitespace. Settings propagate through configuration overlays and compile into a reusable
  comparator.
- Source-fragment annotation sorting and printing for packages, modules, types, members, parameters, local variables,
  record components, and type uses, with Java-lexer coverage for annotations absent from Spoon's model,
  opt-out support, and compilation/idempotence regression fixtures.

### Changed

- Grouped annotation scanning, source-fragment models, metadata preparation, and type-name/import/inheritance helpers
  under `translator.spoon.annotation`. Parser tree traversal stays in `translator.spoon.SpoonTypeTreeUtils`;
  `core.spoon` retains shared models, cross-stage type operations, and relocation diagnostics alongside their
  package-private LIS helper. `MemberRelocation` retains its package-private constructor.
- Restricted initial annotation-group and fragment construction to the scanner's package. Sorting copies existing gaps
  and groups with prepared whitespace and order while retaining their original source ranges.
- Spoon type traversal streams declarations directly into indexes. Parsing statistics count types and members in one
  traversal without intermediate aggregate lists; removed redundant root-type and member-list helpers.
- Unclosed annotation-argument diagnostics use the lexer's end-of-source offset without passing a diagnostic-only
  annotation-start parameter through the scanner.
- Annotation dependency-cache access uses a `computeIfAbsent` wrapper around independent repeatable-chain traversal.
  Computation validates source declarations and detects cycles locally without accessing the cache.
  Source repeatable relationships remain prepared before scanning annotation groups.
  Added a compiled array-annotation regression where a defaulted container precedes any confirmed repeated-type use.
- Annotation dependency keys uniformly mark unconfirmed uses and metadata container roots with `?.`.
  The cache maps confirmed annotation names to final keys, using source/import evidence before metadata traversal.
  Removed the parallel confirmed-name set and container-root set; alias candidates use the current sequence and both
  sides of source repeatable relationships. Unconfirmed terminals and per-use fallbacks have no separate cache entries.
  Added compilable external-inheritance and imported-container regressions.
- Annotation preservation-key collection uses one candidate index for possible aliases and repeatable families,
  adding candidate type/container keys directly to each annotation fragment. Removed synthetic pair strings,
  their extra index, and quadratic pair construction, along with the separate input-key set and reverse name index.
  Candidates are limited to current input keys and known repeatable-container keys instead of unrelated homonyms.
  Confirmed distinct homonyms retain disjoint key sets; uncertain occurrences can carry several alternatives.
  Input sets already containing all candidates are reused; singleton candidates reuse the input list after validation.
  Each unique input key seeds its own candidate once; confirmed keys validate their terminal cache entry.
  Singleton inputs also skip alias expansion. Missing confirmed identity entries fail with a diagnostic exception.
- Repeatable class-literal validation compares Spoon type-reference objects with null-safe equality after requiring
  a container type reference.
- Annotation type-name lookup prepares shared import facts once per source file, reusing single-type indexes,
  static-member blocker names, all wildcard imports, and available source-owner indexes across lookup and dependency
  preparation. Presence checks use existing collections; model-order lookup failures are preserved.
  The import-context constructor enforces collection write protection without copies or repeated caller-side wrappers.
- All unconfirmed annotation names use `?.` lexical dependency keys; index membership no longer changes their key form
  or suggests another resolution state. Covered a qualified external-container fallback linked to a source-declared
  repeatable annotation through collector dependency keys.
- Annotation-name resolution always returns a non-null named result with canonical-name confirmation. The resolver
  selects contextual or written fallbacks and retains the original written name when lookup is blocked.
- Single-import type-name extraction excludes unresolved static members as well as known fields, methods, and
  wildcard imports. Centralized the type-only contract; the resolver handles static-name blockers separately.
- Ordinary-file qualified annotation fallback retains the written name with or without an external AST reference.
  Removed the written/AST-name comparison while preserving conservative dependencies for possible aliases.
- Annotation name results distinguish confirmed canonical identities from provisional names. Source/import lookup and
  runtime-JDK annotation lookup for module descriptors can confirm identity; string collisions do not select unrelated
  source types or promote fallback uses to confirmed identities. Confirmed contextual names also enter the collector's
  identity index.
- Module annotation fallback looks up actual public `java.lang` types from the running JDK, with a shared top-level
  index and public nested/inherited member traversal. It confirms existing annotation types for simple and qualified
  spellings, derives canonical declaring-owner names, and retains unknown paths without inventing a package prefix.
  Type inspection does not initialize classes or load application dependencies; ordinary-file shadowing protection
  and source/import precedence remain unchanged.
- Restricted the implicit `java.lang` annotation fallback to module descriptors; ordinary files reject it for both
  named and unnamed packages because unavailable same-package types can shadow the inferred annotation.
- Module annotation fallback accepts unrelated single imports and rejects wildcard imports before checking AST
  availability. Matching static-member names still block lookup, including resolved field/method references that can
  omit an inherited annotation type imported by the same declaration.
- Unified qualified-name retention and bounded simple-name enrichment in one final fallback stage.
  Module lookup confirms canonical identity from runtime-JDK types independently of AST-use presence;
  ordinary-file enrichment is limited to a matching current package and remains provisional. Renamed the result flag
  to `canonicalTypeNameConfirmed`; the resolver chooses both values stored in the immutable result DTO.
- Covered a local class inheriting an annotation from another source file: Spoon can report a same-package reference
  that omits the declaring owner, so package agreement alone does not confirm canonical identity.
- Annotation lookup includes local and anonymous enclosing classes before considering outer declarations or imports.
  An unavailable inherited scope now blocks a same-name source annotation instead of confirming the wrong type.
  Scope indexes remain separate from the top-level/member declaration indexes used for annotation metadata.
- Source-binding confirmation relies on `getDeclaration()` in the original single-file model instead of repeating
  declaration-list membership checks; missing external declarations still continue to final fallback-name selection.
- Annotation lookup results validate selected type declarations inside their constructor, removing repeated checks
  from declaration lookup callers while preserving rejection of non-annotation types.
- Single-import extraction no longer silently accepts an unresolved import without its source spelling; documented
  wildcard, field, and method import forms that provide no single-type name.
- Renamed the annotation-name entry point to `resolveAnnotationTypeName`: qualified fallbacks can retain written names
  without proving canonical identity. Lookup behavior remains unchanged.
- Canonical annotation lookup results derive names from typed annotation declarations or validated single-type imports
  instead of raw strings. Wrong-kind declarations, including available external imports and wildcard candidates,
  fail before result construction; unavailable imports retain canonical-name evidence.
- Annotation dependency preparation rejects a resolved source class or interface when an annotation declaration is
  required, before caching or reading repeatable metadata. Unavailable external declarations remain accepted.
- Separated annotation type identity lookup from repeatable-container metadata and order-preservation key resolution.
- Annotation type resolution uses one traversal for declared and inherited paths, shares a canonical source-type index,
  and consults indexed annotation-use bindings only for fallbacks after source scopes and imports.
- Annotation source lookup follows inherited intermediate owners and multi-level superclass/interface chains in the
  current file, including uses missing from Spoon's annotation AST. Each inherited segment searches only until its
  declaration is found on each branch; unavailable ancestors hidden by that declaration do not block resolution.
  Late source bindings also validate package-qualified paths and source wildcard-import scopes; unavailable or
  conflicting wildcard scopes block unverified bindings.
  Distinct inherited homonyms retain separate canonical identities despite Spoon's structural declaration equality.
- Renamed annotation source-tree lookup to reflect the complete search through declared nested types.
- Annotation identity lookup sequences enclosing types, source type trees, imports, source-binding confirmation, and
  final fallback-name selection in one coordinator, preserving terminal unresolved results.
- Annotation lookup names distinguish lexical resolution from import/context fallback; source-binding null checks
  explain their invariant.
- Explicit type imports take precedence over matching unresolved static imports during annotation identity lookup.
- Annotation identity lookup computes source-declaration membership once while preserving lexical-scope and
  inherited-type precedence.
- Annotation-name scanning reports the unexpected token's source offset directly from the lexer cursor.
- Unexpected `@Repeatable` expression shapes, inconsistent class-literal models, and cyclic container relationships fail
  processing for the affected file instead of accepting alternate AST forms or silently dropping dependency metadata.
- Kept annotation type-name formatting in `SpoonTypeNameUtils` and source type-tree traversal in `SpoonTypeTreeUtils`.
  Inherited member-type lookup uses `SpoonTypeInheritanceUtils` with an iterative parent-reference queue and per-call
  local state. `AnnotationTypeNameResolver` determines names with declaration traversal delegated to
  `AnnotationTypeNameFromDeclarationsResolver`; `AnnotationDependencyKeyResolver` handles repeatable containers
  and keys.
- Annotation identity resolution uses the owning resolver's source-file context and indexes directly; each lookup
  accepts only the annotation's source offset and written name.
- Annotation dependency-key preparation reuses unchanged unmodifiable inputs and wraps completed collections without
  defensive copies. Local working indexes no longer create read-only snapshots.
- Annotation import resolution retains each validated type name instead of repeating a nullable lookup.
- Renamed scanner counters and measurement helpers to identify source-length units. Added numerical examples explaining
  declaration-length measurement, saved lexer boundaries, and comment ownership.
- Annotation scanning checks for raw `@` or its Unicode escape with one or more `u` markers before invoking JDT,
  avoiding lexical scans for unrelated escapes.
- Inlined scanner-prepared gap values into `AnnotationSrcGap` and removed `AnnotationGapLayout`. Scanner construction
  uses a named builder; sorting copies original state with a selected prefix while sharing immutable strings.
- Source-text searches share private forward and backward scans with explicit character predicates. Java token
  measurement reuses bounded line-end lookup.
- Source-text helpers use half-open ranges and explicit whitespace, indentation, and line-separator names. Indented
  content lookup reuses bounded whitespace search; indentation lookup rejects offsets outside the source.
- Annotation parsing encapsulates optional arguments and their token-length snapshots, with examples for comment
  ownership and separator requirements. Unchanged preservation keys retain their existing source fragment.
- Moved Java token source handling into neutral `JavaTokenSourceUtils`, with an explicit complete-token input contract.
- Annotation declaration length counts original Unicode escape spellings in tokens and internal comments, excluding
  outside comments and inter-token whitespace. Text blocks retain lexical argument text and indentation normalization.
- Annotation type-name parsing retains the written name and derives simple sorting names only for outer fragments.
- Moved annotation dependency-key finalization into its owning parser. Fragment-list updates remain local to group
  parsing, and fragments with unchanged keys are reused.
- Centralized annotation scanner token advancement and annotation-start checks, with explicit parser entry/exit
  contracts.
- Consolidated annotation source-group parsing into one gap per annotation and one group handoff, including EOF.
  Group and gap boundaries reuse fragment ranges while preserving comment ownership before annotation declarations.
- Annotation type resolution avoids duplicate cache writes for identity keys while preserving repeatable-cycle guards.
- Consolidated annotation scanning into one `AnnotationSourceScanner.scan` entry point accepting the Spoon source-file
  model and creating a concrete `AnnotationDependencyKeyResolver` internally. Its type-name resolver indexes
  annotation-use AST references; the dependency resolver owns the type-key cache.
- Moved annotation order-preservation group keys from source fragments into `AnnotationDescriptor`, alongside sorting
  keys.
- Renamed annotation criterion fields, getters, and builder methods to `annotationOrderingRules` across input and
  unified configuration models; the YAML property remains `annotations-ordering`.
- Renamed the shared flow base to `AbstractSrcProcessingFlow` to reflect its source-processing pipeline role.
- Sorting rejects repeated attempts for a shared Spoon AST through any model view or sorting entry point,
  including unchanged and failed first attempts. Clones retain the consumed state; reparsing starts a fresh lifecycle.
- Annotation sorting preserves source-group order and collects its aggregate change flag while preparing groups,
  removing group-iterator comparisons. Prepared groups and their flag reuse `ElementOrdering` without a separate
  annotation result type or repeated change detection. Sorting output remains in the nested
  `SpoonSorter.SpoonSortingResult`.
- Sorting detects final declaration and annotation order changes while collecting output, removing separate AST and
  annotation-group checks. Each parsed model is sorted once per flow, enforced by the shared-AST sorting guard.
  Detailed member reports are computed only for check flows with declaration changes, preserving minimal relocation
  diagnostics while skipping their cost for reordering, annotation-only changes, and unchanged declarations.
- Sorting results retain independent `annotationsReordered` and `membersReordered` flags for source annotation order
  and Spoon declaration order, including top-level types. `isReordered()` derives their OR without storing another flag
  or traversing source models. Flows use it for sorting checks and status; annotation-only changes leave
  `membersReordered` false and retain annotation diffs. Constructor arguments follow field declaration order.
- Added member-relocation diagnostics to `SortingResult` using unmodifiable views without defensive copies. Producers
  must not mutate handed-off lists. Check flows consume these diagnostics, and sorting timing includes their detection.
  Kept the shared `MemberRelocation` model, its diagnostic producer, and the package-private LIS helper in `core.spoon`
  so the model constructor remains package-private.
- Removed printer settings from parsing and `SpoonAstModel`. Flows now pass `PrinterConfig` directly to serialization,
  allowing the same parsed model to be printed with different settings.
- Updated `SpoonSorter` to accept and return the complete processing model, keeping declaration sorting, opt-outs, and
  annotation groups together at the sorting boundary. Updated direct callers and benchmark model copying.
- Documented the annotation scanner's lexical boundary after verifying Spoon 11.5.0 traversal gaps for array dimensions
  and receiver parameters. Added parser coverage for their exact source-group ranges without changing sorting behavior.
- Centralized annotation scanner whitespace and line-boundary operations in `SrcCodeUtils`, preserving source offsets,
  comment whitespace, and CR/LF/CRLF handling.
- Moved shared annotation source models into standalone `AnnotationSrcGroup`, with nested annotation, gap, and
  base-fragment types. `AnnotationSourceScanner` retains source parsing and fragment preparation.
- Made the annotation group's common fragment base, mixed-sequence getter, and fixed gap-content getter private.
  Typed annotation and gap lists remain available to sorting; printing consumes the prepared replacement code.
- Annotation and gap fragments share source text and original ranges through `SrcFragment`. Sorting determines
  annotation order and gap text. Groups retain unmodifiable views of both typed lists, require one gap per annotation,
  and lazily interleave the mixed sequence through a static helper. Callers must not mutate handed-off lists.
  `annotationSrcFragments` and internal `srcFragments` retain the group's current order. Each group lazily
  caches its mixed sequence and `replacementCode`; independent group builder inputs are removed. Printing replaces
  the original range with that block without inspecting fragment types or reconstructing annotation order.
- Annotation gaps retain one ready-to-emit code string. The scanner prepares fixed content, whitespace-prefix choices,
  and separator counts as immutable values. Sorting selects a prefix without reading or analyzing source
  text, preserving repeated sorting, exact unchanged gaps, and comment attachment.
- Merged annotation-group replacement into `SrcPrinterOutput`; declaration fragments and units without declared types
  share the same source-range copying method.
- Removed redundant collection copies and wrappers in dependency ordering and CLI process test results.
- Annotation ordering models retain unmodifiable views of criteria without copying; callers must not mutate supplied
  lists.
- Annotation fragments keep sorting keys, prepared text, the annotation offset, and required trailing separators.
  Gap preparation uses a computed blank-line requirement instead of interpreting a separator-count value.
  Parsing-only comment and token offsets stay private in the scanner.
- Annotation descriptors retain read-only preservation-key views. Annotation fragment builders initialize inherited
  ranges.
- Annotation order is computed only from source-fragment groups. Removed the duplicate AST scan, descriptor index,
  and annotation-list mutations; source printing uses the single sorted order.
- Annotation fragments and their sorted order now belong to `SpoonAstModel`, passed explicitly between parser, sorter,
  and printer. Removed annotation metadata from `CtCompilationUnit` and the serialization supplier from the model.
- Replaced the inherited Spoon source printer with standalone source-fragment printing, a direct output buffer, and
  indexed member boundaries. Preserved declaration output and skipped-type ranges; removed enum correction through Spoon
  pretty printing.
- On the four-workload JDK 21 corpus, mean printer serialization throughput reached 1.40–5.07× baseline across 1/2/4/8
  workers; sampled p50/p95/p99 decreased for every workload. Allocated bytes per batch decreased by 6.7–66.2% across the
  measured cases.
- Selected printer code decreased from 1000 to 701 physical lines and from 276 to 210 NCSS; summed Cyclo decreased from
  106 to 81, Cognitive from 87 to 59, and external library target types from 21 to 11.
- The source printer now copies the original tail after the last top-level type following one blank line. Preserves
  comment spelling, indentation, semicolons, trailing whitespace and the original file ending; removes separate footer
  rendering.
- Updated Palantir Java Formatter dependency from 2.91.0 to 2.98.0.
- Updated Spoon dependency from 11.2.1 to 11.5.0.

### Fixed

- Annotation sorting preserves repeated visible type identities without loading library annotation classes. Explicit
  containers contribute nested identities; same-file repeatable declarations add dependencies for defaulted containers.
  Unresolved names and ambiguous inherited owner aliases add conservative alternative group keys without warnings;
  independent annotations in the same source group still sort, and confirmed distinct homonyms keep separate identities.
  Hidden container defaults without a source-declared repeatable relationship remain a documented limitation;
  sorting opt-outs preserve nested annotations.
- Removed automatic intermediate-source snapshots from production flows. TRACE logging no longer creates `debug/`
  files or lets snapshot I/O failures interrupt checking or rewriting sources.
- Standalone module builds resolve shared license, PMD, and SpotBugs resources from the repository root through the
  tracked `.mvn` directory.
- Relocation detection includes source-fragment annotation order, including annotations absent from Spoon's AST.
  Check flows display annotation ordering diffs using the standard formatting-violation message. All flows classify
  annotation-only changes as reordering.
- Annotation scanning hands off independent lists for each group and starts fresh lists for the next group, preserving
  annotations for sorting and relocation detection without collection copies.
- Source printing includes annotations preceding the member declaration start reported by Spoon when a comment appears
  between annotations, preserving them during repeated processing of top-level types, nested types, and type members.
- Annotation sorting moves a standalone comment block with the preceding annotation when they are adjacent and a blank
  line separates the block from the next element. Moves that blank line with the block without leaving an extra blank
  line between annotations; separators around independent comment blocks remain in place.
- Annotation sorting moves adjacent leading line and block comments with their annotation. Blank lines separate comment
  blocks; declaration JavaDoc and file preambles retain their positions.
- `ARGUMENTS_ALPHA` distinguishes annotations without parentheses from empty argument lists, ordering `@Tag` before
  `@Tag()` and `@Tag("")`.
- Annotation sorting moves trailing same-line comments with their annotation and retains a line break after `//`
  comments when moving them into an inline annotation group.
- `DECLARATION_LENGTH_ASC` includes the written qualified annotation name and internal comments, alongside parameter
  names and values. Inter-token whitespace and comments between annotations remain excluded.
- Kept leading type comments and JavaDoc adjacent to their declarations in files without package/import declarations,
  including when top-level types are reordered.
- YAML loading rejects null collection entries, including annotation criteria, member groups, subgroups, and type
  groups, through the loader's exception contract. Nullable optional properties remain supported.
- Preserved footer comments after the last top-level type with trailing line terminators, virtual sources, and repeated
  processing, including reordered top-level declarations.
- Fixed duplicate blank lines in Spoon serialization before closing braces, between nested and top-level types, and at
  end of file. Declaration containers now own separators, combining header, member, and group spacing while preserving
  comments, opt-out fragments, and source line endings. Member source boundaries are indexed once, skipped-type offsets
  no longer copy the output buffer, and fragment indentation is retained without rescanning leading whitespace.
  Fragment-boundary detection is centralized in `SrcCodeUtils` with unit coverage for indentation, whitespace, and
  source ranges.
- Generated member-group comments now retain the following member's source indentation before formatting.

## [1.0.1] — 2026-05-07

### Added

- Core pipeline: parse → classify → sort → print → format.
- Declaration-order dependency graph for direct initializer/read-write scenarios.
- Accessor bundling (`keepAccessorsTogether` option).
- Cycle detection with relaxed forward-reference fallback.
- Comment-based opt-out directives: `@jharmonizer:fully-off`, `@jharmonizer:sort-off` at file and type scope.
- Configurable member grouping and ordering via YAML/JSON configuration DSL.
- Maven plugin (`jharmonizer-maven-plugin`) with `reorder`, `check`, and `check-fast` goals.
- CLI fat JAR with `reorder`, `check-all`, and `check-fast` commands.
- JMH sorting performance benchmark (activate with `-Pbenchmark-sort`).
- Support for Maven archetype template files (graceful skip on non-Java placeholders).
- `CONTRIBUTING.md` — contributor guide covering fork/clone workflow, environment setup, build commands, code style, and
  review process.
- `SECURITY.md` — vulnerability disclosure policy.
- `CHANGELOG.md` — this file.
- `docs/directives.md` — full opt-out directives reference.
- `docs/known-limitations.md` — known formatter edge cases and workarounds.
- `docs/benchmark.md` — JMH sorting benchmark usage.
- GitHub Actions Verify workflow (`.github/workflows/verify.yml`) that builds and tests every push and pull request.

[Unreleased]: https://github.com/lemon-ant/JHarmonizer/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/lemon-ant/JHarmonizer/releases/tag/v1.0.1
