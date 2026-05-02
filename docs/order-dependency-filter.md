<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Dependency-graph filtering

The per-type dependency graph (see [`declaration-order-dependencies.md`](declaration-order-dependencies.md))
records two distinct kinds of edges: real declaration-order constraints and
"keep-together" hints for accessor pairs. Filtering decides which of those edges a
given consumer is allowed to see.

JHarmonizer does not use a dedicated Spoon `Filter<CtElement>` for this. The filtering
is folded into the graph's query API.

## Edge kinds

`MemberDependencyEdgeKind` is the only filter axis:

| Kind                       | Meaning                                                                           | Honoured by                                       |
|----------------------------|-----------------------------------------------------------------------------------|---------------------------------------------------|
| `DECLARATION_DEPENDENCY`   | Real Java declaration-order constraint (provider must appear before dependent).   | `SimplifiedDependencyAwareSorter` (provider lift).|
| `ACCESSOR_BUNDLE`          | "Keep adjacent" hint for getter/setter pairs of the same JavaBean property.       | Group bundling in `GroupMembersOrderer` (sets of `Groups`). |

`ACCESSOR_BUNDLE` edges are intentionally **not** declaration-order constraints and
are filtered out before the order is repaired.

## Where filtering happens

`MemberDependencyGraph` exposes its query API with an `allowedEdgeKinds`
`Set<MemberDependencyEdgeKind>` parameter. Internally:

- The set is normalized to a bitmask (`ALL_EDGE_KIND_MASK` short-circuits the
  unfiltered case).
- Edges are stored flat as neighbor + kind values; the kind check is applied at
  query time, not at storage time.
- Transitive queries are cached per `(start member, edge-kind mask)` pair.

The two callers in the sorter pass disjoint masks:

- `GroupMembersOrderer` queries the graph with `EnumSet.of(DECLARATION_DEPENDENCY)`
  to feed the provider-lift repair pass with real constraints only.
- The same caller separately consumes `ACCESSOR_BUNDLE` adjacency to build the
  `Groups` argument used for accessor co-location.

## What the providers themselves filter out

The filtering applied by individual providers (before any edge reaches the graph) is:

- **Cross-file references** are never recorded. `CrossTypeConstantBackRefDependencyProvider`
  explicitly limits itself to type pairs in the same compilation unit; cross-file
  cycles are treated as an application-design problem.
- **Method and constructor bodies** never contribute declaration-order edges, matching
  Java's actual rules.
- **Self-loops** are not produced (a member never depends on itself).
- **Already-satisfied edges from blank-final assignments** are deduplicated:
  `InitializerBlockMutableFieldReadDependencyProvider` skips edges that
  `BlankFinalDefiniteAssignmentDependencyProvider` already covers.
- **Accessor-bundle edges** are only emitted when `keepAccessorsTogether: true` is in
  effect for the containing group; with the flag off, `AccessorPairDependencyProvider`
  contributes nothing.

## Cycle handling

`MemberDependencyGraphBuilder` operates in two passes when at least one group is
configured with `relaxedForwardReferences: false`:

1. Build the strict graph honoring the per-group `relaxedForwardReferences` setting.
2. If the resulting graph contains a cycle on `DECLARATION_DEPENDENCY` edges, retry
   with all groups forced to relaxed mode.
3. If the relaxed graph is also cyclic, throw `SortingException`.

This lets users opt into stricter forward-reference checks without making cyclic
real-world code unsortable.
