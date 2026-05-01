<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# DiffReporter

`DiffReporter` is the current check-flow formatter-difference diagnostic utility. It compares the original source text with the formatted/reordered source text and returns an empty string when the two versions are identical.

## Output format

The reporter uses `java-diff-utils` to build unified hunks, then renders a compact terminal-oriented format:

- hunk headers are kept as `@@ -start,len +start,len @@` lines;
- file headers such as `--- a/path` and `+++ b/path` are intentionally omitted because callers already log the file path;
- every diff content line is rendered as `<prefix>|<content>`, where the prefix is `+`, `-`, or a space;
- whitespace is visualized on rendered lines: space as `·`, tab as `→→→→`, and end of line as `¶`;
- output is truncated to at most 3 hunks per file and at most 20 changed lines per hunk, with omission summaries for hidden content.

## Where it is used

`CHECK_ALL` computes both member relocations and a diff for every changed file. `CHECK_FAIL_FAST` first reports member relocations; if ordering is already valid but formatting changes are detected, it computes a diff and stops the pipeline after that file.
