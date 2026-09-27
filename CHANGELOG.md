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

- Centralized annotation scanner whitespace and line-boundary operations in `SrcCodeUtils`, preserving source offsets,
  comment whitespace, and CR/LF/CRLF handling.
- Moved shared annotation source models into standalone `AnnotationSrcGroup`, with nested annotation, gap, layout,
  and base-fragment types. `AnnotationSourceScanner` retains source parsing and fragment preparation.
- Made the annotation group's common fragment base, mixed-sequence getter, and fixed gap-content getter private.
  Typed annotation and gap lists remain available to sorting; printing consumes the prepared replacement code.
- Annotation and gap fragments share source text and original ranges through `SrcFragment`. Sorting determines
  annotation order and gap text. Groups retain unmodifiable views of both typed lists, require one gap per annotation,
  and lazily interleave the mixed sequence through a static helper. Callers must not mutate handed-off lists.
  `annotationSrcFragments` and internal `srcFragments` retain the group's current order. Each group lazily
  caches its mixed sequence and `replacementCode`; independent group builder inputs are removed. Printing replaces
  the original range with that block without inspecting fragment types or reconstructing annotation order.
- Annotation gaps retain one ready-to-emit code string. The scanner prepares fixed content, whitespace-prefix choices,
  and separator counts in a shared immutable layout. Sorting selects a prefix without reading or analyzing source
  text, preserving repeated sorting, exact unchanged gaps, and comment attachment.
- Merged annotation-group replacement into `SrcPrinterOutput`; declaration fragments and units without declared types
  share the same source-range copying method.
- Removed redundant collection copies and wrappers in dependency ordering and CLI process test results.
- Declaration-only relocation detection is private; callers use the model-based check that also detects annotation order.
- Annotation ordering models retain unmodifiable views of criteria without copying; callers must not mutate supplied lists.
- Annotation fragments keep sorting keys, prepared text, the annotation offset, and required trailing separators.
  Gap preparation uses a computed blank-line requirement instead of interpreting a separator-count value.
  Parsing-only comment and token offsets stay private in the scanner.
- Annotation descriptors use Lombok-generated constructors. Annotation fragment builders initialize inherited ranges.
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
- Annotation scanning hands off independent lists for each group and starts fresh lists for the next group, preserving
  annotations for sorting and relocation detection without collection copies.
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
