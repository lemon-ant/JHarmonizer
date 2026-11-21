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

## Annotation groups

`SpoonAnnotationSorter` sorts eligible groups of adjacent source annotations with the comparator compiled from
`annotations-ordering`. Shared preservation keys keep dependent fragments in relative source order. Only the order
inside a group can change. Singleton groups and groups covered by sort opt-outs retain their source order.
See [configuration](config-dsl.md#annotations-ordering) and [annotation order preservation](annotation-order-preservation.md).

## Input and output

The sorter takes a parsed `SpoonAstModel` and a `CompiledConfig`. It collects final order and detects changes after
grouping and dependency repair. `SortingResult` contains the supplied model with reordered Spoon declarations,
requested member diagnostics, timing, and a read-only map of changed annotation orders keyed by original group start.
The flow uses the model and this map to [print the source](source-printer.md). Groups without an entry print in source order.

`membersReordered` covers Spoon declaration order, including top-level types. `isAnnotationsReordered()` checks
whether the annotation order map contains entries. `isReordered()` is true when either kind of order changed.

`SpoonSorter` claims the shared `CtCompilationUnit` through metadata before the first sort attempt. A second attempt
throws `IllegalStateException`, including after an unchanged or failed sort. Model views and clones share or carry
the claim. Reparsing creates a fresh model.

Check flows request relocation diagnostics when declarations change and use a source diff for annotation changes.
`ReorderFlow` uses change flags. Sorting-first checks run formatting only when both kinds of order are unchanged.
A file-level sort opt-out returns the supplied model with empty orders and diagnostics, false change flags, and zero
sorting time.

## Implementation map

The Spoon-backed sorter lives in
`io.github.lemon_ant.jharmonizer.core.sorter.spoon`:

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `Sorter` / `SortingResult`           | Public facade and per-file result with the supplied post-sort model, selected annotation orders, relocation diagnostics, independent annotation and member flags, their computed aggregate, and timing. |
| `SpoonSorter` / `SpoonSorter.SpoonSortingResult` | Walk types, sort declarations and annotations, and return the post-sort model, selected orders, and declaration-change flag. |
| `SpoonAnnotationSorter` | Sorts adjacent source annotations and collects changed orders. |
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
