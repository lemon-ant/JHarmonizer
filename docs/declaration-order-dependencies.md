<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Declaration-order dependencies

JHarmonizer must not produce a reordering that breaks Java's declaration-order rules.
This document is the catalog of order-sensitive Java constructs the sorter is aware of,
and the mapping from each rule to the provider class that contributes the corresponding
edges to the per-type member dependency graph.

The graph is built by `MemberDependencyGraphBuilder` from the providers in
`io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph` and consumed by
the sorter (see [`sorting-algorythm.md`](sorting-algorythm.md)).

## Order-sensitive constructs

| # | Java construct                                  | Example                                       | Order requirement |
|---|-------------------------------------------------|-----------------------------------------------|-------------------|
| 1 | Field initializer reading a same-class field    | `int a = b;`                                  | provider above    |
| 2 | Static field initializer reading a same-class static field | `static int x = y;`                | provider above    |
| 3 | Instance/static initializer block reading a same-class field | `{ ... = field; }`                | provider above    |
| 4 | Enum constant initializer reading prior enum constants / static fields | `MEDIUM(SMALL.code+1)` | provider above |
| 5 | Blank-final read after its definite assignment   | `final int x; { x = 1; } int y = x;`         | assignment above  |
| 6 | Cross-type constant chain back-referencing the declaring type | `T.F → E1.SF → … → T.G`         | provider above    |
| 7 | JavaBean accessor pair (when `keepAccessorsTogether: true`) | getter and setter for the same property | adjacent (not order) |

Method bodies, constructor bodies, inner-class member references, and references that
cross types in different files are **not** treated as declaration-order dependencies —
Java does not require them, so the graph leaves them free.

## Edge kinds

`MemberDependencyEdgeKind` distinguishes:

- `DECLARATION_DEPENDENCY` — a real declaration-order constraint. Honoured by the
  provider-lift repair pass in `SimplifiedDependencyAwareSorter`.
- `ACCESSOR_BUNDLE` — a "keep adjacent" hint emitted by `AccessorPairDependencyProvider`
  when accessor co-location is enabled. Ignored by ordering and consumed only by the
  group-bundling logic in `GroupMembersOrderer`.

All edges are directed `provider → dependent`. Storage is flat (neighbor + kind);
filtering by kind happens at query time. Transitive queries are cached per
`(start member, edge-kind mask)` pair.

## Providers

Each provider implements `MemberDependencyProvider`. The graph builder iterates
through them and accumulates the edges. Two abstract bases factor common logic; eleven
concrete providers contribute edges.

### Abstract bases

| Class                                                            | Role |
|------------------------------------------------------------------|------|
| `AbstractReferencedFieldsDeclarationDependencyProvider`          | Common scaffolding for initializer-like members that emit `DECLARATION_DEPENDENCY` edges based on order-dependent field references. |
| `AbstractExplicitInitializerForwardReferenceDependencyProvider`  | Common scaffolding for forward-reference detection between fields with explicit (non-default-value) initializers. |

### Concrete providers

| Class                                                              | Catalog rule(s) | Notes |
|--------------------------------------------------------------------|-----------------|-------|
| `FieldInitializerBackwardReferenceDependencyProvider`              | 1, 2            | Regular field initializer references resolved by `DeclaringTypeFieldReferenceUtils`. |
| `ExplicitThisInitializerFieldDependencyProvider`                   | 1               | `this.<field>` references in field initializers (forward-reference handling). |
| `ExplicitDeclaringTypeInitializerFieldDependencyProvider`          | 2               | `<DeclaringType>.<field>` references in static field initializers. |
| `InitializerBlockDependencyProvider`                               | 3               | Static and instance initializer blocks: if a block reads `fieldA`, `fieldA → block`. |
| `InitializerBlockMutableFieldReadDependencyProvider`               | 3               | Conservative edge for mutable fields whose contents may have been mutated by a prior initializer block via method calls (e.g. `map.put(...)`). |
| `EnumConstantInitializerDependencyProvider`                        | 4               | Enum constants are static fields initialized in source order; their initializers cannot be reordered past their providers. |
| `BlankFinalDefiniteAssignmentDependencyProvider`                   | 5               | If a dependent initialization member reads a blank final field, edges are added from all potential assignment providers declared above it. |
| `CrossTypeConstantBackRefDependencyProvider`                       | 6               | Detects transitive cross-type initializer chains within the same compilation unit (`T.F → E1.SF1 → … → T.G`). Cross-file pairs are intentionally out of scope. |
| `AccessorPairDependencyProvider`                                   | 7               | Emits `ACCESSOR_BUNDLE` edges (not declaration-order). Driven by `keepAccessorsTogether` and `SpoonJavaBeansAccessorUtils`. |

`MemberDependencyGraphBuilder` exposes a `relaxedForwardReferences` knob for the strict
mode: if any group is configured with `relaxedForwardReferences: false` and the
resulting graph contains a cycle, the builder automatically retries with all groups
forced back to relaxed mode. If the relaxed graph is also cyclic, a `SortingException`
is thrown.

## Conventions and limitations

- Only **same-file** dependencies are tracked. Cross-file circular static-initializer
  dependencies are considered an application-design problem, not something a
  harmonization tool should silently route around.
- Method bodies and constructor bodies are intentionally not treated as
  declaration-order dependencies, matching Java's actual rules.
- Generic type bounds, annotation type usage, and similar same-file cross-type
  references that Java does not enforce as declaration-order constraints are not
  modeled here either.

## Filtering

For details on what gets filtered out before the graph is constructed (for example
self-loops, accessor bundles when accessor co-location is off, edges across types),
see [`order-dependency-filter.md`](order-dependency-filter.md).
