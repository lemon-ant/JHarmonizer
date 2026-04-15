# 07 — Static block field initializer forward reference

## Symptom

After processing, the application starts failing at runtime. Reverting the
JHarmonizer-produced file restores correct behaviour.

## Root cause

`ITEM_REGISTRY` is a mutable `HashMap` that is pre-declared but populated by a
`static {}` block. `REGISTRY_SNAPSHOT` captures the contents of `ITEM_REGISTRY`
at class-load time via `List.copyOf(ITEM_REGISTRY.values())`.

The correct static-initialization order is:

1. Declare `ITEM_REGISTRY` (empty `HashMap`).
2. Run `static {}` block — fills `ITEM_REGISTRY`.
3. Initialize `REGISTRY_SNAPSHOT` — copies the now-populated values.

Before the fix, JHarmonizer moved the `static {}` block to *after* both field
declarations, so `REGISTRY_SNAPSHOT` was initialized from an empty map and
remained empty for the lifetime of the class.

## Why the blank-final path (case 14) was not enough

Case 14 (`BlankFinalOptimizedProviderSample`) uses a **blank final** that is
directly *assigned* inside the `static {}` block.
`BlankFinalDefiniteAssignmentDependencyProvider` detects that assignment (`=`)
and keeps the block in the right position.

In this case the field already has an initializer (`new HashMap<>()`). The
`static {}` block does not *assign* the field — it only calls `put()` on the
object the field already holds. In Spoon's AST, `ITEM_REGISTRY.put(...)` is a
`CtFieldRead` of `ITEM_REGISTRY` followed by a method invocation, not a
`CtFieldWrite`. The blank-final provider only looks for `CtFieldWrite`, so it
missed the dependency.

## Fix

`InitializerBlockMutableFieldReadDependencyProvider` — when a field G's
initializer reads a non-compile-time-constant field F, and there is an
initializer block B declared before G in source order that also reads F (via
method calls that may mutate the object F references), an ordering dependency
edge B → G is added to the dependency graph. This prevents the sorter from
moving B after G.
