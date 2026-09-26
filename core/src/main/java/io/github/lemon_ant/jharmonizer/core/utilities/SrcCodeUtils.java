// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provides helpers for working with raw source code text.
 */
@UtilityClass
public class SrcCodeUtils {

    /**
     * Counts line separators in a source slice, treating CRLF as one separator.
     * @param start first source offset
     * @param endExclusive end of the source slice
     * @param srcCode source code text
     * @return number of CR, LF, or CRLF separators in the slice
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int countLineSeparators(int start, int endExclusive, @NonNull String srcCode) {
        Objects.checkFromToIndex(start, endExclusive, srcCode.length());
        int count = 0;
        for (int offset = start; offset < endExclusive; offset++) {
            char character = srcCode.charAt(offset);
            if (character == '\r' || character == '\n' && (offset == start || srcCode.charAt(offset - 1) != '\r')) {
                count++;
            }
        }
        return count;
    }

    /**
     * Finds a fragment's exclusive end, excluding trailing {@linkplain Character#isWhitespace(char) whitespace}.
     *
     * @param start the first source index to inspect
     * @param end the inclusive last source index; {@code start - 1} denotes an empty range
     * @param srcCode the source code text to inspect
     * @return the index after the last non-whitespace character, or {@code start} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findFragmentEndExclusive(int start, int end, @NonNull String srcCode) {
        int contentEndExclusive = end + 1;
        Objects.checkFromToIndex(start, contentEndExclusive, srcCode.length());
        while (contentEndExclusive > start && Character.isWhitespace(srcCode.charAt(contentEndExclusive - 1))) {
            contentEndExclusive--;
        }
        return contentEndExclusive;
    }

    /**
     * Finds a fragment's first content position with its preceding spaces and tabs.
     * Other leading {@linkplain Character#isWhitespace(char) whitespace} is skipped.
     *
     * @param start the first source index to inspect
     * @param end the inclusive last source index; {@code start - 1} denotes an empty range
     * @param srcCode the source code text to inspect
     * @return the indentation start, possibly before {@code start}, or {@code end + 1} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findFragmentStartWithIndentation(int start, int end, @NonNull String srcCode) {
        Objects.checkFromToIndex(start, end + 1, srcCode.length());
        int indentationStart = start;
        for (int position = start; position <= end; position++) {
            char character = srcCode.charAt(position);
            if (!Character.isWhitespace(character)) {
                // Without a boundary in this range, the source position may omit an indentation prefix.
                return indentationStart == start ? findIndentationStart(start, srcCode) : indentationStart;
            }
            // Restart indentation after whitespace other than spaces/tabs, e.g. \n, \r, or \f.
            if (character != '\t' && character != ' ') {
                indentationStart = position + 1;
            }
        }
        return end + 1;
    }

    /**
     * Finds the first indentation character position for the source fragment.
     *
     * @param start the source index to scan backward from
     * @param srcCode the source code text to inspect
     * @return the indentation start offset for the fragment
     */
    public static int findIndentationStart(int start, @NonNull String srcCode) {
        int position = start - 1;
        while (position >= 0) {
            char character = srcCode.charAt(position);
            if (character != '\t' && character != ' ') {
                break;
            }
            position--;
        }
        return position + 1;
    }
}
