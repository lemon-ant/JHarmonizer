<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Member dependency graph

JHarmonizer does not use a single Spoon `Filter` to mark order-dependent members. The current implementation builds a directed member dependency graph for each `CtType` through `MemberDependencyGraphBuilder`.

## Edge kinds

| Edge kind | Meaning |
|---|---|
| `DECLARATION_DEPENDENCY` | Real provider-before-dependent ordering constraint used by dependency-aware sorting. |
| `ACCESSOR_BUNDLE` | Non-ordering edge used to keep JavaBean accessors for the same property together; ignored by topo-ordering. |

## Provider chain

For each source member, the graph builder asks these providers for direct provider edges:

1. `AccessorPairDependencyProvider`
2. `EnumConstantInitializerDependencyProvider`
3. `BlankFinalDefiniteAssignmentDependencyProvider`
4. `FieldInitializerBackwardReferenceDependencyProvider`
5. `ExplicitThisInitializerFieldDependencyProvider`
6. `ExplicitDeclaringTypeInitializerFieldDependencyProvider`
7. `InitializerBlockDependencyProvider`
8. `InitializerBlockMutableFieldReadDependencyProvider`
9. `CrossTypeConstantBackRefDependencyProvider`

The graph stores edges as `provider -> dependent`.

## Strict and relaxed forward references

Provider behavior is controlled by each member's natural group configuration:

- `keepAccessorsTogether` enables accessor bundle edges;
- `relaxedForwardReferences` controls whether forward field references become declaration dependencies.

In relaxed mode, only backward field references where the provider already appeared earlier in source order contribute dependency edges. In strict mode, forward references can also contribute dependency edges.

If a strict graph contains a declaration-dependency cycle and at least one group uses strict mode, the builder retries with all groups forced to relaxed mode. If the relaxed graph is still cyclic, sorting fails with `SortingException`.

## Scope

This document describes the implemented dependency-graph assembly. Broader Java declaration-order notes live in `declaration-order-dependencies.md` and known intentionally unsupported or unmodeled patterns live in `known-unhandled-patterns.md`.
