<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Java Sorter Configuration DSL

This document describes the YAML configuration model consumed by the current JHarmonizer code. The built-in defaults live in `core/src/main/resources/default-config.yml`, and external YAML files are parsed as partial overlays on top of those defaults.

## Root keys

A complete default configuration contains these root keys:

```yaml
formatting:
  fix-imports: true
  formatter-style: PALANTIR
  blank-line-after-type-header: false
  blank-line-before-comment: true
  blank-line-between-fields: false

backups-enabled: true
print-processing-statistics: true

header-line:
  character: '-'
  left-padding: 3

top-level-types-ordering:
  main-type-first: true
  type-groups:
    - [ class, record ]
    - interface
    - enum
    - annotation
  ordering-rules: [ visibility-desc, alpha ]

type-members-ordering:
  - name: Default Rule
    includes: ~.*
    ordering-rules: preserve
    groups: []
```

External config files used by the CLI and Maven plugin are flexible overlays: they may contain any non-empty subset of these root keys. Missing values continue to come from the embedded defaults.

## Formatting

`formatting` controls the final formatter/import pass and printer-specific blank-line options.

| Key | Meaning |
|---|---|
| `fix-imports` | If `true`, the final Palantir formatter pass also fixes imports. |
| `formatter-style` | Formatter style token. Current YAML tokens are `PALANTIR`, `GOOGLE`, `NONE`, and `AOP`; `AOP` maps to Palantir's AOSP style in code. Values are case-insensitive and may use hyphens where enum names use underscores. |
| `blank-line-after-type-header` | Insert a blank line after a type header before its first member. |
| `blank-line-before-comment` | Insert a blank line before members that have leading comments. |
| `blank-line-between-fields` | Insert a blank line between consecutive field declarations. |

`formatter-style: NONE` disables source formatting but still allows import fixing when `fix-imports: true`.

## Top-level type ordering

`top-level-types-ordering` controls ordering of top-level types inside a compilation unit.

| Key | Meaning |
|---|---|
| `main-type-first` | If `true`, the type whose name matches the file name is kept first. |
| `type-groups` | Ordered type-kind groups. Each entry may be a scalar such as `interface` or a flow list such as `[ class, record ]`. |
| `ordering-rules` | Rules applied within top-level groups. |

Supported type kinds are `class`, `record`, `interface`, `enum`, and `annotation`. Each kind may appear only once across `type-groups`.

## Member groups

`type-members-ordering` is an ordered list of root member groups. The first root group whose selectors match a type defines the ordering tree for that type.

Each group supports:

| Key | Required | Meaning |
|---|---:|---|
| `name` | yes | Group name. Root group names are also used when merging external overlays with defaults. |
| `includes` | one of includes/excludes | Selector expression describing members/types included in the group. |
| `excludes` | one of includes/excludes | Selector expression describing members/types excluded from the group. |
| `ordering-rules` | no | String or list of ordering rules. |
| `separator` | no | Separator style: `new-line`, `header`, or `none`. |
| `keepAccessorsTogether` | no | If `true`, JavaBean-style accessors are clustered by property before member ordering is applied. |
| `relaxedForwardReferences` | no | If `true`, forward-reference dependency protection is relaxed for this group subtree. |
| `groups` | no | Nested member groups. |

At least one of `includes` or `excludes` must be non-empty for every group.

## Selector syntax

Selectors deserialize into OR-of-AND token sets.

| YAML form | Meaning |
|---|---|
| `includes: field` | One AND group with one token. |
| `includes: field, static` | One AND group: both `field` and `static` must match. |
| `includes: [ field, static ]` | One AND group when written as a one-line flow list. |
| Block list with scalar entries | OR alternatives: each line is its own selector group. |
| Block list with nested flow lists | OR alternatives where each nested list is an AND group. |

Example:

```yaml
includes:
  - [ field, static, final ]
  - [ method, '@Test' ]
```

Selector tokens used by the default config include:

- member/type kind tokens: `field`, `method`, `constructor`, `initializer`, `record-component`, `enum-constant`, `class`, `record`, `interface`, `enum`, `annotation`;
- modifier/visibility tokens: `public`, `protected`, `package-private`, `private`, `static`, `final`;
- annotation selectors prefixed with `@`, for example `@Test` or `@~.*Test$`;
- exact-name selectors prefixed with `=`, for example `=toString`;
- regular-expression selectors prefixed with `~`, for example `~.*Test$`.

## Ordering rules

`ordering-rules` accepts either a single string or a YAML list. Comma-separated strings are also accepted.

Supported values are:

| Value | Meaning |
|---|---|
| `preserve` | Preserve source order for this group. |
| `alpha` | Sort by the computed alphabetical key. |
| `visibility-desc` | Sort from most visible to least visible: public → protected → package-private → private. |
| `visibility-asc` | Sort from least visible to most visible: private → package-private → protected → public. |

Values are case-insensitive and hyphenated YAML values map to enum names with underscores.

## Option inheritance

For nested member groups, these options are inherited from the nearest parent that defines them:

- `keepAccessorsTogether`
- `separator`
- `ordering-rules`
- `relaxedForwardReferences`

Inheritance is resolved top-down. If a child defines an option explicitly, it replaces the inherited value for that child subtree. For `ordering-rules`, an explicit child value fully replaces the inherited list; `ordering-rules: []` is allowed and means no explicit sort keys at that level.

## Overlay merge semantics

When a custom configuration is applied over the embedded defaults, root groups from `type-members-ordering` are merged by exact root-group `name`:

- if a custom root group name matches a default root group name, that default root group is fully replaced;
- the replacement stays at the original default position;
- if a custom root group name is new, that group is inserted before all default root groups;
- multiple new custom root groups keep their relative order from the custom file;
- nested `groups:` blocks are not merged recursively.

This lets a project override one named root group while keeping the rest of the default model unchanged.
