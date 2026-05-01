<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer Project White Paper

## Project goal

JHarmonizer is an IDE-independent Java source harmonization tool. It provides deterministic class/member layout, dependency-aware reordering, formatter/import cleanup, and check modes suitable for CI.

## Current capabilities

- Parses Java source with Spoon.
- Sorts top-level types and type members using YAML-defined group trees.
- Preserves modeled declaration-order dependencies so provider members stay before dependent members where the current dependency model can detect that relationship.
- Clusters JavaBean-style accessors when `keepAccessorsTogether` is enabled.
- Serializes the transformed model and delegates final formatting/import cleanup to Palantir Java Format.
- Supports file-level and type-level opt-out directives.
- Provides CLI and Maven plugin integrations.

## Supported execution modes

| Mode | CLI | Maven | Effect |
|---|---|---|---|
| Reorder | `reorder` | `jharmonizer:reorder` | Rewrite changed files in place. |
| Check all | `check-all` | `jharmonizer:check` | Report every non-conforming file without rewriting. |
| Check fast | `check-fast` | `jharmonizer:check-fast` | Stop at the first non-conforming file without rewriting. |

There is no separate formatter-only public mode in the current CLI or Maven plugin. Formatting is part of the reorder/check pipelines.

## Configuration

The embedded default YAML file is the baseline for behavior. Project-specific YAML files and wrapper parameters are overlays. Current root configuration areas are formatting, backup/statistics flags, header-line settings, top-level type ordering, and type-member ordering.

## Why this matters

JHarmonizer reduces noisy diffs, makes source layout repeatable across IDEs and machines, and gives teams a build-enforceable way to keep Java member organization consistent.
