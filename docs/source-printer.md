<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Source printer

`SpoonParser` stores the original source, annotation fragments, and immutable `PrinterConfig` in `SpoonAstModel`.
`SrcAstTranslator` passes the sorted model directly to the stateless `SpoonSrcPrinter`; no printer instance is stored.
The printer uses Spoon declarations and source positions; all output code belongs to this project.
It neither inherits from Spoon printers nor regenerates declarations through `CtElement.toString()`.
Parsing, sorting, formatting and import cleanup retain their existing entry points.

| Component | Responsibility |
| --- | --- |
| `SpoonSrcPrinter` | Stateless serialization entry point with explicit configuration |
| `SpoonSrcPrinter.Serialization` | Per-call state, declaration layout, member spacing and boundaries, skipped-type ranges |
| `SrcPrinterOutput` | Source slices and tails, annotation-group replacements, indentation, line separator detection, output offsets |
| `SpoonTypeMemberUtils` | Explicit members, effective source boundaries and comment attribution |
| `SrcCodeUtils` | Shared source-fragment boundary operations |
| `SpoonGroupSeparatorUtils` | Assigning separators and resolving their kind and header text |

Containers own separators. A type header, adjacent members and group metadata request one shared
blank line at each boundary. Method bodies, initializers, annotations and source comments are copied
as fragments. Nested types recurse through the same layout operation. Synthetic declarations remain
excluded; skipped types are copied as a whole and their UTF-16 output offsets are recorded.

Each type indexes its original member starts in a sorted `int[]`. Binary search finds the next source
boundary regardless of output order, including duplicate starts in multi-field declarations.
Effective starts include annotations preceding Spoon's declaration range: a comment between annotations can otherwise
make the range omit an earlier annotation. Top-level types, nested types, and members use the same boundary calculation.
The output uses one `StringBuilder`, initially sized to the original source, and appends source ranges
directly. Interior line endings remain unchanged; generated lines use the dominant separator, with
CRLF, LF, CR tie precedence. The complete serialized source is not cached.

Each call creates a private `Serialization` with its own configuration reference, output buffer and range map.
Configuration, source text, annotation order, skipped types and the compilation unit come from the supplied model;
the printer has no shared state. `SrcPrinterOutput` indexes each `AnnotationSrcGroup` by its original start.
At a group boundary it appends `replacementCode` and continues after the original group end. It does not inspect
annotation or gap fragments, decide separator ownership, or rebuild replacement text.
The same range-copying method handles fragments inside types and entire units without declared types, including
`package-info.java` and `module-info.java`, without adding whitespace to those units.
The standalone `AnnotationSrcGroup` model owns its nested annotation, gap, layout, and private base-fragment types.
`AnnotationSourceScanner` collects `AnnotationSrcFragment` and `AnnotationSrcGap` in separate lists. Both extend
`SrcFragment` with text and original source bounds. `SpoonAnnotationSorter` sorts annotations and resolves gap text in
the original source slots. Both producers pass their two typed lists to the group constructor, which requires one gap
per annotation and retains unmodifiable views. Producers must not modify handed-off lists; the scanner allocates fresh
lists for each group. A static helper lazily assembles and caches the mixed sequence by pairing entries at the same
index. The mixed sequence stays private and cannot be supplied independently. Each group lazily concatenates and caches
its own `replacementCode`. Its annotation list supports sorting and relocation detection; its gap list retains
slot order for subsequent sorting. Original fragment ranges remain unchanged even when a gap's text changes length.
Each gap stores only one complete code string. Its shared `AnnotationGapLayout` holds fixed content, exact and relocated
whitespace-prefix choices, destination indentation, and the available separator count. The scanner prepares all of
these components. Sorting compares boundaries and counts to select a prefix, which the gap combines with its fixed
content. It never reads or analyzes the original source, and later sorts reuse the same immutable layout.
Ordinary whitespace and independent comment blocks stay in their slots. The sorter supplies separators required by
trailing line comments or attached lower comment blocks when the destination gap cannot retain them.
An annotation that stays in its original slot retains its unmodified gap, even when other group members move.
For example, the blank gap in `@B\n\n@A` stays between the sorted annotations; the terminator of `@B // note\n@A`
must also follow `@B` when it moves. These are separate ownership rules, so blindly concatenating annotations with
their original trailing whitespace would change the output.
The groups alone determine annotation output order; AST annotation lists retain their parsed order and provide
annotation presence for spacing decisions and positions for declaration boundaries.
The result wraps the collected map once. Later calls cannot alter earlier output or ranges, and
calls for independent models may run concurrently with different configurations. Failed calls leave no shared state.

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

## Annotation scanning boundary

Reviewed on 2026-09-27 with Spoon 11.5.0 and JDK 21. Retain JDT token scanning as the source of annotation groups;
do not restrict scanning to positions obtained from Spoon's annotation nodes.

Spoon provides annotation lists, recursive element queries, and source positions through
[`CtElement`](https://github.com/INRIA/spoon/blob/v11.5.0/src/main/java/spoon/reflect/declaration/CtElement.java).
These APIs describe the modeled elements. They do not establish that every written annotation has a model node.
The existing `18-annotation-language-constructs/input/TypeUseAnnotationOrdering.java` fixture demonstrates the gap:
debugger logpoints in `SpoonParser` and `SrcAstTranslator` observed only six annotations in a recursive `CtAnnotation`
query of the main type, versus twelve lexical annotations within that type.

| Source location | Recursive Spoon query | Lexical scanner |
| --- | --- | --- |
| Generic argument, type parameter, and return type | 6 | 6 |
| Two array dimensions in `String @Z @A [] @D @C []` | 0 | 4 |
| Receiver parameter `@Z @A TypeUseAnnotationOrdering this` | 0 | 2 |

The four `@Target` annotations on the fixture's annotation declarations are outside the main type and excluded from
these counts. This result concerns the configured Spoon version and fixture, not a general absence of type-use support.

The original-source-fragment API also has limits:
[`CtCompilationUnitImpl`](https://github.com/INRIA/spoon/blob/v11.5.0/src/main/java/spoon/support/reflect/declaration/CtCompilationUnitImpl.java)
rejects fragment roots for module and package compilation units, and lazily loads original text from the backing file.
The scanner instead consumes the supplied source string, including virtual sources without a backing file.

`AnnotationSourceScanner` uses JDT's existing
[`IScanner`](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/compiler/IScanner.html)
token API, which supplies token kinds, original source positions, and token text with Unicode escapes decoded.
It does not build a second Java AST. Scanning also supplies lexical adjacency, written argument order, parentheses,
and comment/separator ownership needed by the sorting contract. Spoon remains responsible for declaration structure,
dependencies, opt-outs, and declaration boundaries.

Using AST annotations first would still require scanning the remaining source to find omitted groups and process gaps,
then reconciling both inventories. Keep one lexical inventory unless a measured alternative preserves the same output
and source ranges. Recheck this decision when upgrading Spoon or changing annotation discovery.

`SpoonParserTest.parseJavaSrcFile_arrayAndReceiverAnnotations_preservesLexicalSourceGroups` checks the exact group
bounds and source text for the omitted groups using the existing fixture. It does not require Spoon to keep omitting
them. The shared printer E2E runner checks full output, compilation, and repeated processing for annotation fixtures,
including comments, package/module declarations, records, and type uses.
