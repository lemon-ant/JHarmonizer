<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Changelog

All notable changes to JHarmonizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- Printer E2E tests for declarations, comments, string contents, headerless files, whitespace-only preambles, line separators, group boundaries, opt-out preservation, and idempotence.
- Printer buffer tests for empty output, missing line terminators, and existing LF/CRLF/CR endings.
- Printer coverage measurements, unreachable-branch notes, and a Maven/JaCoCo reproduction command.

### Changed

- Updated Palantir Java Formatter dependency from 2.91.0 to 2.98.0.
- Updated Spoon dependency from 11.2.1 to 11.5.0.
- Added scoped PMD suppressions for intentional reference-identity comparisons.
- Required cross-platform Java/Maven tooling and concise technical text in agent instructions.
- Run local source reordering in `validate` before PMD checks.
- Clarified prepared test state and one-time setup rules in agent instructions.

### Fixed

- Fixed duplicate blank lines in Spoon serialization before closing braces, between nested and top-level types, and at end of file. Declaration containers now own separators, combining header, member, and group spacing while preserving comments, opt-out fragments, and source line endings. Member source boundaries are indexed once, skipped-type offsets no longer copy the output buffer, and fragment indentation is retained without rescanning leading whitespace. Fragment-boundary detection is centralized in `SrcCodeUtils` with unit coverage for indentation, whitespace, and source ranges.
- Generated member-group comments now retain the following member's source indentation before formatting.

## [1.0.1] — 2026-05-07

### Added

- Core pipeline: parse → classify → sort → print → format.
- Declaration-order dependency graph for direct initializer/read-write scenarios.
- Accessor bundling (`keepAccessorsTogether` option).
- Cycle detection with relaxed forward-reference fallback.
- Comment-based opt-out directives: `@jharmonizer:fully-off`, `@jharmonizer:sort-off` at file and type scope.
- Configurable member grouping and ordering via YAML/JSON configuration DSL.
- Maven plugin (`jharmonizer-maven-plugin`) with `reorder`, `check`, and `check-fast` goals.
- CLI fat JAR with `reorder`, `check-all`, and `check-fast` commands.
- JMH sorting performance benchmark (activate with `-Pbenchmark-sort`).
- Support for Maven archetype template files (graceful skip on non-Java placeholders).
- `CONTRIBUTING.md` — contributor guide covering fork/clone workflow, environment setup, build commands, code style, and review process.
- `SECURITY.md` — vulnerability disclosure policy.
- `CHANGELOG.md` — this file.
- `docs/directives.md` — full opt-out directives reference.
- `docs/known-limitations.md` — known formatter edge cases and workarounds.
- `docs/benchmark.md` — JMH sorting benchmark usage.
- GitHub Actions Verify workflow (`.github/workflows/verify.yml`) that builds and tests every push and pull request.

[Unreleased]: https://github.com/lemon-ant/JHarmonizer/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/lemon-ant/JHarmonizer/releases/tag/v1.0.1
