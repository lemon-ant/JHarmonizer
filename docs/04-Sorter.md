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

## Input / output

Input: a `SpoonAstModel` (see [`03-Parser.md`](03-Parser.md)) plus a `CompiledConfig`
(see [`02-Configurator.md`](02-Configurator.md)).

Output: the AST with members reordered in place. The serializer (Spoon custom printer)
later writes the AST back to text in the new order.

## Implementation map

The Spoon-backed sorter lives in
`io.github.lemon_ant.jharmonizer.core.sorter.spoon`:

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `Sorter` / `SortingResult`           | Public sorter facade and per-type sorting result.                                                     |
| `SpoonSorter`                        | Top-level driver: walks types, dispatches members into compiled groups, emits sorted output.          |
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
4. **Repair against the dependency graph**: relocations that would violate a
   declaration-order arc are rejected; the affected members fall back to source
   order (full algorithm in [`sorting-algorythm.md`](sorting-algorythm.md)).
5. **Render**: ordered groups are emitted as `MemberGroupBlock`s with separator
   directives (`new-line`, `header`, `none`) propagated to the printer.

## Top-level types

Top-level types of a compilation unit are sorted independently by `SpoonSorter` using
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

The full ordering algorithm — including accessor clustering, the LIS-based relocation
detector, and the dependency-graph repair pass — is documented in
[`sorting-algorythm.md`](sorting-algorythm.md).
