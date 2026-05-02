<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Opt-out directives

JHarmonizer supports two opt-out directives placed as source comments.

## Directive tokens

| Directive | Effect |
|---|---|
| `@jharmonizer:fully-off` | Disable all harmonization (sorting, formatting, import fixing) |
| `@jharmonizer:sort-off` | Disable sorting only; formatting and import fixing still run |

Directive matching is case-insensitive, so variants such as `@JHarmonizer:Fully-Off` are also recognized.

## Supported comment forms

```java
// @jharmonizer:fully-off
// @jharmonizer:sort-off
/* @jharmonizer:fully-off */
/* @jharmonizer:sort-off */
```

## Supported scopes

### File scope

Place a directive in the compilation-unit preamble:

- before `package`
- between `package` and the first `import`
- between `import` statements and the first top-level type
- at the beginning of a file with no `package` declaration

Behavior:

- `@jharmonizer:fully-off` — fully disable harmonization for the entire file (no sorting, no formatting, no import fixing)
- `@jharmonizer:sort-off` — disable sorting only for the entire file; formatting and import fixing still run

### Type scope

Place a directive immediately before a top-level or nested type declaration.

Behavior:

- `@jharmonizer:fully-off` — fully disable harmonization for that type subtree and preserve its original source text
- `@jharmonizer:sort-off` — disable sorting only for that type subtree; it is still formatted together with the rest of the file

## Unsupported placements and tokens

The following are intentionally not supported and are ignored:

- member-level directives for fields, methods, constructors, or initializer blocks
- region-based `off/on` markers
- any token other than `@jharmonizer:fully-off` and `@jharmonizer:sort-off`
- directives inside method bodies or inside Javadoc

Warnings are logged for the following file-scope cases: unrecognized directive tokens,
Javadoc comments containing a directive token (`/** @jharmonizer:... */`),
malformed directives where the token is not at the start of the comment payload, and multiple
file-scope directives in the same file (the last one wins).
For type-scope, parsing is silent for unrecognized or inapplicable comments; the only
type-scope warning is emitted when a single type has multiple applicable opt-out directives
(the last one wins).
