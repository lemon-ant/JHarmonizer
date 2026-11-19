<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Sorter

## Purpose

Reorder all members inside every type of a parsed Java file so that the resulting
declaration order matches the active configuration (the compiled member-group tree),
while honouring the declaration-order dependency graph and accessor co-location.

## What gets sorted

The sorter handles every Spoon `CtTypeMember` kind:

- fields,
- enum constants,
- record components,
- static and instance initializer blocks,
- constructors,
- methods,
- nested types (classes, interfaces, enums, records, annotations).

Sorting is recursive: nested types are processed with the same configuration as the
enclosing type.

Global `annotations-ordering` also reorders source annotation groups on packages, modules, types, and their descendants.
The configuration compiler builds the comparator once. During parsing, `AnnotationSourceScanner` captures source
fragments and computes their keys, storing the immutable groups in `SpoonAstModel`. `SpoonAnnotationSorter` sorts each
eligible source group once with that comparator, including annotations missing from Spoon's model.
AST annotation lists retain their parsed order: printing consumes the selected lexical order, so a second AST
scan and annotation sort would not affect output. Singleton groups and groups in excluded types retain their original
order.
The sorter returns `sortedAnnotationSrcFragmentsByGroupSrcStart`, a read-only map containing only changed orders of
existing annotation fragments, keyed by original group start. It does not prepare gap text or construct new source
groups.
The scanner prepares immutable gap layouts for separator requirements zero, one, and two. Rendering selects those
ready layouts, joins their whitespace and fixed content, and retains an original annotation/gap pair verbatim.
Source groups retain their original typed lists and lazily cache only original `replacementCode`. Callers must not
modify handed-off lists. The renderer receives a selected order without changing those source models; the printer
replaces the original group range with its joined output.
Group boundaries and gap positions remain fixed when annotation order changes. Blank lines determine whether
a standalone block belongs to the preceding or following annotation. Separate blocks and their surrounding gaps stay in
place; declaration JavaDoc and file preambles retain their positions. Scanner-prepared layouts preserve line-comment
terminators and move blank lines with attached comment blocks while removing redundant blank lines from the original
position. Complete ties retain source order. See
[`annotations-ordering`](config-dsl.md#annotations-ordering) for criteria, defaults, and comment attachment rules.

## Input / output

Input: a `SpoonAstModel` (see [`03-Parser.md`](03-Parser.md)) plus a `CompiledConfig`
(see [`02-Configurator.md`](02-Configurator.md)).

Output: `SortingResult` contains `sortedSpoonAstModel`, the sparse `sortedAnnotationSrcFragmentsByGroupSrcStart` map,
prepared member-relocation diagnostics, independent `annotationsReordered` and `membersReordered` flags, and timing
statistics. The returned model is the supplied context after in-place AST sorting, not a copy or snapshot. No printing
context is constructed by sorting. The member flag covers Spoon declaration order, including top-level types; the
annotation flag covers source annotation fragments.
`isAnnotationsReordered()` derives its value from map non-emptiness; `isReordered()` derives the flags' OR without
storing another flag. Declaration lists are reordered in place, while AST annotation lists remain unchanged.
The flow reads the returned model and map from `SortingResult` and combines them in `SpoonPrintingInput` before
serialization. That input shares existing AST, source, and group references; groups missing from the order map retain
their original order and code. The sorter knows neither the printer nor its input model. `SpoonAstModel` keeps parsed
context and no post-sort annotation-order field.

Each freshly parsed shared AST is sorted once per flow. The nested `SpoonSortingResult` carries
`sortedSpoonAstModel`, the selected orders, and the declaration-change flag. The facade reads that returned model for
relocation diagnostics and forwards it with the same order map into public `SortingResult`, adding diagnostics and
timing. Both result layers retain the same model instance and shared AST.
Final declaration and annotation order is collected together with identity-based change flags,
after grouping and dependency repair. Annotation source groups retain their source-range order; only annotation order
inside a group can change. The sparse order map contains only changed groups, so its non-emptiness supplies the
aggregate annotation-change flag without comparing group permutations or preparing replacement groups.
No separate AST traversal is needed to decide whether sorting changed order.

`SpoonSorter` atomically claims the shared `CtCompilationUnit` model through metadata before any sorting mutation.
A repeated attempt through the input model, a pre-existing view, or either sorting entry point throws
`IllegalStateException`, including after unchanged or failed first attempts.
[Spoon's clone implementation](https://github.com/INRIA/spoon/blob/v11.5.0/src/main/java/spoon/support/visitor/clone/CloneBuilder.java#L60)
copies this metadata, so a clone of a consumed model remains consumed. Reparsing source starts a new sorting lifecycle.

The monitor protects only the short metadata claim; sorting runs outside it. The helper's local PMD suppression follows
the [JDK 21 guidance](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html) for short in-memory operations.

Check flows request the LIS member report when declarations changed and consume `annotationsReordered` for annotation
diffs. `ReorderFlow` requests only change flags and skips the diagnostic pass. Annotation-only changes and unchanged
declarations also skip member diagnostics. Report and chunk lists use unmodifiable views without defensive copies;
producers must not mutate handed-off lists. One timed block covers sorting and requested diagnostics. A file-level
sorting opt-out returns the unchanged supplied model, empty orders and diagnostics, `false` change flags, and zero
sorting time.

`checkSortingThenFormattingIfOrdered` checks sorting first and skips formatting if declaration or annotation order
changed. Flows use `isReordered()` for this decision and for status; `membersReordered` alone excludes annotation
changes. Annotation violations retain the `REORDERED` status and use the existing formatting-violation diff renderer.

## Implementation map

The Spoon-backed sorter lives in
`io.github.lemon_ant.jharmonizer.core.sorter.spoon`:

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `Sorter` / `SortingResult`           | Public facade and per-file result with the supplied post-sort model, selected annotation orders, relocation diagnostics, independent annotation and member flags, their computed aggregate, and timing. |
| `SpoonSorter` / `SpoonSorter.SpoonSortingResult` | Walk types, sort declarations and annotations, and return the post-sort model, selected orders, and declaration-change flag. |
| `OrderChangeCollector` | Collects final permutations and detects identity changes during the same traversal. |
| `OrderChangeCollector.ElementOrdering` | Final declaration or annotation-fragment sequence with its identity-change flag. |
| `TypeMemberGrouper`                  | Dispatches each member to its leaf member group via the compiled selector predicates.                 |
| `NaturalMemberGroupResolver` / `EffectiveMemberGroupResolver` | Resolve which compiled group claims a given member, with first-match-wins semantics.    |
| `GroupMembersOrderer`                | Orders members inside a single leaf group; computes accessor super-clusters and property clusters.    |
| `OrderingKeyFactory`                 | Builds `OrderingKey` / `ClusteredOrderingKey` instances used by the comparators.                      |
| `ComparatorUtils`                    | Pre-computed comparator constants for `preserve` / `alpha` / `visibility-asc` / `visibility-desc` plus tie-breakers. |
| `SortableTypeMember`                 | Lightweight data holder that pairs a `CtTypeMember` with its ordering keys.                           |
| `MemberGroupBlock` / `GroupBoundaryMarker` | Output blocks and inter-group boundary markers used during serialization.                       |
| `SpoonMemberDescriptorFactory`       | Adapts Spoon `CtTypeMember` instances into `MemberDescriptor`s consumed by compiled selectors.        |
| `SpoonTypeMemberUtils`               | Visibility ranking and other shared per-member utilities.                                             |
| `dependency_graph/`                  | Declaration-order dependency providers and graph builder. See [`declaration-order-dependencies.md`](declaration-order-dependencies.md). |

## High-level algorithm

For each type processed (top-level and nested):

1. **Dispatch**: every member is routed to exactly one leaf group of the compiled
   member-group tree by `TypeMemberGrouper`. Dispatch uses first-match-wins over the
   DFS post-order of the compiled tree.
2. **Build the dependency graph**: `MemberDependencyGraphBuilder` runs every
   `*DependencyProvider` over the type and produces a `MemberDependencyGraph` of
   declaration-order arcs. See [`declaration-order-dependencies.md`](declaration-order-dependencies.md).
3. **Order each leaf group**: `GroupMembersOrderer` builds `SortableTypeMember`
   instances, computes accessor super-clusters and property clusters via
   `OrderingKeyFactory`, and applies the comparator chain produced by
   `ComparatorUtils.buildClusteredOrderingComparator(...)`. The comparator chain is
   driven by the inherited `ordering-rules` of the leaf group.
4. **Repair against the dependency graph**: the per-group ordering is passed to
   `SimplifiedDependencyAwareSorter`, which performs the dependency-aware
   provider-lift repair pass described in
   [`sorting-algorythm.md`](sorting-algorythm.md).
5. **Render**: ordered groups are emitted as `MemberGroupBlock`s with separator
   directives (`new-line`, `header`, `none`) propagated to the printer.

## Top-level types

Top-level types of a source file are sorted independently by `SpoonSorter` using
`UnifiedTopLevelTypesOrdering` (see [`config-dsl.md`](config-dsl.md#top-level-types-ordering)),
with the same `OrderingKey` machinery but a member-only comparator
(`buildMemberOnlyOrderingComparator`).

## Determinism

Sorting is fully deterministic: every comparator falls back through a stable chain
(rule key → `srcStart` → alpha key → visibility), so two runs over the same input
produce byte-identical AST orderings.

## Opt-out interaction

Members of types marked `@jharmonizer:fully-off` are not sorted at all (the original
source range is reproduced verbatim by the printer). Members of types marked
`@jharmonizer:sort-off` skip the dispatch / ordering step but are still re-emitted by
the formatter. See [`docs/directives.md`](directives.md).

## Algorithmic deep-dive

The full ordering algorithm — accessor clustering, the comparator chain, and the
dependency-graph repair pass — is documented in
[`sorting-algorythm.md`](sorting-algorythm.md). The post-sort *relocation
detector* (which produces the human-friendly "move *N* members before *X*" report)
is a separate diagnostic pass and is documented in its own file:
[`relocation-detector.md`](relocation-detector.md).
