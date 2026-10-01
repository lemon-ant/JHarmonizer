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
     * Finds the first content position, skipping leading {@linkplain Character#isWhitespace(char) whitespace}.
     * @param start first source index to inspect
     * @param end inclusive last source index; {@code start - 1} denotes an empty range
     * @param srcCode source code text
     * @return first non-whitespace offset, or {@code end + 1} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findFragmentStart(int start, int end, @NonNull String srcCode) {
        Objects.checkFromToIndex(start, end + 1, srcCode.length());
        int contentStart = start;
        while (contentStart <= end && Character.isWhitespace(srcCode.charAt(contentStart))) {
            contentStart++;
        }
        return contentStart;
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
     * Finds the end of a run of spaces and tabs within a source range.
     * @param start first indentation offset to inspect
     * @param endExclusive end of the source range
     * @param srcCode source code text
     * @return first offset that is not a space or tab, or {@code endExclusive} if the range contains only indentation
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findIndentationEnd(int start, int endExclusive, @NonNull String srcCode) {
        Objects.checkFromToIndex(start, endExclusive, srcCode.length());
        int indentationEnd = start;
        while (indentationEnd < endExclusive
                && (srcCode.charAt(indentationEnd) == ' ' || srcCode.charAt(indentationEnd) == '\t')) {
            indentationEnd++;
        }
        return indentationEnd;
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

    /**
     * Finds the content end of a source range, excluding only trailing CR and LF characters.
     * Spaces, tabs, and other whitespace remain part of the content.
     * @param start first source offset
     * @param endExclusive end of the source range
     * @param srcCode source code text
     * @return offset after the last character other than CR or LF, or {@code start} if none remains
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findLineContentEndExclusive(int start, int endExclusive, @NonNull String srcCode) {
        Objects.checkFromToIndex(start, endExclusive, srcCode.length());
        int contentEndExclusive = endExclusive;
        while (contentEndExclusive > start
                && (srcCode.charAt(contentEndExclusive - 1) == '\r'
                        || srcCode.charAt(contentEndExclusive - 1) == '\n')) {
            contentEndExclusive--;
        }
        return contentEndExclusive;
    }

    /**
     * Finds the CR, LF, or CRLF separator ending immediately before a source offset.
     * @param lineStart source offset immediately after the separator
     * @param srcCode source code text
     * @return separator start, or {@code lineStart} if no separator precedes it
     * @throws IndexOutOfBoundsException if the offset is outside the source text or its end boundary
     */
    public static int findLineSeparatorStart(int lineStart, @NonNull String srcCode) {
        Objects.checkFromToIndex(0, lineStart, srcCode.length());
        if (lineStart == 0) {
            return lineStart;
        }
        int separatorStart = lineStart - 1;
        char lastCharacter = srcCode.charAt(separatorStart);
        if (lastCharacter != '\r' && lastCharacter != '\n') {
            return lineStart;
        }
        if (lastCharacter == '\n' && separatorStart > 0 && srcCode.charAt(separatorStart - 1) == '\r') {
            separatorStart--;
        }
        return separatorStart;
    }

    /**
     * Finds the source offset after the last CR or LF before a content position.
     * @param contentStart content offset or the end of the source text
     * @param srcCode source code text
     * @return offset after the preceding CR or LF, or zero if neither occurs before {@code contentStart}
     * @throws IndexOutOfBoundsException if the offset is outside the source text or its end boundary
     */
    public static int findLineStart(int contentStart, @NonNull String srcCode) {
        Objects.checkFromToIndex(0, contentStart, srcCode.length());
        return Math.max(srcCode.lastIndexOf('\r', contentStart - 1), srcCode.lastIndexOf('\n', contentStart - 1)) + 1;
    }
}
