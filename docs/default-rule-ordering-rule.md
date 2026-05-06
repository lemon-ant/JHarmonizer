<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer "Default Rule" — member ordering specification

This document describes the structure and semantics of the **Default Rule** root member
group shipped in `core/src/main/resources/default-config.yml`. It is the fallback root
group: it matches every member that no earlier root group has claimed (its include
selector is the regex `~.*`).

## Root rules in `default-config.yml`

The embedded `default-config.yml` is **not** a single ordering rule. It defines a list
of root rules under `type-members-ordering:`, evaluated in order. Each top-level type
in the project is matched against the rules from top to bottom; the first matching
rule wins, and that rule's `groups:` subtree is used to order the type's members.

The rules shipped in the embedded default, in declaration order, are:

| # | Root rule           | What it matches                                                                                                                                                                  | Why it exists                                                                                                                                                                         |
|---|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | **Test Classes**    | Classes annotated with `@Test`/`@ExtendWith`/`@Nested`/`@RunWith`, or whose name ends in `Test` / `IT` (JUnit 4/5 conventions).                                                    | Test classes have a different natural shape: JUnit fields and constants on top, then `@BeforeEach`/`@BeforeAll`, then `@Test`/`@ParameterizedTest`/etc., then `@AfterEach`/`@AfterAll`, then utility methods, then `@TestConfiguration` and `@Nested` test classes. The general-purpose Default Rule would be a poor fit here. |
| 2 | **DTO and Entities**| Classes named `*Dto` / `*DTO` / `*Entity`, or annotated with `@Value`, `@Data`, `@Entity`, `@MappedSuperclass`, `@Embeddable`, `@ConfigurationProperties`, or `@Document`. Excludes `interface`, `enum`, `annotation`. | Data-carrier classes benefit from the strict `Serializable UID → constants by visibility → static fields → final instance fields → static initializers → static methods → instance initializers → constructors → accessors (kept together) → other public methods → `equals`/`hashCode`/`toString`/`clone`/`compareTo` → non-public methods` shape. |
| 3 | **Default Rule**    | Everything else (`includes: ~.*`).                                                                                                                                               | The general-purpose fallback documented in the rest of this file: production classes that are neither tests nor DTOs/entities.                                                          |

In other words, the embedded default is **already specialized** for the three most
common kinds of Java types in real projects: tests, data carriers, and "regular" code.
A user-supplied YAML overlay can add new root rules in front of the defaults (they are
merged at the root level by `name`; see [`02-Configurator.md`](02-Configurator.md)).

This document describes only **rule #3 — Default Rule**. Rules #1 and #2 are
self-documenting in `default-config.yml` itself.

## What the Default Rule does (rule #3)

This is a description of what the embedded default actually does for the third rule.
The full reference schema for member-group YAML lives in [`config-dsl.md`](config-dsl.md);
the documented ordering-rule values (`alpha`, `preserve`, `visibility-asc`,
`visibility-desc`) are the ones used below.

## Top-level layout (in order)

```
Default Rule  (matches ~.*, ordering-rules: preserve)
├─ 1) Record components
├─ 2) Enum constants
├─ 3) Fields
├─ 4) Initializers
├─ 5) Methods (including constructors)
└─ 6) Nested types
```

The root `ordering-rules: preserve` controls the rendering between these top-level
subgroups when no inner rule applies. Each subgroup overrides its own ordering as
described below.

## 1) Record components

```yaml
- name: Record components
  includes: [ record-component ]
```

Bucket for `RECORD_COMPONENT` members. They live in the record header, so the bucket
exists only to keep them separate from regular fields; ordering is inherited
(`preserve`).

## 2) Enum constants

```yaml
- name: Enum constants
  includes: [ enum-constant ]
```

Bucket for `ENUM_CONSTANT` members. Enum constants must stay at the top of an enum
body; ordering is inherited (`preserve`).

## 3) Fields

```yaml
- name: Fields
  separator: new-line
  ordering-rules: [ visibility-desc, alpha ]
  includes: field
```

