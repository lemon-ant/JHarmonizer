<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# DiffReporter

## Purpose

`io.github.lemon_ant.jharmonizer.core.diff.DiffReporter` computes a human-readable
unified diff between the original and rewritten source text of a single file. It is
used by the `CHECK_*` flows and by the `REORDER` flow when verbose diagnostics for
non-conforming files are needed.

## Implementation

- Backed by [`java-diff-utils`](https://github.com/java-diff-utils/java-diff-utils)
  (`DiffUtils`, `UnifiedDiffUtils`, `Patch<String>`).
- Compares texts line-by-line.
- Returns an empty string when the texts are identical.

## Output format

Git-style unified diff with a JHarmonizer-specific presentation:

- Hunk headers on their own line: `@@ -start,len +start,len @@`.
- A `|` separator between the diff prefix (`+`, `-`, or space) and the line content.
  This makes the prefix unambiguous when the source itself starts with `+`/`-`.
- File header lines (`--- a/<path>`, `+++ b/<path>`) are **omitted**. The caller
  prints the file path itself before the diff body.
- Whitespace is visualised inside changed and context lines:
  - space → `·`
  - tab → `→→→→`
  - end-of-line → `¶`
- Context size is fixed at 3 lines.
- Output is truncated for readability:
  - at most 3 hunks per file;
  - at most 20 changed lines per hunk;
  - omitted content is replaced with a `... and N more` marker.

## API

```java
public static String computeDiff(String filePath, String originalText, String revisedText);
```

Returns the formatted diff or `""` when the texts are equal. The `filePath` is only
used by the underlying diff library for internal labelling and does not appear in the
returned text.

## Related

- `FormattingViolationPrinter` consumes `computeDiff(...)` to render per-file
  violation reports during `CHECK_*` flows.
- `MemberRelocationPrinter` is a separate component that prints member-relocation
  diagnostics computed by `RelocationDetector` (see [`sorting-algorythm.md`](sorting-algorythm.md));
  it does not produce textual diffs.
