<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer: Java Source Harmonization Tool

## Overview

JHarmonizer keeps Java source layout deterministic by parsing source files, applying configurable member-ordering rules, preserving declaration-order constraints that the current dependency model understands, serializing the resulting Spoon model, and running a final formatter/import pass.

The codebase currently provides:

- `jharmonizer-core` — core parser, sorter, formatter, diff, opt-out, and source-processing flows;
- `jharmonizer-cli` — a standalone picocli fat JAR;
- `jharmonizer-maven-plugin` — Maven goals for rewrite and check flows;
- `dependency-aware-sorting` — reusable dependency-aware ordering utilities used by the core sorter.

## Main flows

| Flow | Public wrappers | Behavior |
|---|---|---|
| Reorder | CLI `reorder`, Maven `jharmonizer:reorder` | Rewrites changed files in place and optionally creates `.bak` backups. |
| Check all | CLI `check-all`, Maven `jharmonizer:check` | Reports all files that would change and leaves sources untouched. |
| Check fast | CLI `check-fast`, Maven `jharmonizer:check-fast` | Stops after the first ordering or formatting violation and leaves sources untouched. |

## Pipeline

1. Resolve the embedded default YAML configuration with any external overlay.
2. Discover Java files under the selected base directory using include/exclude globs.
3. Parse each source file with the Spoon-based translator.
4. Apply file/type opt-out directives.
5. Sort top-level types and type members according to the compiled group tree and dependency constraints.
6. Serialize the Spoon model back to Java source.
7. Run the configured formatter/import pass.
8. In check flows, log member relocations and/or formatting diffs; in reorder flow, write changed files.

## User-facing configuration

The supported YAML root keys are documented in [`config-dsl.md`](config-dsl.md). The embedded default configuration is the executable reference for default behavior: `core/src/main/resources/default-config.yml`.
