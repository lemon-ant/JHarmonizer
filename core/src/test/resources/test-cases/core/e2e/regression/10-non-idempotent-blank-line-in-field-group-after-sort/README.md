<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# 10 — Non-idempotent blank line after type header after field reorder

## Observed failure (before fix)

Running JHarmonizer twice on the same file produces different output:

- **First pass** inserts a spurious blank line directly after the class opening brace `{`.
- **Second pass** removes it — making the formatting non-idempotent.

## Minimal reproducer

```java
import java.util.concurrent.ExecutorService;

public abstract class TestListener {
    private volatile ExecutorService executor; // keep volatile
    private volatile Object conn;              // keep volatile
    private final Object config;
}
```

### After first pass (non-deterministic — spurious blank line):

```java
public abstract class TestListener {

    private final Object config;

    private volatile Object conn; // keep volatile
    private volatile ExecutorService executor; // keep volatile
}
```

### After second pass (stable output):

```java
public abstract class TestListener {
    private final Object config;

    private volatile Object conn; // keep volatile
    private volatile ExecutorService executor; // keep volatile
}
```

## Root cause — Spoon upstream comment misattribution bug

This is caused by a Spoon bug: when a member is moved during reordering, Spoon misattributes
its trailing inline `// comment` to the next member in the original source order.

Specifically, after parsing the **first-pass output** (which has `config` first, then `conn`,
then `executor`), Spoon correctly attributes the trailing `// keep volatile` to each volatile
field. However, when parsing the **original source** (where volatile fields appear before `config`),
Spoon attributes the trailing `// keep volatile` comment from `conn` (the field ending on line N) to
`config` (the next element in AST order), because the comment appears before `config` in source order.

When `blank-line-before-comment: true` (the default), the misattributed comment makes the printer
believe `config` has a leading comment, triggering a spurious blank line before it. Because `config`
is the first member after sorting, this blank line appears directly after `{`.

**The misattribution is position-dependent**: after the first pass, the volatile fields appear after
`config` in source order, so the comment is correctly attributed and the blank line is not inserted
on the second pass.

## Scope of misattribution

The misattribution occurs because Spoon assigns comments to the **next AST element in source order**
when the comment is not directly attached by position. A trailing inline `// comment` on line N of
member A gets attached to member B (the next member in original source order) after AST traversal,
even if member B is on a different line.

This affects multi-member types where:
1. At least one member has a trailing inline `// comment`.
2. Member ordering changes after reorder (so the comment-bearing member is no longer adjacent to
   the same next-member as in the original source).

## JHarmonizer workaround (fix location)

- `io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils`
- Method: `hasLeadingCommentOnSeparateLine(CtTypeMember, Set<Integer>)`

Approach: when deciding whether a member has a genuine leading comment (warranting a blank line),
filter out any comment whose start line coincides with the **last source line** (`getEndLine()`) of
any other member declaration in the same type. Trailing inline `// comments` always appear on the
last line of the member declaration they are attached to — including when that declaration spans
multiple lines (e.g., a field annotated with `@Deprecated` on its own line). Using `getEndLine()`
rather than `getLine()` is essential for multi-line declarations: `getLine()` would give the first
line of the declaration, missing the trailing comment that appears on the last line.

Such a filtered comment is a trailing inline comment that was misattributed by Spoon — not a real
standalone leading comment. Genuine leading comments occupy their own source lines (lines not used
as the end line of any member declaration), so they are unaffected by this filter.

## Test fixtures

Two fixtures cover this scenario:

### `TestListener.java` — single-line declarations
The primary reproducer (3 fields, 1 import). All fields are single-line declarations. The bug is
triggered and verified here.

### `TestListenerMultiLine.java` — multi-line declaration with trailing comment
Adds a `@Deprecated` annotation on a separate line before the `executor` field, making `executor`
a two-line declaration. This fixture specifically verifies that the fix uses `getEndLine()` rather
than `getLine()` to detect misattributed trailing inline comments. With `getLine()`, the annotation
line would be in the filter set but the declaration's last line would not, allowing the misattributed
comment to pass through and trigger a spurious blank line after `{`.

## Upstream Spoon issue

This bug has not yet been reported upstream. See `docs/TODO.md` §7 for the action item to file
a minimal reproducer with the Spoon issue tracker.
