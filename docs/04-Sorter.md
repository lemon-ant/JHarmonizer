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
AST annotation lists retain their parsed order: printing consumes the sorted source groups directly, so a second AST
scan and annotation sort would not affect output. Singleton groups and groups in excluded types retain their original
order.
The sorter passes sorted annotations and resolved gap slots as two required lists to the group constructor.
Resolving a gap only selects a scanner-prepared whitespace prefix, comparing source boundaries and separator counts.
It does not read source text, inspect characters, or parse comments. Each gap combines that prefix with its fixed
content; its shared immutable layout supports subsequent sorts without storing complete previous code versions. The
constructor retains unmodifiable views and requires one gap per annotation. Producers must not modify the supplied
lists after handoff. The private mixed sequence is lazily built and cached by a static helper that alternates the lists;
sorting reads only the typed lists. Clients cannot access or independently replace the derived sequence. The group lazily
concatenates it into `replacementCode` on first access; every newly constructed group has its own caches.
The printer only replaces the original group range with that complete block. Group boundaries and
gap positions remain fixed when the sorter replaces the immutable annotation order. Blank lines determine whether
a standalone block belongs to the preceding or following annotation. Separate blocks and their surrounding gaps stay in
place; declaration JavaDoc and file preambles retain their positions. The sorter preserves line-comment terminators and
moves blank lines with the blocks they attach to an annotation, removing redundant blank lines from the original
position. Complete ties retain source order. See
[`annotations-ordering`](config-dsl.md#annotations-ordering) for criteria, defaults, and comment attachment rules.

## Input / output

Input: a `SpoonAstModel` (see [`03-Parser.md`](03-Parser.md)) plus a `CompiledConfig`
(see [`02-Configurator.md`](02-Configurator.md)).

Output: `SortingResult` contains a model wrapper with the new immutable annotation order, prepared member-relocation
diagnostics, the `annotationsReordered` and combined `membersReordered` flags, and timing statistics. The underlying
Spoon AST is shared with the input wrapper; declaration lists are reordered in place, while annotation lists remain
unchanged.
Serialization consumes the returned model to print declaration order from the AST and annotation order from the groups.

`Sorter` computes the existing LIS relocation report before returning. The result and chunk lists use unmodifiable views
without defensive copies. Producers must not mutate handed-off lists. Each invocation uses fresh result and scope lists,
so sorting the same AST again leaves earlier reports intact. The referenced Spoon nodes remain mutable.
Check flows consume these prepared diagnostics and `annotationsReordered` to report sorting violations and annotation
diffs. `Sorter` computes annotation changes once and reuses that value for the combined `membersReordered` flag,
checking declaration order only when annotations have not moved. `ReorderFlow` uses the combined flag for status
reporting. Member-only changes set the combined flag while leaving `annotationsReordered` false, so check flows emit
member diagnostics without an annotation diff. One timed block covers sorting and detection. A file-level sorting
opt-out returns empty diagnostics, `false` change flags, and zero sorting time.

## Implementation map

The Spoon-backed sorter lives in
`io.github.lemon_ant.jharmonizer.core.sorter.spoon`:

| Class                                | Role                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `Sorter` / `SortingResult`           | Public facade and per-file result with the model, relocation diagnostics, annotation and combined change flags, and timing. |
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
4. **Repair against the dependency graph**: the per-group ordering is passed to
   `SimplifiedDependencyAwareSorter`, which performs the dependency-aware
   provider-lift repair pass described in
   [`sorting-algorythm.md`](sorting-algorythm.md).
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

The full ordering algorithm — accessor clustering, the comparator chain, and the
dependency-graph repair pass — is documented in
[`sorting-algorythm.md`](sorting-algorythm.md). The post-sort *relocation
detector* (which produces the human-friendly "move *N* members before *X*" report)
is a separate diagnostic pass and is documented in its own file:
[`relocation-detector.md`](relocation-detector.md).
