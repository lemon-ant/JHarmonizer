// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.Objects;
import java.util.function.IntPredicate;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provides helpers for working with raw source code text.
 */
@UtilityClass
// Raw source-offset operations stay together; private scans share bounds checks and character policies.
@SuppressWarnings("PMD.TooManyMethods")
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
     * Finds the content's exclusive end, excluding trailing {@linkplain Character#isWhitespace(char) whitespace}.
     *
     * @param start the first source index to inspect
     * @param endExclusive the first source index after the range
     * @param srcCode the source code text to inspect
     * @return the index after the last non-whitespace character, or {@code start} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findContentEndExclusive(int start, int endExclusive, @NonNull String srcCode) {
        return skipCharactersBackward(start, endExclusive, srcCode, Character::isWhitespace);
    }

    /**
     * Finds the first content position with its preceding spaces and tabs.
     * Other leading {@linkplain Character#isWhitespace(char) whitespace} is skipped.
     *
     * @param start the first source index to inspect
     * @param endExclusive the first source index after the range
     * @param srcCode the source code text to inspect
     * @return the indentation start, possibly before {@code start}, or {@code endExclusive} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findContentStartWithIndentation(int start, int endExclusive, @NonNull String srcCode) {
        int contentStart = findFirstNonWhitespace(start, endExclusive, srcCode);
        // Empty gaps have no indentation to copy. Otherwise retain only adjacent spaces/tabs, even before start.
        // For "\f  text", this skips the form feed while keeping the two spaces before text.
        return contentStart == endExclusive ? endExclusive : findIndentationStart(contentStart, srcCode);
    }

    /**
     * Finds the first content position, skipping leading {@linkplain Character#isWhitespace(char) whitespace}.
     * @param start first source index to inspect
     * @param endExclusive first source index after the range
     * @param srcCode source code text
     * @return first non-whitespace offset, or {@code endExclusive} if the range has no content
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findFirstNonWhitespace(int start, int endExclusive, @NonNull String srcCode) {
        return skipCharactersForward(start, endExclusive, srcCode, Character::isWhitespace);
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
        return skipCharactersForward(start, endExclusive, srcCode, SrcCodeUtils::isIndentationCharacter);
    }

    /**
     * Finds the start of the adjacent run of spaces and tabs before a source offset.
     *
     * @param contentStart the source index to scan backward from
     * @param srcCode the source code text to inspect
     * @return first adjacent space or tab, or {@code contentStart} when none precedes it
     * @throws IndexOutOfBoundsException if the offset is outside the source text or its end boundary
     */
    public static int findIndentationStart(int contentStart, @NonNull String srcCode) {
        return skipCharactersBackward(0, contentStart, srcCode, SrcCodeUtils::isIndentationCharacter);
    }

    /**
     * Finds the first CR or LF in a source range.
     * @param start first source offset to inspect
     * @param endExclusive first source offset after the range
     * @param srcCode source code text
     * @return first line-separator offset, or {@code endExclusive} if none occurs in the range
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findLineEnd(int start, int endExclusive, @NonNull String srcCode) {
        return skipCharactersForward(start, endExclusive, srcCode, character -> !isLineSeparatorCharacter(character));
    }

    /**
     * Finds the source offset after the last CR or LF before a content position.
     * @param contentStart content offset or the end of the source text
     * @param srcCode source code text
     * @return offset after the preceding CR or LF, or zero if neither occurs before {@code contentStart}
     * @throws IndexOutOfBoundsException if the offset is outside the source text or its end boundary
     */
    public static int findLineStart(int contentStart, @NonNull String srcCode) {
        return skipCharactersBackward(0, contentStart, srcCode, character -> !isLineSeparatorCharacter(character));
    }

    /**
     * Finds the CR, LF, or CRLF separator ending immediately before a source offset.
     * @param srcOffset source offset to inspect backward from
     * @param srcCode source code text
     * @return separator start, or {@code srcOffset} if no separator immediately precedes it
     * @throws IndexOutOfBoundsException if the offset is outside the source text or its end boundary
     */
    public static int findPrecedingLineSeparatorStart(int srcOffset, @NonNull String srcCode) {
        Objects.checkFromToIndex(0, srcOffset, srcCode.length());
        if (srcOffset == 0) {
            return srcOffset;
        }
        int separatorStart = srcOffset - 1;
        char lastCharacter = srcCode.charAt(separatorStart);
        if (!isLineSeparatorCharacter(lastCharacter)) {
            return srcOffset;
        }
        if (lastCharacter == '\n' && separatorStart > 0 && srcCode.charAt(separatorStart - 1) == '\r') {
            separatorStart--;
        }
        return separatorStart;
    }

    /**
     * Finds the start of trailing CR and LF characters in a source range.
     * Spaces, tabs, and other whitespace stop the search and remain in the range.
     * @param start first source offset
     * @param endExclusive end of the source range
     * @param srcCode source code text
     * @return offset after the last character other than CR or LF, or {@code start} if none remains
     * @throws IndexOutOfBoundsException if the range is invalid for the source text
     */
    public static int findTrailingLineSeparatorsStart(int start, int endExclusive, @NonNull String srcCode) {
        return skipCharactersBackward(start, endExclusive, srcCode, SrcCodeUtils::isLineSeparatorCharacter);
    }

    private static boolean isIndentationCharacter(int character) {
        return character == ' ' || character == '\t';
    }

    private static boolean isLineSeparatorCharacter(int character) {
        return character == '\r' || character == '\n';
    }

    private static int skipCharactersBackward(
            int start, int endExclusive, String srcCode, IntPredicate characterPredicate) {
        Objects.checkFromToIndex(start, endExclusive, srcCode.length());
        int offset = endExclusive;
        while (offset > start && characterPredicate.test(srcCode.charAt(offset - 1))) {
            offset--;
        }
        return offset;
    }

    private static int skipCharactersForward(
            int start, int endExclusive, String srcCode, IntPredicate characterPredicate) {
        Objects.checkFromToIndex(start, endExclusive, srcCode.length());
        int offset = start;
        while (offset < endExclusive && characterPredicate.test(srcCode.charAt(offset))) {
            offset++;
        }
        return offset;
    }
}
