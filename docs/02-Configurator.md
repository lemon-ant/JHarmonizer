<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Configurator

## Purpose

The configuration layer loads the embedded JHarmonizer YAML defaults, parses optional external YAML overlays, merges them, and converts the result into the compiled model consumed by the sorter, formatter, and processor.

## Current configuration sources

| Source | Used by | Behavior |
|---|---|---|
| Embedded `default-config.yml` | Core, CLI, Maven plugin | Always loaded as the baseline. |
| YAML file passed to CLI `--config` | CLI | Parsed as a flexible overlay and merged over defaults. |
| Maven `configFile` / `jharmonizer.configFile` | Maven plugin | Defaults to `${project.basedir}/jharmonizer.yml`; if the file does not exist, no file overlay is applied. |
| Programmatic `FlexibleUnifiedConfig` | Core constructor and wrappers | Optional direct overlay. |
| CLI/Maven booleans | CLI, Maven plugin | Override selected config values such as backups/statistics output. |

IDE configuration parsers and XML project configuration are not part of the current implementation.

## Main components

| Component | Responsibility |
|---|---|
| `JHarmonizerConfigLoader` | Reads YAML from files/resources into strict or flexible input models. |
| `JHarmonizerConfigurationManager` | Loads default/flexible configs and exposes parse helpers. |
| `ConfigurationManager` | Builds default or overridden `CompiledConfig` instances. |
| `UnifiedConfigMerger` | Merges strict/flexible unified configuration objects. |
| `JHarmonizer2UnifiedConverter` and flexible converter | Convert YAML input models into unified config models. |

## Merge semantics

External root groups from `type-members-ordering` are merged by exact root-group `name`:

1. A matching external root group fully replaces the default root group with the same name.
2. The replacement keeps the matched default group's original position.
3. New external root groups are inserted before the remaining default root groups.
4. Multiple new external root groups preserve their order from the external file.
5. Nested `groups:` blocks are not merged recursively.

Scalar and formatting options are normal overlay values: an explicitly provided external value replaces the baseline value, while an omitted field keeps the baseline value.

## Nested group inheritance

Inside a single `type-members-ordering` tree, nested groups inherit these options from the nearest parent that defines them:

- `keepAccessorsTogether`
- `separator`
- `ordering-rules`
- `relaxedForwardReferences`

A child value replaces the inherited value for that child subtree. For `ordering-rules`, replacement is list-based; an explicit empty list is allowed and means no explicit ordering keys at that level.
