<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# 11 — Non-idempotent blank line inside nested interface after sort

## Observed failure (before fix)

Running JHarmonizer twice on the same file produces different output:

- **First pass** correctly produces no blank line between `interface Builder {` and `RegistryService build();`.
- **Second pass** inserts a spurious blank line after `interface Builder {`, making the formatting non-idempotent.

## Minimal reproducer

```java
public interface RegistryService extends Closeable {

    /**
     * The builder.
     */
    interface Builder {

        Builder withOptions(Object options);

        RegistryService build();

        Object getOptions();

    }
}
```

### After first pass (stable — no blank line after `{`):

```java
    interface Builder {
        RegistryService build();

        Object getOptions();

        Builder withOptions(Object options);
    }
```

### After second pass (spurious blank line added — non-idempotent):

```java
    interface Builder {

        RegistryService build();

        Object getOptions();

        Builder withOptions(Object options);
    }
```

## Root cause — Spoon comment misattribution across nested type boundary

This is caused by a Spoon parsing quirk: when a nested type's opening `{` is immediately followed
by its first member (no blank line between them), Spoon can misattribute the type's own javadoc
comment (which precedes the `interface`/`class` keyword) to that first inner member instead of to
the enclosing type declaration.

In the first-pass output, `interface Builder {` and `RegistryService build();` are on adjacent lines
with no blank line between them. When Spoon parses this source:

- `// ---...---` (the separator comment before `/** The builder. */`) → attributed to `Builder` type
- `/** The builder. */` (the javadoc for `Builder`) → misattributed to `build()` (first inner member)

When `blank-line-before-comment: true` (the default), the misattributed `/** The builder. */`
makes the printer believe `build()` has a genuine leading comment, triggering a spurious blank
line before it. Since `build()` is the first sorted member of `Builder`, this blank line appears
directly after `interface Builder {`.

**The misattribution is position-dependent**: in the original source, `Builder` has a blank line
after `{` (before `withOptions`), so Spoon correctly attributes the javadoc to `Builder`. After the
first pass (sorted output has no blank line after `{`), Spoon misattributes the javadoc to `build()`.

## JHarmonizer workaround (fix location)

- `io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils`
- Method: `hasLeadingCommentOnSeparateLine(CtTypeMember, Set<Integer>, int typeBodyStartLine)`

Approach: when deciding whether a member has a genuine leading comment (warranting a blank line),
filter out any comment whose start line is strictly before the enclosing type's declaration line
(`typeBodyStartLine`). A comment that starts before the type keyword line is outside the type body
and cannot be a genuine leading comment for any inner member.

The `typeBodyStartLine` is `type.getPosition().getLine()`, which returns the line of the `interface`
or `class` keyword. Comments starting before this line (outside the type body) are filtered out.
Genuine leading comments inside the type body start on or after the type's declaration line.

## Upstream Spoon issue

This bug has not yet been reported upstream. See `docs/TODO.md` §8 for the action items to file
a minimal reproducer with the Spoon issue tracker and to link the upstream issue back here once filed.
