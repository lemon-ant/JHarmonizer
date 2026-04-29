# Replace topo-style final ordering with provider-lift repair

## Goal

Replace the current final dependency-aware ordering strategy so that the produced order is no longer driven by lexicographical/stable topological selection of the next legal element.

The new final ordering behavior must be based on **provider lift**:
the algorithm should preserve the existing base order as the primary desired sequence and repair it by moving required providers directly before the earliest blocked dependent element.

## Problem

The current ordering strategy may produce a dependency-valid order that is technically correct but visually less intuitive.

In particular, the current behavior may:
- spread dependency-related elements across the output;
- push dependent elements downward in ways that are hard to explain locally;
- preserve global legality while producing an order that is less readable for humans.

The desired behavior is different:
- keep the base order as the default intention;
- when a dependency conflict is encountered, resolve it by pulling the required providers upward;
- keep the repair local, contiguous, and easy to explain.

## Required change

Change the final ordering policy so that:

- ordering starts from the already computed base order;
- the algorithm scans that order from left to right;
- when the earliest blocked dependent element is encountered, the algorithm resolves the conflict by moving the required provider closure directly before that dependent element;
- the process repeats until the order becomes dependency-valid.

## Behavioral requirements

### Base order remains primary

The existing base comparator and the already computed base order must remain the primary desired order.

The new logic must not replace the base order with an unrelated global traversal strategy.
Instead, it must **repair** the base order only where required by hard dependencies.

### Earliest blocked element wins

When the current order is invalid, the algorithm must always resolve the **earliest blocked dependent element** first.

Do not skip a blocked element in order to continue producing later legal elements.

### Provider lift is contiguous

When a repair is required, the moved provider set must appear as one contiguous block directly before the blocked dependent element.

Do not smear lifted providers across the order.
Do not separate the lifted providers from the dependent element they justify.

### Transitive providers must be respected

The repair must account for the required transitive provider closure, not only direct providers.

If a provider needed for the blocked element itself depends on other providers that are still located later in the order, those prerequisites must be handled as part of the same repair.

### Stability of unaffected elements

Elements that are not part of the current repair must preserve their relative order.

The new policy must minimize collateral reordering and must not introduce unrelated reshuffling.

### Determinism

For identical input, dependencies, and base order, the output must always be identical.

### Existing dependency model must stay intact

Keep the existing dependency graph construction, dependency semantics, and any existing cycle-condensation / bundling logic intact.

This task is about replacing the **final ordering policy**, not redesigning dependency extraction.

## Explicit non-goals

This task must **not** introduce:
- dependent shift;
- branching or search-based candidate exploration;
- scoring between multiple alternative outputs;
- heuristic multi-strategy comparison;
- arbitrary global reordering beyond provider lift repair.

The required behavior is a single deterministic strategy: **provider lift**.

## Acceptance criteria

- The final order is produced by provider-lift repair over the base order.
- The algorithm no longer relies on lexicographical/stable topological next-node selection as the final ordering policy.
- The earliest blocked dependent element is always repaired first.
- Required providers are moved directly before the blocked dependent element as a contiguous block.
- Transitive unmet providers are respected.
- Unaffected elements preserve relative order.
- The output remains dependency-valid and deterministic.

## Test requirements

Add or update regression tests so that provider-lift behavior is explicitly locked down.

At minimum cover:

- a case where the previous topo-style strategy produced a dependency-valid but visually less intuitive result;
- a case where transitive providers must be lifted together;
- a case showing that unrelated elements preserve their relative order;
- a case proving that the final order is built by provider-lift repair rather than by selecting the next currently legal node.

Use exact expected output assertions for the most important regression fixtures.
