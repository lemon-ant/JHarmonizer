<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Configurator

## Purpose

Build the runtime configuration consumed by the sorter and formatter, by:

- loading the embedded default configuration from a classpath resource,
- optionally overlaying a user-supplied YAML configuration on top,
- applying programmatic overrides from the CLI / Maven plugin layer,
- compiling the result into the runtime `CompiledConfig`.

There is **no** ingestion of other vendor configuration formats today. Adapters
for additional vendor formats (for example IntelliJ IDEA `codeStyleConfig.xml`,
Eclipse formatter profiles, EditorConfig, Spotless config) are a planned future
capability — see [Why a separate Unified layer](#why-a-separate-unified-layer)
and the corresponding entry in [`TODO.md`](TODO.md). The multi-layer model
described below is deliberately structured so that those adapters can be added
without changing the merge / compile logic.

## Pipeline

```
default-config.yml (classpath)                ← embedded baseline
        │
        ▼
JHarmonizerConfig (vendor strict model)
        │  JHarmonizer2UnifiedConverter
        ▼
UnifiedConfig (strict, vendor-independent)    ── baseline ──┐
                                                            │
external YAML (optional, user-supplied)                     │
        │                                                   │
        ▼                                                   │
JHarmonizerFlexibleConfig (vendor flexible)                 │
        │  JHarmonizerFlexible2FlexibleUnifiedConverter      │
        ▼                                                   │
FlexibleUnifiedConfig (overlay) ──── UnifiedConfigMerger ───┤
                                                            │
CLI / Maven param overrides → FlexibleUnifiedConfig ────────┤
                                                            ▼
                                                       UnifiedConfig (merged)
                                                            │
                                                            ▼
                                          Unified2CompiledModelCompiler
                                                            │
                                                            ▼
                                                       CompiledConfig
```

## Model layers

| Layer       | Purpose                                                                                  | Strict variant           | Flexible / overlay variant     |
|-------------|------------------------------------------------------------------------------------------|--------------------------|--------------------------------|
| Vendor      | 1:1 mirror of the YAML schema accepted by JHarmonizer                                    | `JHarmonizerConfig`      | `JHarmonizerFlexibleConfig`    |
| Unified     | Strongly-typed, vendor-independent model used as the merge boundary                       | `UnifiedConfig`          | `FlexibleUnifiedConfig`        |
| Compiled    | Runtime model with predicate-based selectors and post-order group indexes                 | `CompiledConfig`         | _n/a_                          |

The flexible variants allow every field to be `null`, which is what makes them safe to use
as overlays. The strict variants reject missing required fields.

### Why a separate Unified layer

The Unified layer is the architectural extension point for supporting other vendor
configuration formats. Today only one vendor format exists (the JHarmonizer YAML
schema), so the Vendor → Unified converter is effectively an identity mapping. The
intent, however, is that adding support for another vendor (for example an
IntelliJ IDEA `codeStyleConfig.xml`, an Eclipse formatter profile, an EditorConfig
file, or a Spotless config) means writing a single component:

- a vendor-specific loader / deserializer for that format, plus
- a vendor-specific `<Vendor>2UnifiedConverter` that maps it onto `UnifiedConfig`.

Once a vendor adapter produces a `UnifiedConfig` (or a `FlexibleUnifiedConfig`
overlay), the rest of the pipeline — overlay merging, compilation, sorter/formatter
consumption — works unchanged, because the entire downstream toolset operates on
the unified model. In other words, the Unified layer is what keeps the merge and
compile machinery vendor-agnostic.

### Why a separate Compiled layer

The Compiled layer exists for **performance**. `UnifiedConfig` is a literal
description of the rules a user wrote; `CompiledConfig` precomputes everything the
hot path needs:

- each rule line in `includes`/`excludes` is compiled into a single boolean
  predicate over `MemberDescriptor`, so member dispatch is one virtual call and a
  predicate evaluation per rule;
- the member-group tree is laid out in DFS post-order with stable indexes so
  first-match-wins lookups are O(depth) per member;
- the formatter style, header line, and other scalar settings are pinned into
  immutable fields, so no string parsing happens per file.

Today the precomputation covers the parts that dominate per-file cost; future work
may move additional logic into the compiled layer, but the principle is the same —
all heavy interpretation of the rules happens once, at construction time, not
per file.

## Loading entry points

`JHarmonizerConfigLoader` (package-private, called via the manager facade):

- `loadDefault()` — reads `/default-config.yml` from the classpath.
- `loadFrom(InputStream)` / `loadFrom(File)` / `loadFromClasspathResource(URL)` — strict load.
- `loadFlexibleFrom(InputStream)` / `loadFlexibleFrom(File)` / `loadFlexibleFromClasspathResource(URL)` — overlay load.

`JHarmonizerConfigurationManager` is the public facade and exposes:

- `parseUnifiedDefaultConfig()` — returns the embedded baseline as `UnifiedConfig`.
- `parseUnifiedConfigFromClasspathResource(URL)` — returns a strict unified config from the classpath.
- `parseFlexibleUnifiedConfigFromClasspathResource(URL)` — returns a flexible (overlay) unified config from the classpath.
- `parseFlexibleUnifiedConfigFromFile(Path)` — returns a flexible (overlay) unified config from disk.

The manager class is marked with a `// TODO Merge with JHarmonizerConfigLoader`. Treat the
two as a single subsystem.

## Merging

Overlays are merged with `UnifiedConfigMerger.merge(baseline, overlay)`. Behavior:

- Scalar overlay fields that are `null` are inherited from the baseline; non-null values
  override the baseline.
- `top-level-types-ordering` and `formatting`/`header-line` blocks are merged field-by-field
  with the same null-means-inherit rule.
- `type-members-ordering` is merged at the **root** level by `name`:
  - matching custom root group fully replaces the default root group, keeping the original
    default position;
  - new custom root groups are inserted before all default root groups, keeping their
    relative order from the overlay;
  - nested `groups:` subtrees are not merged — replacing a root group replaces its whole
    subtree.

## Programmatic overrides

`AbstractJHarmonizerMojo` and `BaseCommand` (CLI) build a `FlexibleUnifiedConfig` from
their own command-line / Mojo parameters and merge it on top of the file-loaded overlay:

| Parameter (Mojo / CLI)                                         | Field overridden in overlay  |
|----------------------------------------------------------------|------------------------------|
| `jharmonizer.backupsEnabled` / `--no-backup`                   | `backupsEnabled`             |
| `jharmonizer.processingStatisticsMode` / `--statistics-mode`   | `processingStatisticsMode`   |

When neither file overlay nor parameter overlay is present, the embedded default is used
as-is.

## Compilation

`Unified2CompiledModelCompiler.compile(UnifiedConfig)` produces `CompiledConfig`:

- `MemberGroupCompiler.compileTopLevelGroups(...)` builds a DFS-ordered tree of
  `CompiledMemberGroup` nodes and assigns post-order indexes. Each rule line in
  `includes`/`excludes` is compiled into a single predicate over `MemberDescriptor`.
- `TopLevelTypesOrderingCompiler.compileTopLevelTypesOrdering(...)` compiles the
  top-level types ordering rules.
- `formatting`, `backupsEnabled`, `processingStatisticsMode`, and `headerLine` are passed
  through unchanged.

`CompiledConfig` is the immutable runtime model handed to the rest of the pipeline.

## Reference

For user-facing reconfiguration workflows, see [`reconfiguration.md`](reconfiguration.md).
For the YAML schema accepted by the loader, see [`config-dsl.md`](config-dsl.md).