All `FIELD` members. A blank line is inserted before this group at render time.
Inside the group, fields are ordered by visibility (most-visible first), then alpha.
Children:

```
Fields
├─ Static fields              (includes: static)
│  └─ Static final fields    (includes: final)
│     ├─ serialVersionUID    (=serialVersionUID, preserve)
│     └─ Logger fields       (~(?i)(log|.*logger)$, preserve)
└─ Instance final fields     (includes: final)
```

Notes:
- `Static fields` does not declare its own ordering — it inherits `[ visibility-desc, alpha ]`.
- `Static final fields` has only two named special-case children (`serialVersionUID`
  and `Logger fields`); other static-final fields fall back to the parent's
  `[ visibility-desc, alpha ]`.
- `Instance final fields` exists but there is **no** explicit "Instance non-final
  fields" subgroup — non-final instance fields are placed by the parent rules.

## 4) Initializers

```yaml
- name: Initializers
  includes: [ initializer ]
  groups:
    - name: Static initializers
      includes: static
```

Only `Static initializers` is broken out as an explicit child. Instance initializer
blocks fall through to the parent and inherit `preserve`.

## 5) Methods (including constructors)

```yaml
- name: Methods
  ordering-rules: alpha
  keepAccessorsTogether: true
  includes:
    - method
    - constructor
```

`keepAccessorsTogether: true` is set at this level and inherited by all descendants —
JavaBean getter/setter pairs declared inside the same type stay adjacent.

Children, in order:

```
Methods
├─ Public
│  ├─ Static methods       (includes: static)
│  ├─ Constructors         (includes: constructor)
│  ├─ Instance methods     (excludes: =toString, =equals, =hashCode, =clone, =compareTo)
│  └─ Basic Object Methods (includes: =toString, =equals, =hashCode, =clone, =compareTo; ordering-rules: alpha)
├─ Protected
│  ├─ Static methods
│  ├─ Constructors
│  └─ Instance methods
├─ Package-private
│  ├─ Static methods
│  ├─ Constructors
│  └─ Instance methods
└─ Private
   ├─ Static methods
   ├─ Constructors
   └─ Instance methods
```

Notes:
- The `Basic Object Methods` subgroup is **only** under `Public` — `equals`/`hashCode`/
  `toString`/`clone`/`compareTo` are pinned at the end of the public methods block.
  The other visibility groups do not single them out.
- Each leaf inherits `ordering-rules: alpha` from `Methods`.

## 6) Nested types

```yaml
- name: Nested types
  separator: new-line
  ordering-rules: alpha
  includes:
    - annotation
    - enum
    - record
    - interface
    - class
```

A blank line is inserted before the group at render time. Children are by visibility,
then split into two combined buckets:

```
Nested types
├─ Public
│  ├─ Annotations and Interfaces  (annotation, interface)
│  └─ Classes, Enums, and Records (class, enum, record)
├─ Protected
│  ├─ Annotations and Interfaces
│  └─ Classes, Enums, and Records
├─ Package-private
│  ├─ Annotations and Interfaces
│  └─ Classes, Enums, and Records
└─ Private
   ├─ Annotations and Interfaces
   └─ Classes, Enums, and Records
```

All leaves inherit `ordering-rules: alpha` from the parent.

## Non-negotiable constraints

The Default Rule is rendered subject to the same global constraints that apply to any
configuration:

1. Compilation safety — declaration-order dependencies (initializer references, blank-final
   definite assignment, enum constant initializers, etc.) are honoured by the
   declaration-order dependency graph and override the visual ordering when they
   conflict. See [`declaration-order-dependencies.md`](declaration-order-dependencies.md).
2. Accessor co-location — when `keepAccessorsTogether` is enabled, JavaBean getter/setter
   pairs are kept adjacent inside their containing group; see
   [`sorting-algorythm.md`](sorting-algorythm.md).
3. Source of truth — when the YAML in `default-config.yml` and this document diverge,
   the YAML wins. Update the document.
