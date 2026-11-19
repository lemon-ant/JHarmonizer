<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Source printer

`SpoonParser` stores the original source, annotation fragments, and opt-outs in `SpoonAstModel`.
The flow retains its immutable `PrinterConfig` and passes it to `SrcAstTranslator.serialize(model, printerConfig)`.
The translator forwards the model and settings to the stateless `SpoonSrcPrinter`; no printer instance is stored.
The same parsed model can be serialized with different settings without reparsing or retaining settings in the model.
The printer uses Spoon declarations and source positions; all output code belongs to this project.
It neither inherits from Spoon printers nor regenerates declarations through `CtElement.toString()`.
Parsing and sorting do not depend on printer settings.

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
Effective starts include annotations preceding the member declaration range reported by Spoon. The reported start can
fall at a comment after those annotations. Top-level types, nested types, and members use the same
boundary calculation. It retains leading comments already included in the member range reported by Spoon;
it does not search comments independently.
Source-text helpers use half-open `[start, endExclusive)` ranges; the printer converts Spoon's inclusive source ends
before invoking them.
The output uses one `StringBuilder`, initially sized to the original source, and appends source ranges
directly. Interior line endings remain unchanged; generated lines use the dominant separator, with
CRLF, LF, CR tie precedence. The complete serialized source is not cached.

Each call creates a private `Serialization` with its own configuration reference, output buffer and range map.
Configuration, source text, annotation order, skipped types and the Spoon AST come from the supplied model;
the printer has no shared state. `SrcPrinterOutput` indexes each `AnnotationSrcGroup` by its original start.
At a group boundary it delegates text joining to `AnnotationSrcGroupRenderer`, passing the optional selected order from
`annotationOrderByGroupSrcStart`, and continues after the original group end. It does not decide separator ownership
or inspect original source to prepare gap text.
The same range-copying method handles fragments inside types and entire files without declared types, including
`package-info.java` and `module-info.java`, without adding whitespace to those files.
The standalone `AnnotationSrcGroup` model owns its nested annotation, gap, and private base-fragment types.
`AnnotationSourceScanner` collects `AnnotationSrcFragment` and `AnnotationSrcGap` in separate lists. Both extend
`SrcFragment` with text and original source bounds. The scanner alone constructs those source models. The group
constructor requires one gap per annotation and retains unmodifiable views; producers must not modify handed-off
lists. The scanner allocates fresh lists for each group. A private helper lazily interleaves the source-order lists,
and each group caches its original `replacementCode`. Original annotations, gaps, and source ranges never change.
Each gap stores original code, fixed content, and three scanner-prepared moved layouts, indexed by required separator
count zero, one, or two. A layout records whether the annotation's exact separators must be emitted and which leading
whitespace follows them. It shares fixed gap content instead of storing complete strings for every annotation/slot pair.
`SpoonAnnotationSorter` returns only changed orders of existing fragments. The renderer joins these fragments with
the selected layouts without source analysis, new gap/group instances, or comment-ownership decisions.
Ordinary whitespace and independent comment blocks stay in their slots. Prepared layouts supply separators required
by trailing line comments or attached lower comment blocks when the destination gap cannot retain them.
An annotation that stays in its original slot retains its unmodified gap, even when other group members move.
For example, the blank gap in `@B\n\n@A` stays between the sorted annotations; the terminator of `@B // note\n@A`
must also follow `@B` when it moves. These are separate ownership rules, so blindly concatenating annotations with
their original trailing whitespace would change the output.
Whether to emit an annotation's separators depends on its destination gap after sorting. In
`@A // note\r\n@B\n@C `, moving A to B's slot reuses the gap's `\n`; moving A to C's inline slot requires A's `\r\n`.
The scanner knows both fragments and gaps, but their final pairing is selected later. A single separator string per
annotation cannot represent both outcomes. Preparing complete strings for every possible annotation/slot pair would
require a table that grows quadratically with the group's size.
The original groups and separate order map determine annotation output order; AST annotation lists retain their parsed
order and provide annotation presence for spacing decisions and positions for declaration boundaries.
The result wraps the collected map once. Later calls cannot alter earlier output or ranges, and
calls for independent models may run concurrently with different configurations. Failed calls leave no shared state.

Spoon 11.5.0 provides correct member positions for the existing enum lambda/body regressions. The old
enum correction, which generated text and searched it with a regular expression, has been removed.
Spoon 11.5.0 reports `CommentedAnnotation` in printer scenario `21-annotation-group-gaps` as starting at a comment after
both annotations. `findEffectiveMemberStart` includes those earlier annotations and retains leading comments already
covered by the reported range. Recheck this correction after the next Spoon GA upgrade.
The fixture expectations are the compatibility reference, except for
the source-tail behavior described below.

The source tail starts after the greatest original end offset among top-level types, independently of
their output order. `printTail` skips the leading gap, retains indentation, and copies the remaining
source range once after one blank line. Comment text, internal spacing, trailing whitespace and the
presence or absence of a final line terminator remain unchanged. Whitespace-only tails add nothing.
File bounds and comment attachment reported by Spoon are not used, so virtual sources and comments adjacent
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
rejects fragment roots for `module-info.java` and `package-info.java` models, and lazily loads original text from the
backing file.
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
