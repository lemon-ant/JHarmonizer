<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Source printer

`SpoonParser` retains one reusable `SpoonSrcPrinter` in the serialization supplier of each model.
The printer uses Spoon declarations and source positions; all output code belongs to this project.
It neither inherits from Spoon printers nor regenerates declarations through `CtElement.toString()`.
Parsing, sorting, formatting and import cleanup retain their existing entry points.

| Component | Responsibility |
| --- | --- |
| `SpoonSrcPrinter` | Immutable configuration and independent serialization calls |
| `SpoonSrcPrinter.Serialization` | Per-call state, declaration layout, member spacing and boundaries, skipped-type ranges |
| `SrcPrinterOutput` | Source slices and tails, indentation, dominant line separator detection, output offsets |
| `SpoonTypeMemberUtils` | Explicit members, effective ends and comment attribution |
| `SrcCodeUtils` | Shared source-fragment boundary operations |
| `SpoonGroupSeparatorUtils` | Assigning separators and resolving their kind and header text |

Containers own separators. A type header, adjacent members and group metadata request one shared
blank line at each boundary. Method bodies, initializers, annotations and source comments are copied
as fragments. Nested types recurse through the same layout operation. Synthetic declarations remain
excluded; skipped types are copied as a whole and their UTF-16 output offsets are recorded.

Each type indexes its original member starts in a sorted `int[]`. Binary search finds the next source
boundary regardless of output order, including duplicate starts in multi-field declarations.
The output uses one `StringBuilder`, initially sized to the original source, and appends source ranges
directly. Interior line endings remain unchanged; generated lines use the dominant separator, with
CRLF, LF, CR tie precedence. No serialized result is cached.

Each call creates a private `Serialization` with its own output buffer and range map. The printer
retains only `PrinterConfig`; source text, skipped types and compilation units are call arguments.
The result wraps the collected map once. Later calls cannot alter earlier output or ranges, and
failed calls leave no state in the service. The service retains no source text or buffer capacity between calls.

Spoon 11.5.0 provides correct member positions for the existing enum lambda/body regressions. The old
enum correction, which generated text and searched it with a regular expression, has been removed.
The fixture expectations are the compatibility reference, except for
the source-tail behavior described below.

The source tail starts after the greatest original end offset among top-level types, independently of
their output order. `printTail` skips the leading gap, retains indentation, and copies the remaining
source range once after one blank line. Comment text, internal spacing, trailing whitespace and the
presence or absence of a final line terminator remain unchanged. Whitespace-only tails add nothing.
Compilation-unit bounds and comment attachment are not used, so virtual sources and comments adjacent
to the closing brace follow the same rule. A Spoon type range can already include a trailing block
comment beyond its body end; that comment stays in the copied type range. Starting the tail at the
body end would duplicate it. Repeated processing preserves this boundary.

The tail can also contain empty top-level declarations (`;`), allowed by
[JLS 7.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.6), or a terminal ASCII SUB
character, allowed by [JLS 3.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.5).
Copying through the source end preserves these forms, including Unicode escapes, without parsing them again.
This intentionally replaces normalized footer rendering with source preservation before optional formatting.

`SpoonGroupSeparatorUtils` in `core.spoon` keeps the metadata key and blank-line marker private.
Callers assign headers with `markHeader`, assign blank lines with `markBlankLine`, and read once
with `resolveSeparator`. Its immutable `GroupSeparator` contains a `UnifiedSeparator` kind
(`NONE`, `NEW_LINE`, `HEADER`) and nullable `headerText`, present only for `HEADER`.
The `NONE` and `NEW_LINE` values reuse shared instances. Raw metadata and text classification
stay inside the utility. Empty headers and unmodified header text retain their existing behavior.

`GroupBoundaryMarker` selects the first member and interprets group configuration. The printer
owns comment-position checks, duplicate-header detection and combined spacing decisions.
