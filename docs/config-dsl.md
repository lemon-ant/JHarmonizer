<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer Configuration DSL

This document describes the YAML schema accepted by JHarmonizer (read by
`JHarmonizerConfigLoader` into the `JHarmonizerConfig` model and converted into the
unified/compiled internal model used by the sorter and formatter).

A user-supplied configuration file is **merged over** the embedded default configuration
(`core/src/main/resources/default-config.yml`). Any block omitted by the user is taken
from the defaults; root member groups are merged by name (see
[Merging member groups](#merging-member-groups) below).

## Top-level layout

```yaml
formatting: { ... }
backups-enabled: true
processing-statistics-mode: MINIMAL
annotations-ordering: [ declaration-length-asc, name-length-asc, alpha, arguments-alpha ]
header-line: { ... }
top-level-types-ordering: { ... }
type-members-ordering:
  - { ... }
```

All seven top-level keys are required in a strict (non-flexible) configuration; in flexible/overlay
configurations any of them may be omitted and inherited from the baseline.

## `formatting`

| Key                            | Type                                              | Description                                                                |
|--------------------------------|---------------------------------------------------|----------------------------------------------------------------------------|
| `fix-imports`                  | `boolean`                                         | Reorder/remove imports during the final formatting pass.                   |
| `formatter-style`              | `NONE`, `AOSP`, `GOOGLE`, `PALANTIR`              | Underlying Palantir-formatter style; `NONE` skips Palantir formatting.     |
| `blank-line-after-type-header` | `boolean`                                         | Insert a blank line after the type declaration header before the first member. |
| `blank-line-before-comment`    | `boolean`                                         | Insert a blank line before members carrying leading comments.              |
| `blank-line-between-fields`    | `boolean`                                         | Insert a blank line between consecutive field declarations.                |

## `backups-enabled`

`boolean`. When `true`, `reorder` writes a `<file>.bak` copy of every modified source.

## `processing-statistics-mode`

`FULL`, `MINIMAL`, or `DISABLED`. Controls how much processing-statistics output is printed at the end of a run.

## `annotations-ordering`

Global annotation sorting criteria, applied from left to right. Each next criterion resolves ties in the preceding
criteria; complete ties retain their original order.

| Criterion | Order |
|-----------|-------|
| `alpha` | Simple annotation name, A to Z, case-sensitive. |
| `name-length-asc` | Shorter simple names first. |
| `declaration-length-asc` | Shorter complete annotation declarations first, including arguments and internal comments. |
| `arguments-alpha` | Case-sensitive alphabetical argument text, including parameter names and assignments. |

The default is `[ declaration-length-asc, name-length-asc, alpha, arguments-alpha ]`. A single criterion may be written
as a scalar, for example `annotations-ordering: arguments-alpha`. Tokens accept hyphens or underscores and are
case-insensitive.
Use `annotations-ordering: []` to preserve annotation order. An overlay replaces the whole criterion list; omitting
the setting inherits the baseline.

Declaration length counts the written qualified name, `@`, parentheses, parameter names, assignments, values, and
comments inside the declaration. Inter-token whitespace is excluded; string contents count, with incidental text-block
indentation removed. Comments outside the declaration are excluded from its length. The original annotation text is
printed unchanged. Comments move according to their source adjacency:

| Comment placement | Attachment |
| --- | --- |
| Starts on the same line as an annotation | Preceding annotation. |
| Standalone block with no blank line before the next annotation | Following annotation, including when there is also no blank line above the block. |
| Standalone block adjacent to the preceding annotation, with a blank line below the block | Preceding annotation. |
| Blank lines above and below the block | Original source gap. |

Declaration JavaDoc and file preambles retain their positions. A moved `//` comment retains a line break before the next
Java token. A block attached below an annotation moves with its following blank line, preserving its attachment at the
new position without leaving an extra blank line between annotations. Gaps around independent comment blocks remain in
place. Blank lines inside a block comment do not separate it from an annotation.

Argument comparison concatenates Java tokens inside the parentheses in their written order, excluding comments and
inter-token whitespace. For example, `@Tag("aa")` precedes `@Tag("zz")`, and `@Tag(value = "aa")` compares as
`value="aa"`.
Constant references and expressions are compared as written, without evaluation. Annotations without parentheses sort
before those with empty parentheses, followed by non-empty argument text: `@Tag`, `@Tag()`, `@Tag("")`. Parentheses
containing only comments or whitespace compare as empty parentheses.

Sorting applies to annotations on packages, modules, types and their descendants, including members, parameters,
local variables, record components, and type uses. Adjacent annotations form a sorting group; other Java tokens separate
groups. Annotation arguments and arrays of nested annotation values retain their order. Where Spoon omits annotations,
the Java lexer supplies the source fragments and the same compiled comparator orders them.
File and type opt-out directives also disable annotation sorting in their scope.

## `header-line`

| Key            | Type      | Description                                                                |
|----------------|-----------|----------------------------------------------------------------------------|
| `character`    | `char`    | Character used to draw the header-line separator inside `// ----` comments. |
| `left-padding` | `int`     | Number of leading filler characters before the group name in the header.   |

## `top-level-types-ordering`

Controls the order of top-level types inside a single `.java` file.

| Key               | Type                                       | Description                                                                                                                                  |
|-------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `main-type-first` | `boolean`                                  | Place the public top-level type whose name matches the file name first, regardless of the rules below.                                       |
| `type-groups`     | non-empty list of selectors                | Ordered list of type-kind buckets. Each entry is a single kind or a list/array of kinds. Each `JHarmonizerTypeKind` may appear at most once. |
| `ordering-rules`  | non-empty list of `UnifiedOrderingRule`    | Comparators applied **inside** each `type-groups` bucket (e.g. `[ visibility-desc, alpha ]`).                                                |

`type-groups` accepts the following type-kind tokens: `class`, `interface`, `enum`,
`annotation`, `record`.

## `type-members-ordering`

Non-empty ordered list of root member groups. Each entry is a `member-group` node.

### `member-group` node

| Key                        | Type                                  | Required        | Description                                                                                                                                                          |
|----------------------------|---------------------------------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name`                     | `String`                              | yes             | Stable, human-readable identifier. Used both for separator headers and for overlay merge by name.                                                                    |
| `includes`                 | selector DSL                          | one of include/exclude required | Members included in this group.                                                                                                                       |
| `excludes`                 | selector DSL                          | one of include/exclude required | Members excluded from this group (after `includes` matched).                                                                                          |
| `ordering-rules`           | list of `UnifiedOrderingRule`         | optional        | Comparators applied to members inside this group. Inherited from the nearest ancestor that defines it; an empty list `[]` is allowed.                                |
| `separator`                | `new-line`, `header`, `none`          | optional        | Visual separator inserted before the rendered group. Inherited.                                                                                                      |
| `keepAccessorsTogether`    | `boolean`                             | optional        | When `true`, getter/setter pairs for the same JavaBean property are kept adjacent. Inherited.                                                                        |
| `relaxedForwardReferences` | `boolean`                             | optional        | When `true` (default), only references to fields above the current member create dependency-graph constraints. When `false`, every same-type field reference creates a constraint. |
| `groups`                   | list of nested `member-group` nodes   | optional        | Recursively defines a child group tree. Children participate in dispatch in the order listed.                                                                        |

A group must define **at least one** of `includes`/`excludes`.

#### Selector DSL (`includes`/`excludes`)

Each list represents an OR of rule lines; each rule line is an AND of atomic
constraints (parsed by `MemberGroupRuleLineParser`). Accepted YAML shapes:

| YAML shape                        | Meaning                                   |
|-----------------------------------|-------------------------------------------|
| `includes: field`                 | one rule line with a single token         |
| `includes: static, final`         | one rule line with two AND-tokens         |
| `includes: [ static, final ]`     | flow array on one line — same as above    |
| <pre>includes:<br>  - field<br>  - method</pre> | two rule lines (OR) with one token each |
| <pre>includes:<br>  - [ class, '@Nested' ]<br>  - [ class, '@~.*Test$' ]</pre> | two rule lines (OR), each AND-combining several tokens |

Acceptance for a member: `any(includeLines) AND NOT any(excludeLines)`.
Empty `includes` matches every member; empty `excludes` matches nothing.

#### Atomic tokens

| Token kind            | Examples                                                   | Notes                                                                                                  |
|-----------------------|------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Member kind           | `field`, `method`, `constructor`, `init` / `initializer`, `class`, `interface`, `enum`, `record`, `annotation`, `enum-constant`, `record-component` | Corresponds to `MemberKind`.                                              |
| Access level          | `public`, `protected`, `package` / `package-private`, `private` | Corresponds to `MemberAccess`.                                                                  |
| Declaration modifier  | `static`, `final`, `abstract`, `default`, `synchronized`, `transient`, `volatile`, `native`, `strictfp`, `sealed`, `non-sealed` / `nonsealed` | Corresponds to `DeclarationModifier`. Conflicting combinations are rejected at compile time. |
| Exact name            | `=foo`                                                     | Member name must equal `foo`. Only one name token per rule line.                                       |
| Regex name            | `~.*Test$`                                                 | Member name must match the regex.                                                                      |
| Exact annotation      | `@Override`                                                | Member must carry the `@Override` annotation.                                                          |
| Regex annotation      | `@~.*Mapping$`                                             | Member must carry an annotation whose simple name matches the regex.                                   |

#### `ordering-rules` values

`UnifiedOrderingRule` enum, accepted as YAML scalars:

| Value             | Effect                                                             |
|-------------------|--------------------------------------------------------------------|
| `alpha`           | Alphabetical by member name (ties broken deterministically).        |
| `preserve`        | Preserve original source order.                                     |
| `visibility-asc`  | Least-visible first: `private` → `package` → `protected` → `public`. |
| `visibility-desc` | Most-visible first: `public` → `protected` → `package` → `private`.  |

`ordering-rules` can be given as a single scalar (`ordering-rules: alpha`) or a list
(`ordering-rules: [ visibility-desc, alpha ]`).

#### `separator` values

| Value      | Effect                                                                                                            |
|------------|-------------------------------------------------------------------------------------------------------------------|
| `new-line` | Insert a blank line before the rendered group.                                                                    |
| `header`   | Insert a `// ----- <name> -----`-style header comment built from `header-line.character` and `header-line.left-padding`. |
| `none`     | No visual separator.                                                                                              |

## Merging member groups

When a user-provided configuration is applied on top of the embedded defaults,
`type-members-ordering` root groups are merged **by `name`**:

- if a custom root-group name matches a default root-group name, the default root group
  is fully replaced (the replacement keeps the original default position);
- if a custom root-group name is new, that root group is inserted before all default
  root groups, in the order it appears in the custom file;
- nested `groups:` blocks are not merged recursively — replacing a root group replaces
  its whole subtree.

This lets a user override one named root group while keeping the rest of the default
model unchanged.

## Reference example

A complete, working reference configuration that exercises every feature documented above
is the embedded default at `core/src/main/resources/default-config.yml`. Read it side by
side with this document — the schema is what ships, the defaults are what users inherit
when they do not provide their own file.
