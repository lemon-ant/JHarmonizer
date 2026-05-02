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

There is **no** IDE-format ingestion (no `.editorconfig`, no `.idea/*.xml`, no Eclipse XML)
in the current implementation.

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

| Parameter (Mojo / CLI)                                               | Field overridden in overlay         |
|----------------------------------------------------------------------|-------------------------------------|
| `jharmonizer.backupsEnabled` / `--no-backup`                         | `backupsEnabled`                    |
| `jharmonizer.printProcessingStatistics` / `--no-statistics`          | `printProcessingStatistics`         |

When neither file overlay nor parameter overlay is present, the embedded default is used
as-is.

## Compilation

`Unified2CompiledModelCompiler.compile(UnifiedConfig)` produces `CompiledConfig`:

- `MemberGroupCompiler.compileTopLevelGroups(...)` builds a DFS-ordered tree of
  `CompiledMemberGroup` nodes and assigns post-order indexes. Each rule line in
  `includes`/`excludes` is compiled into a single predicate over `MemberDescriptor`.
- `TopLevelTypesOrderingCompiler.compileTopLevelTypesOrdering(...)` compiles the
  top-level types ordering rules.
- `formatting`, `backupsEnabled`, `printProcessingStatistics`, and `headerLine` are passed
  through unchanged.

`CompiledConfig` is the immutable runtime model handed to the rest of the pipeline.

## Reference

For the YAML schema accepted by the loader, see [`config-dsl.md`](config-dsl.md).
