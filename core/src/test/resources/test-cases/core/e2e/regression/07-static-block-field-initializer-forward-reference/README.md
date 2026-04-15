# 07 — Static block field initializer forward reference

## Symptom

After processing, the application starts failing at runtime. Reverting the
JHarmonizer-produced file restores correct behaviour.

## Root cause (under investigation)

`ITEM_REGISTRY` is a mutable `HashMap` that is pre-declared but populated by a
`static {}` block. `REGISTRY_SNAPSHOT` captures the contents of `ITEM_REGISTRY`
at class-load time via `List.copyOf(ITEM_REGISTRY.values())`.

The correct static-initialization order is:

1. Declare `ITEM_REGISTRY` (empty `HashMap`).
2. Run `static {}` block — fills `ITEM_REGISTRY`.
3. Initialize `REGISTRY_SNAPSHOT` — copies the now-populated values.

JHarmonizer moves the `static {}` block to *after* both field declarations,
so `REGISTRY_SNAPSHOT` is initialized from an empty map and remains empty for
the lifetime of the class.

## Difference from the blank-final pattern (case 14)

Case 14 (`BlankFinalOptimizedProviderSample`) uses a **blank final** that is
directly *assigned* inside the `static {}` block. JHarmonizer detects that
assignment and keeps the block in the right position.

In this case the field already has an initializer (`new HashMap<>()`). The
`static {}` block does not *assign* the field — it only calls `put()` on the
object the field already holds. JHarmonizer does not model that method call as
a write to the field's contents, so it fails to detect that any subsequent
field reading those contents must come *after* the block.
