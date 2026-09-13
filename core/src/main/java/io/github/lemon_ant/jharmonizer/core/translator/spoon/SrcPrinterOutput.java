// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentEndExclusive;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentStartWithIndentation;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationStart;

import lombok.NonNull;

/** Owns one serialization buffer and copies source slices without intermediate strings. */
final class SrcPrinterOutput {

    @NonNull
    // Each invocation owns a fresh buffer; capacity cannot accumulate across files.
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder buffer;

    @NonNull
    private final String lineSeparator;

    @NonNull
    private final String srcCode;

    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * Creates an output buffer using the original source's size and dominant line separator.
     * @param srcCode original source
     */
    SrcPrinterOutput(@NonNull String srcCode) {
        this.srcCode = srcCode;
        lineSeparator = detectDominantLineSeparator(srcCode);
        buffer = new StringBuilder(srcCode.length());
    }

    /**
     * Returns the next output character offset.
     * @return current output length
     */
    int getPrintedLength() {
        return buffer.length();
    }

    /**
     * Copies a fragment with its indentation and one final line separator.
     * @param start first source offset
     * @param end inclusive final source offset; {@code start - 1} denotes an empty range
     * @return whether the fragment contained non-whitespace text
     */
    boolean printFragment(int start, int end) {
        try {
            int indentationStart = findFragmentStartWithIndentation(start, end, srcCode);
            if (indentationStart > end) {
                return false;
            }
            // Interior whitespace belongs to the fragment; surrounding gaps belong to its container.
            int fragmentEndExclusive = findFragmentEndExclusive(start, end, srcCode);
            buffer.append(srcCode, indentationStart, fragmentEndExclusive);
            writeln();
            return true;
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalStateException(
                    "Invalid source fragment range: start=" + start + ", end=" + end + ", sourceLength="
                            + srcCode.length(),
                    exception);
        }
    }

    /**
     * Prints a group comment with the following member's original indentation.
     * @param memberStart member's source offset
     * @param groupHeader comment content
     */
    void printGroupHeader(int memberStart, @NonNull String groupHeader) {
        buffer.append(srcCode, findIndentationStart(memberStart, srcCode), memberStart)
                .append("// ")
                .append(groupHeader);
        writeln();
    }

    /**
     * Copies a nonblank source tail after one blank line, preserving indentation and all remaining text.
     * @param start first source offset after the last original top-level type
     */
    void printTail(int start) {
        int tailStart = findFragmentStartWithIndentation(start, srcCode.length() - 1, srcCode);
        if (tailStart < srcCode.length()) {
            writeln();
            buffer.append(srcCode, tailStart, srcCode.length());
        }
    }

    /** Appends one detected line separator. */
    void writeln() {
        buffer.append(lineSeparator);
    }

    @NonNull
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidReassigningLoopVariables"})
    private static String detectDominantLineSeparator(String srcCode) {
        if (srcCode.isEmpty()) {
            return System.lineSeparator();
        }

        int crlfCount = 0;
        int lfCount = 0;
        int crCount = 0;

        for (int index = 0; index < srcCode.length(); index++) {
            char currentChar = srcCode.charAt(index);

            if (currentChar == '\r') {
                boolean hasNextChar = (index + 1) < srcCode.length();
                if (hasNextChar && srcCode.charAt(index + 1) == '\n') {
                    crlfCount++;
                    index++; // skip '\n' in CRLF
                } else {
                    crCount++; // classic Mac style: CR only
                }
                continue;
            }

            if (currentChar == '\n') {
                lfCount++; // Unix/macOS modern style: LF only
            }
        }

        return selectDominantLineSeparator(crlfCount, lfCount, crCount);
    }

    @NonNull
    private static String selectDominantLineSeparator(int crlfCount, int lfCount, int crCount) {
        if (crlfCount == 0 && lfCount == 0 && crCount == 0) {
            return System.lineSeparator();
        }

        // Ties preserve the established CRLF > LF > CR precedence.
        if (crlfCount >= lfCount && crlfCount >= crCount) {
            return "\r\n";
        }
        if (lfCount >= crCount) {
            return "\n";
        }
        return "\r";
    }
}
