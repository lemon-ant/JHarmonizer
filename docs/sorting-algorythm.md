<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer sorting algorithm

This document describes the algorithm used to order members inside a single member
group, including the key derivation, comparator chain, dependency-graph repair pass,
and the post-sorting relocation reporter.

## Inputs

For one leaf member group inside one type, the sorter receives:

- `compiledMemberGroup` — the leaf node of the compiled member-group tree, carrying
  the inherited `ordering-rules`, `keepAccessorsTogether`, and `relaxedForwardReferences`
  settings.
- `groupMembers` — the `CtTypeMember`s that the dispatcher routed into this leaf
  group, in original source order.
- `memberDependencyGraph` — the per-type dependency graph built once by
  `MemberDependencyGraphBuilder` (see [`declaration-order-dependencies.md`](declaration-order-dependencies.md)).

## Output

A list of `CtTypeMember`s in the final declaration order. The result is wrapped in a
`MemberGroupBlock` and concatenated with the other groups into the type body.

## Step 1 — Derive ordering keys

`OrderingKeyFactory.createSortableMembers(...)` derives an `OrderingKey` per member
from `SpoonTypeMemberUtils`:

| Field on `OrderingKey`        | Source                                                            |
|-------------------------------|-------------------------------------------------------------------|
| `srcStart`                    | Spoon `SourcePosition` of the member declaration.                 |
| `alphaKey`                    | Member name plus signature for methods/constructors (so overloads sort by parameter list). |
| `alphaSortingRank`            | Per-kind grouping rank used as a secondary alpha tie-breaker.     |
| `visibilityRank`              | `public=0`, `protected=1`, `package=2`, `private=3`.              |

For accessor co-location (when `keepAccessorsTogether` is on), the factory also
computes two representative keys per member:

- **Property cluster representative** — shared by the getter and setter for the same
  JavaBean property.
- **Super-cluster representative** — shared by all accessors in the group when at
  least `MIN_ACCESSORS_FOR_SUPER_CLUSTER = 2` accessors are present.

Members that do not belong to any cluster carry self-references for both
representatives.

## Step 2 — Build the comparator

`ComparatorUtils.buildSortableTypeMemberComparator(orderingRules)` returns a
`Comparator<SortableTypeMember>` that:

1. Compares **super-cluster representatives** first (reference equality short-circuit
   inside the same cluster).
2. Then compares **property-cluster representatives**.
3. Then compares each member's **own `OrderingKey`**.

For each level the underlying `OrderingKey` comparator is built from the inherited
`ordering-rules`. The four rules map to pre-computed comparator constants:

| `OrderingRule`     | Comparator                                                        |
|--------------------|-------------------------------------------------------------------|
| `PRESERVE`         | `compareInt(srcStart)`                                            |
| `ALPHA`            | `compareInt(alphaSortingRank).thenComparing(alphaKey)`            |
| `VISIBILITY_DESC`  | `compareInt(visibilityRank)` (public first because public=0)      |
| `VISIBILITY_ASC`   | `compareInt(visibilityRank).reversed()` (private first)           |

A deterministic tie-breaker chain (`PRESERVE` then `ALPHA`) is appended to the
configured rules so two equal configurations always produce identical output. When no
rules are configured, the default `PRESERVE → ALPHA` chain is used directly.

## Step 3 — Provider-lift repair against the dependency graph

The base order produced by sorting `SortableTypeMember`s with the comparator above is
fed into `SimplifiedDependencyAwareSorter` together with:

- accessor-pair bundles modeled as `Groups`, and
- `DECLARATION_DEPENDENCY` edges from `MemberDependencyGraph`, modeled as `Dependencies`.

The sorter implements a **provider-lift** repair policy:

1. The base order (from Step 2) is treated as the desired sequence.
2. The base order is scanned left to right.
3. When the earliest blocked dependent is encountered (its required providers have
   not yet been emitted), the **minimal transitive provider closure** is moved as a
   single contiguous block directly before the blocked element.
4. Lifted providers are topologically sorted among themselves with base-rank
   tie-breaking.
5. Repeat until the order is dependency-valid.

Properties:

- The base order remains the primary intent; only members forced by hard dependencies
  are repositioned.
- Lifted providers stay contiguous and sit immediately above the dependent that
  required them (no smearing).
- Unaffected members preserve their relative order.
- The output is fully deterministic.

This is the only repair strategy: there is no alternative branching, no scoring
between candidates, and no global topological re-shuffle.

The mutual-exclusion precondition required by the simplified sorter — accessor-pair
groups never overlap with dependency-graph nodes — is enforced when the Groups and
Dependencies are constructed in `GroupMembersOrderer`.

## Step 4 — Emit the ordered group

The `CtTypeMember`s are collected from the sorted `SortableTypeMember`s and wrapped
back into a `MemberGroupBlock`. Separator directives propagate to the printer.

## Top-level types

`SpoonSorter` reuses the same comparator machinery for top-level types of a
compilation unit, but with the simpler `ComparatorUtils.buildOrderingKeyComparator(...)`
(member-only) chain — top-level types do not participate in accessor clustering or in
the per-type declaration-order dependency graph.

## Post-sorting relocation reporter

After sorting completes, `RelocationDetector` produces a human-friendly relocation
report consumed by `MemberRelocationPrinter`. The reporter is a pure diagnostic
pass — it never influences the produced output. Its full algorithm (the LIS-based
minimal-moved-set computation, scope handling, and chunk gluing) is documented
separately in [`relocation-detector.md`](relocation-detector.md).

## Determinism

Every step of the pipeline is deterministic:

- Comparator chains terminate in stable tie-breakers.
- The provider-lift repair always picks the earliest blocked dependent and its minimal
  closure.
- The relocation report is derived from a deterministic patience-sort LIS (see
  [`relocation-detector.md`](relocation-detector.md)).

Two runs over identical inputs produce byte-identical AST orderings and identical
relocation reports.
