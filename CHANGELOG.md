<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Changelog

All notable changes to JHarmonizer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## 1.0.1 - unreleased

### Added

- Global annotation sorting by name, name length, complete declaration length, or argument text, with ascending criteria
  and ordered tie-breakers. Defaults to declaration length, name length, alphabetical name, then alphabetical argument
  text; an empty list preserves source order. Argument comparison includes parameter names and assignments, excluding
  comments and inter-token whitespace. Settings propagate through configuration overlays and compile into a reusable
  comparator.
- Source-fragment annotation sorting and printing for packages, modules, types, members, parameters, local variables,
  record components, and type uses, with Java-lexer coverage for annotations absent from Spoon's model,
  opt-out support, and compilation/idempotence regression fixtures.

### Changed

- Removed redundant collection copies and wrappers in dependency ordering and CLI process test results.
- Declaration-only relocation detection is private; callers use the model-based check that also detects annotation order.
- Annotation ordering models retain unmodifiable views of criteria without copying; callers must not mutate supplied lists.
- Annotation fragments retain only fields consumed by sorting, relocation detection, and printing; removed the unused
  annotation-only `endExclusive` offset and its calculation.
- Annotation descriptors use Lombok-generated constructors, with call sites aligned to field declaration order.
- Annotation order is computed only from source-fragment groups. Removed the duplicate AST scan, descriptor index,
  and annotation-list mutations; source printing uses the single sorted order.
- Annotation fragments and their sorted order now belong to `SpoonAstModel`, passed explicitly between parser, sorter,
  and printer. Removed annotation metadata from `CtCompilationUnit` and the serialization supplier from the model.
- Replaced the inherited Spoon source printer with standalone source-fragment printing, a direct output buffer, and
  indexed member boundaries. Preserved declaration output and skipped-type ranges; removed enum correction through Spoon
  pretty printing.
- On the four-workload JDK 21 corpus, mean printer serialization throughput reached 1.40–5.07× baseline across 1/2/4/8
  workers; sampled p50/p95/p99 decreased for every workload. Allocated bytes per batch decreased by 6.7–66.2% across the
  measured cases.
- Selected printer code decreased from 1000 to 701 physical lines and from 276 to 210 NCSS; summed Cyclo decreased from
  106 to 81, Cognitive from 87 to 59, and external library target types from 21 to 11.
- The source printer now copies the original tail after the last top-level type following one blank line. Preserves
  comment spelling, indentation, semicolons, trailing whitespace and the original file ending; removes separate footer
  rendering.
- Updated Palantir Java Formatter dependency from 2.91.0 to 2.98.0.
- Updated Spoon dependency from 11.2.1 to 11.5.0.

### Fixed

- Standalone module builds resolve shared license, PMD, and SpotBugs resources from the repository root through the
  tracked `.mvn` directory.
- Relocation detection includes source-fragment annotation order, including annotations absent from Spoon's AST.
  Check flows display annotation ordering diffs using the standard formatting-violation message. All flows classify
  annotation-only changes as reordering.
- Annotation scanning snapshots each group before clearing its reusable buffer, preserving annotations for sorting
  and relocation detection.
- Annotation fragments and replacements use named builders with private constructors, preventing source reordering
  from changing the mapping between constructor arguments and fields.
- Source printing includes annotations preceding Spoon's declaration start when a comment appears between annotations,
  preserving them during repeated processing of top-level types, nested types, and type members.
- Annotation sorting moves a standalone comment block with the preceding annotation when they are adjacent and a blank
  line separates the block from the next element. Moves that blank line with the block without leaving an extra blank
  line between annotations; separators around independent comment blocks remain in place.
- Annotation sorting moves adjacent leading line and block comments with their annotation. Blank lines separate comment
  blocks; declaration JavaDoc and file preambles retain their positions.
- `ARGUMENTS_ALPHA` distinguishes annotations without parentheses from empty argument lists, ordering `@Tag` before
  `@Tag()` and `@Tag("")`.
- Annotation sorting moves trailing same-line comments with their annotation and retains a line break after `//`
  comments when moving them into an inline annotation group.
- `DECLARATION_LENGTH_ASC` includes the written qualified annotation name and internal comments, alongside parameter
  names and values. Inter-token whitespace and comments between annotations remain excluded.
- Kept leading type comments and JavaDoc adjacent to their declarations in files without package/import declarations,
  including when top-level types are reordered.
- YAML loading rejects null collection entries, including annotation criteria, member groups, subgroups, and type groups,
  through the loader's exception contract. Nullable optional properties remain supported.
- Preserved footer comments after the last top-level type with trailing line terminators, virtual sources, and repeated
  processing, including reordered top-level declarations.
- Fixed duplicate blank lines in Spoon serialization before closing braces, between nested and top-level types, and at
  end of file. Declaration containers now own separators, combining header, member, and group spacing while preserving
  comments, opt-out fragments, and source line endings. Member source boundaries are indexed once, skipped-type offsets
  no longer copy the output buffer, and fragment indentation is retained without rescanning leading whitespace.
  Fragment-boundary detection is centralized in `SrcCodeUtils` with unit coverage for indentation, whitespace, and
  source ranges.
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
- `CONTRIBUTING.md` — contributor guide covering fork/clone workflow, environment setup, build commands, code style, and
  review process.
- `SECURITY.md` — vulnerability disclosure policy.
- `CHANGELOG.md` — this file.
- `docs/directives.md` — full opt-out directives reference.
- `docs/known-limitations.md` — known formatter edge cases and workarounds.
- `docs/benchmark.md` — JMH sorting benchmark usage.
- GitHub Actions Verify workflow (`.github/workflows/verify.yml`) that builds and tests every push and pull request.

[Unreleased]: https://github.com/lemon-ant/JHarmonizer/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/lemon-ant/JHarmonizer/releases/tag/v1.0.1
