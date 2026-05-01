<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer Architecture

## Modules

| Module | Responsibility |
|---|---|
| `dependency-aware-sorting` | Generic ordering utilities for applying desired order while respecting dependency constraints. |
| `core` | Configuration, source discovery, Spoon translation, sorting, serialization, formatting, diffs, opt-outs, and processing statistics. |
| `cli` | picocli command-line wrapper around `core`. |
| `maven-plugin` | Maven goal wrapper around `core`. |

## Core pipeline

```text
configuration overlay
    ↓
CompiledConfig
    ↓
source discovery by base directory and globs
    ↓
Spoon parse + opt-out metadata
    ↓
member/top-level-type sorting
    ↓
Spoon serialization
    ↓
formatter/import pass
    ↓
flow result and statistics
```

## Flow architecture

`SrcProcessor` selects an `IFlow` implementation from `FlowType`:

- `ReorderFlow` writes changed files and optionally creates backups;
- `CheckAllFlow` collects diagnostics for all changed files;
- `CheckFailFastFlow` requests stream stop after the first violation.

All flows share common opt-out handling, sorting/serialization helpers, formatting fallback behavior, and per-file error isolation through `AbstractOptOutFlow`.

## Diagnostics

Ordering diagnostics are reported as member relocations computed from the original member-order snapshot and the sorted Spoon model. Formatting-only diagnostics are reported through `DiffReporter`, which emits compact unified hunks with whitespace visualization and truncation.
