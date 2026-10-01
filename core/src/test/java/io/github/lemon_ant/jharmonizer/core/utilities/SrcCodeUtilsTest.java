// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentEndExclusive;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentStartWithIndentation;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationEnd;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineContentEndExclusive;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineSeparatorStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SrcCodeUtilsTest {

    @NonNull
    private static Stream<Arguments> provideContentCharacters() {
        return Stream.of(
                Arguments.of("ordinary letter", 'x'),
                Arguments.of("closing brace", '}'),
                Arguments.of("comment start", '/'),
                Arguments.of("non-breaking space U+00A0", '\u00A0'),
                Arguments.of("figure space U+2007", '\u2007'),
                Arguments.of("narrow non-breaking space U+202F", '\u202F'),
                Arguments.of("next line U+0085", '\u0085'),
                Arguments.of("zero width space U+200B", '\u200B'),
                Arguments.of("byte order mark U+FEFF", '\uFEFF'));
    }

    @NonNull
    private static Stream<Arguments> provideInvalidRanges() {
        return Stream.of(
                Arguments.of("negative start", -1, 0),
                Arguments.of("end before an empty range", 0, -2),
                Arguments.of("reversed range", 3, 1),
                Arguments.of("end beyond source", 0, 4),
                Arguments.of("empty range beyond EOF", 5, 4),
                Arguments.of("end index overflow", 0, Integer.MAX_VALUE));
    }

    @NonNull
    private static Stream<Arguments> provideWhitespaceBoundaries() {
        return Stream.of(
                Arguments.of("LF", "\n"),
                Arguments.of("CR", "\r"),
                Arguments.of("CRLF", "\r\n"),
                Arguments.of("vertical tab U+000B", "\u000B"),
                Arguments.of("form feed U+000C", "\f"),
                Arguments.of("file separator U+001C", "\u001C"),
                Arguments.of("group separator U+001D", "\u001D"),
                Arguments.of("record separator U+001E", "\u001E"),
                Arguments.of("unit separator U+001F", "\u001F"),
                Arguments.of("ogham space U+1680", "\u1680"),
                Arguments.of("en quad U+2000", "\u2000"),
                Arguments.of("em quad U+2001", "\u2001"),
                Arguments.of("en space U+2002", "\u2002"),
                Arguments.of("em space U+2003", "\u2003"),
                Arguments.of("three-per-em space U+2004", "\u2004"),
                Arguments.of("four-per-em space U+2005", "\u2005"),
                Arguments.of("six-per-em space U+2006", "\u2006"),
                Arguments.of("punctuation space U+2008", "\u2008"),
                Arguments.of("thin space U+2009", "\u2009"),
                Arguments.of("hair space U+200A", "\u200A"),
                Arguments.of("line separator U+2028", "\u2028"),
                Arguments.of("paragraph separator U+2029", "\u2029"),
                Arguments.of("medium mathematical space U+205F", "\u205F"),
                Arguments.of("ideographic space U+3000", "\u3000"),
                Arguments.of("multiple boundaries with intermediate indentation", "\r\n \t\f\n"));
    }

    @Nested
    class FindFragmentEndExclusive {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideContentRanges")
        void findFragmentEndExclusive_contentInRange_returnsEndAfterLastContent(
                @NonNull String scenario, @NonNull String srcCode, int start, int end, int expectedEndExclusive) {
            // When
            int fragmentEndExclusive = findFragmentEndExclusive(start, end, srcCode);

            // Then
            assertThat(fragmentEndExclusive).as(scenario).isEqualTo(expectedEndExclusive);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideInvalidRanges")
        void findFragmentEndExclusive_invalidRange_throwsIndexOutOfBoundsException(
                @NonNull String scenario, int start, int end) {
            // When / Then
            assertThatThrownBy(() -> findFragmentEndExclusive(start, end, "text"))
                    .as(scenario)
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideContentCharacters")
        void findFragmentEndExclusive_nonWhitespaceCharacter_stopsSkipping(@NonNull String scenario, char character) {
            // Given
            String expectedFragment = "text" + character;
            String srcCode = expectedFragment + " \n\t";

            // When
            int fragmentEndExclusive = findFragmentEndExclusive(0, srcCode.length() - 1, srcCode);

            // Then
            assertThat(fragmentEndExclusive).as(scenario).isEqualTo(5);
            assertThat(srcCode.substring(0, fragmentEndExclusive)).isEqualTo(expectedFragment);
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void findFragmentEndExclusive_nullSource_throwsNullPointerException() {
            // When / Then
            assertThatThrownBy(() -> findFragmentEndExclusive(0, -1, null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideRangesWithoutContent")
        void findFragmentEndExclusive_rangeWithoutContent_returnsStart(
                @NonNull String scenario, @NonNull String srcCode, int start, int end, int expectedEndExclusive) {
            // When
            int fragmentEndExclusive = findFragmentEndExclusive(start, end, srcCode);

            // Then
            assertThat(fragmentEndExclusive).as(scenario).isEqualTo(expectedEndExclusive);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideWhitespaceBoundaries")
        void findFragmentEndExclusive_trailingWhitespace_excludesWhitespace(
                @NonNull String scenario, @NonNull String whitespace) {
            // Given / When
            String srcCode = " \ttext" + whitespace + " \t";
            int fragmentEndExclusive = findFragmentEndExclusive(0, srcCode.length() - 1, srcCode);

            // Then
            assertThat(fragmentEndExclusive).as(scenario).isEqualTo(6);
            assertThat(srcCode.substring(0, fragmentEndExclusive)).isEqualTo(" \ttext");
        }

        @NonNull
        private static Stream<Arguments> provideContentRanges() {
            return Stream.of(
                    Arguments.of("content without trailing whitespace", "text", 0, 3, 4),
                    Arguments.of("one-character range includes its end", "x", 0, 0, 1),
                    Arguments.of("trailing spaces", "text   ", 0, 6, 4),
                    Arguments.of("trailing tabs", "text\t\t", 0, 5, 4),
                    Arguments.of("mixed trailing spaces and tabs", "text \t ", 0, 6, 4),
                    Arguments.of("leading indentation is retained", " \ttext \t", 0, 7, 6),
                    Arguments.of("interior line break and indentation are retained", "x\n  y \n", 0, 6, 5),
                    Arguments.of("start inside content", "text", 2, 3, 4),
                    Arguments.of("content at the lower bound", "ignoredx \t", 7, 9, 8),
                    Arguments.of("end inside content", "text", 0, 1, 2),
                    Arguments.of("content after the range is ignored", "x \ty", 0, 2, 1),
                    Arguments.of("end inside trailing whitespace", "text  ignored", 0, 5, 4),
                    Arguments.of("content at the inclusive end", " \tx ignored", 0, 2, 3),
                    Arguments.of("last content in a multiline fragment", "x\n  y", 0, 4, 5));
        }

        @NonNull
        private static Stream<Arguments> provideRangesWithoutContent() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, -1, 0),
                    Arguments.of("empty range before content", "text", 0, -1, 0),
                    Arguments.of("empty range inside source", "text", 2, 1, 2),
                    Arguments.of("empty range at EOF", "text", 4, 3, 4),
                    Arguments.of("spaces only", "   ", 0, 2, 0),
                    Arguments.of("tabs only", "\t\t", 0, 1, 0),
                    Arguments.of("mixed whitespace only", " \t\r\n\f\u2003", 0, 5, 0),
                    Arguments.of("content exists only after the range", " \ttext", 0, 1, 0),
                    Arguments.of("content exists only before the range", "x \t", 1, 2, 1),
                    Arguments.of("whitespace before start stays outside the range", " \t ", 1, 2, 1));
        }
    }

    @Nested
    class FindFragmentStart {

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideInvalidRanges")
        void findFragmentStart_invalidRange_throwsIndexOutOfBoundsException(
                @NonNull String scenario, int start, int end) {
            assertThatThrownBy(() -> findFragmentStart(start, end, "text"))
                    .as(scenario)
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSourceRanges")
        void findFragmentStart_sourceRange_returnsFirstContentOffset(
                @NonNull String scenario, @NonNull String srcCode, int start, int end, int expectedStart) {
            assertThat(findFragmentStart(start, end, srcCode)).as(scenario).isEqualTo(expectedStart);
        }

        @NonNull
        private static Stream<Arguments> provideSourceRanges() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, -1, 0),
                    Arguments.of("empty interior range", " x", 1, 0, 1),
                    Arguments.of("spaces before content", "  @A ", 0, 4, 2),
                    Arguments.of("range starts after content", "@A \t@B", 2, 5, 4),
                    Arguments.of("bounded search stops before content", "  @A", 0, 0, 1),
                    Arguments.of("only whitespace", " \t\r\n\f", 0, 4, 5),
                    Arguments.of("CRLF before indentation", "\r\n\t@A", 0, 4, 3),
                    Arguments.of("form feed and Unicode whitespace", "\f\u2003x", 0, 2, 2),
                    Arguments.of("non-breaking space is content", "\u00A0x", 0, 1, 0));
        }
    }

    @Nested
    class FindFragmentStartWithIndentation {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideContentRanges")
        void findFragmentStartWithIndentation_contentInRange_preservesIndentation(
                @NonNull String scenario, @NonNull String srcCode, int start, int end, int expectedStart) {
            // When
            int fragmentStart = findFragmentStartWithIndentation(start, end, srcCode);

            // Then
            assertThat(fragmentStart).as(scenario).isEqualTo(expectedStart);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideInvalidRanges")
        void findFragmentStartWithIndentation_invalidRange_throwsIndexOutOfBoundsException(
                @NonNull String scenario, int start, int end) {
            // When / Then
            assertThatThrownBy(() -> findFragmentStartWithIndentation(start, end, "text"))
                    .as(scenario)
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideWhitespaceBoundaries")
        void findFragmentStartWithIndentation_nonIndentationWhitespace_keepsFollowingIndentation(
                @NonNull String scenario, @NonNull String whitespace) {
            // Given
            String srcCode = " \t" + whitespace + " \ttext";
            int expectedStart = 2 + whitespace.length();

            // When
            int fragmentStart = findFragmentStartWithIndentation(0, srcCode.length() - 1, srcCode);

            // Then
            assertThat(fragmentStart).as(scenario).isEqualTo(expectedStart);
            assertThat(srcCode.substring(fragmentStart)).isEqualTo(" \ttext");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtilsTest#provideContentCharacters")
        void findFragmentStartWithIndentation_nonWhitespaceCharacter_stopsSkipping(
                @NonNull String scenario, char character) {
            // Given
            String srcCode = "\n \t" + character + "\ntext";
            String expectedFragment = " \t" + character + "\ntext";

            // When
            int fragmentStart = findFragmentStartWithIndentation(0, srcCode.length() - 1, srcCode);

            // Then
            assertThat(fragmentStart).as(scenario).isEqualTo(1);
            assertThat(srcCode.substring(fragmentStart)).isEqualTo(expectedFragment);
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void findFragmentStartWithIndentation_nullSource_throwsNullPointerException() {
            // When / Then
            assertThatThrownBy(() -> findFragmentStartWithIndentation(0, -1, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideRangesWithoutContent")
        void findFragmentStartWithIndentation_rangeWithoutContent_returnsEndExclusive(
                @NonNull String scenario, @NonNull String srcCode, int start, int end, int expectedStart) {
            // When
            int fragmentStart = findFragmentStartWithIndentation(start, end, srcCode);

            // Then
            assertThat(fragmentStart).as(scenario).isEqualTo(expectedStart);
        }

        @NonNull
        private static Stream<Arguments> provideContentRanges() {
            return Stream.of(
                    Arguments.of("content at the start of the file", "text", 0, 3, 0),
                    Arguments.of("one-character range includes its end", "x", 0, 0, 0),
                    Arguments.of("leading spaces", "    text", 0, 7, 0),
                    Arguments.of("leading tabs", "\t\ttext", 0, 5, 0),
                    Arguments.of("mixed spaces and tabs", " \t text", 0, 6, 0),
                    Arguments.of("start after indentation", " \t text", 3, 6, 0),
                    Arguments.of("start inside indentation", " \t text", 1, 6, 0),
                    Arguments.of("indentation follows earlier code", "x; \ttext", 4, 7, 2),
                    Arguments.of("start inside content", "text", 2, 3, 2),
                    Arguments.of("blank lines before indented content", "\n\n \ttext", 0, 7, 2),
                    Arguments.of("start inside a gap before another line", " \t\n  text", 1, 8, 3),
                    Arguments.of("content immediately after a line break", "\ntext", 0, 4, 1),
                    Arguments.of("content at the inclusive end", "\n  xignored", 0, 3, 1),
                    Arguments.of("content prevents skipping later line breaks", "x\n  y", 0, 4, 0));
        }

        @NonNull
        private static Stream<Arguments> provideRangesWithoutContent() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, -1, 0),
                    Arguments.of("empty range before content", "text", 0, -1, 0),
                    Arguments.of("empty range inside source", "text", 2, 1, 2),
                    Arguments.of("empty range at EOF", "text", 4, 3, 4),
                    Arguments.of("spaces only", "   ", 0, 2, 3),
                    Arguments.of("tabs only", "\t\t", 0, 1, 2),
                    Arguments.of("mixed whitespace only", " \t\r\n\f\u2003", 0, 5, 6),
                    Arguments.of("content exists only after the range", " \ttext", 0, 1, 2),
                    Arguments.of("content exists only before the range", "x \t", 1, 2, 3),
                    Arguments.of("indentation before start is not content", " \t ", 1, 2, 3));
        }
    }

    @Nested
    class FindIndentationEnd {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSourceRanges")
        void findIndentationEnd_sourceRange_skipsOnlySpacesAndTabs(
                @NonNull String scenario, @NonNull String srcCode, int start, int endExclusive, int expectedEnd) {
            assertThat(findIndentationEnd(start, endExclusive, srcCode))
                    .as(scenario)
                    .isEqualTo(expectedEnd);
        }

        @NonNull
        private static Stream<Arguments> provideSourceRanges() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, 0, 0),
                    Arguments.of("empty interior range", " \t@A", 2, 2, 2),
                    Arguments.of("mixed indentation", " \t@A", 0, 4, 2),
                    Arguments.of("indentation after preceding content", "x \t@A", 1, 5, 3),
                    Arguments.of("bounded indentation", " \t ", 0, 2, 2),
                    Arguments.of("indentation reaches EOF", " \t ", 0, 3, 3),
                    Arguments.of("form feed stops indentation", " \f @A", 0, 5, 1),
                    Arguments.of("CRLF stops indentation", " \r\n@A", 0, 5, 1),
                    Arguments.of("Unicode whitespace stops indentation", " \u2003@A", 0, 4, 1),
                    Arguments.of("no indentation", "@A", 0, 2, 0));
        }
    }

    @Nested
    class FindIndentationStart {

        @Test
        @SuppressWarnings("DataFlowIssue")
        void findIndentationStart_nullSource_throwsNullPointerException() {
            // When / Then
            assertThatThrownBy(() -> findIndentationStart(0, null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideIndentationPrefixes")
        void findIndentationStart_sourcePosition_returnsStartOfAdjacentSpacesAndTabs(
                @NonNull String scenario, @NonNull String srcCode, int start, int expectedStart) {
            // When
            int indentationStart = findIndentationStart(start, srcCode);

            // Then
            assertThat(indentationStart).as(scenario).isEqualTo(expectedStart);
        }

        @NonNull
        private static Stream<Arguments> provideIndentationPrefixes() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, 0),
                    Arguments.of("file start", "text", 0, 0),
                    Arguments.of("preceding content stops the search", "text", 2, 2),
                    Arguments.of("spaces before content", "   text", 3, 0),
                    Arguments.of("tabs before content", "\t\ttext", 2, 0),
                    Arguments.of("start inside mixed indentation", " \t text", 2, 0),
                    Arguments.of("indentation after code on the same line", "x; \ttext", 4, 2),
                    Arguments.of("line break before indentation", "\n \ttext", 3, 1),
                    Arguments.of("form feed before indentation", "\f \ttext", 3, 1),
                    Arguments.of("Unicode whitespace before indentation", "\u2003 \ttext", 3, 1),
                    Arguments.of("indentation reaches EOF", " \t ", 3, 0));
        }
    }

    @Nested
    class FindLineContentEndExclusive {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSourceRanges")
        void findLineContentEndExclusive_sourceRange_removesOnlyTrailingLineSeparators(
                @NonNull String scenario, @NonNull String srcCode, int start, int endExclusive, int expectedEnd) {
            assertThat(findLineContentEndExclusive(start, endExclusive, srcCode))
                    .as(scenario)
                    .isEqualTo(expectedEnd);
        }

        @NonNull
        private static Stream<Arguments> provideSourceRanges() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, 0, 0),
                    Arguments.of("empty interior range", "x\n", 1, 1, 1),
                    Arguments.of("spaces and tabs in a line comment", "// note  \t\r\n", 0, 12, 10),
                    Arguments.of("LF", "// x\n", 0, 5, 4),
                    Arguments.of("CR", "// x\r", 0, 5, 4),
                    Arguments.of("consecutive separators", "x\r\n\n", 0, 4, 1),
                    Arguments.of("bounded search ignores following text", "x\nnext", 0, 2, 1),
                    Arguments.of("range contains only separators", "\r\n@A", 0, 2, 0),
                    Arguments.of("range begins inside CRLF", "x\r\n", 2, 3, 2),
                    Arguments.of("spaces after a separator remain", "x\n ", 0, 3, 3),
                    Arguments.of("other whitespace remains", " \f\u2003", 0, 3, 3));
        }
    }

    @Nested
    class FindLineSeparatorStart {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSourcePositions")
        void findLineSeparatorStart_sourcePosition_findsOnlyTheImmediatelyPrecedingSeparator(
                @NonNull String scenario, @NonNull String srcCode, int lineStart, int expectedStart) {
            assertThat(findLineSeparatorStart(lineStart, srcCode)).as(scenario).isEqualTo(expectedStart);
        }

        @NonNull
        private static Stream<Arguments> provideSourcePositions() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, 0),
                    Arguments.of("source start", "x", 0, 0),
                    Arguments.of("no preceding separator", "x", 1, 1),
                    Arguments.of("LF at source start", "\n", 1, 0),
                    Arguments.of("LF", "x\nA", 2, 1),
                    Arguments.of("CR", "x\rA", 2, 1),
                    Arguments.of("CRLF", "x\r\nA", 3, 1),
                    Arguments.of("CRLF at source start", "\r\n", 2, 0),
                    Arguments.of("consecutive LF separators", "x\n\nA", 3, 2),
                    Arguments.of("CR followed by CRLF", "x\r\r\nA", 4, 2),
                    Arguments.of("bounded CR before LF", "x\r\nA", 2, 1),
                    Arguments.of("indentation after separator", "\r\n \t", 4, 4));
        }
    }

    @Nested
    class FindLineStart {

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideSourcePositions")
        void findLineStart_contentPosition_returnsOffsetAfterPrecedingCrOrLf(
                @NonNull String scenario, @NonNull String srcCode, int contentStart, int expectedStart) {
            assertThat(findLineStart(contentStart, srcCode)).as(scenario).isEqualTo(expectedStart);
        }

        @NonNull
        private static Stream<Arguments> provideSourcePositions() {
            return Stream.of(
                    Arguments.of("empty source", "", 0, 0),
                    Arguments.of("source start", "abc", 0, 0),
                    Arguments.of("no line separator", "abc", 3, 0),
                    Arguments.of("CRLF before indentation", "x\r\n \t@A", 5, 3),
                    Arguments.of("CR before indentation", "x\r @A", 3, 2),
                    Arguments.of("LF before annotation", "x\n@A", 2, 2),
                    Arguments.of("empty preceding line", "x\n\n@A", 3, 3),
                    Arguments.of("CRLF at EOF", "x\r\n", 3, 3),
                    Arguments.of("later separator is ignored", "first\nsecond\nthird", 8, 6),
                    Arguments.of("separator at the boundary is excluded", "line\n", 4, 0),
                    Arguments.of("Unicode separator is not CR or LF", "x\u2028@A", 3, 0));
        }
    }
}
