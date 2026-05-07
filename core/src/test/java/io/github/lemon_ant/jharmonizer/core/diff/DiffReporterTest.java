// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiffReporterTest {

    private static final String FILE_PATH = "com/example/Sample.java";

    @Nested
    class IdenticalTexts {

        @Test
        void computeDiff_identicalTexts_returnsEmpty() {
            // Given
            String text = "class A {\n    void a() {}\n}\n";

            // When
            String result = computeDiff(FILE_PATH, text, text);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SingleHunk {

        @Test
        void computeDiff_singleChange_producesHunkHeaderWithoutFileHeaders() {
            // Given
            String original = "class A {\n    void a() {}\n    void b() {}\n}\n";
            String revised = "class A {\n    void b() {}\n    void a() {}\n}\n";

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] diffLines = result.split(System.lineSeparator(), -1);
            assertThat(diffLines).anySatisfy(diffLine -> assertThat(diffLine).startsWith("@@ "));
            assertThat(diffLines).noneSatisfy(diffLine -> assertThat(diffLine).startsWith("--- "));
            assertThat(diffLines).noneSatisfy(diffLine -> assertThat(diffLine).startsWith("+++ "));
            // @@ header must not contain visualization markers
            assertThat(diffLines)
                    .filteredOn(diffLine -> diffLine.startsWith("@@ "))
                    .allSatisfy(
                            diffLine -> assertThat(diffLine).doesNotContain("·").doesNotContain("→→→→"));
        }
    }

    @Nested
    class WhitespaceVisualization {

        @Test
        void computeDiff_changedLinesContainingSpacesAndTabs_visualizesWhitespaceInContentLines() {
            // Given
            String original = "class A {\n    void a() {}\n}\n";
            String revised = "class A {\n\tvoid a() {}\n}\n";
            WhitespaceVisualizationStyle style = ConsoleUnicodeDetector.resolveStyle();

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] diffLines = result.split(System.lineSeparator(), -1);
            // Removed line: spaces visualized, ends with EOL mark
            assertThat(diffLines)
                    .anySatisfy(diffLine -> assertThat(diffLine)
                            .startsWith("-")
                            .contains(style.getSpaceMark().repeat(4))
                            .endsWith(style.getEolMark()));
            // Added line: tab visualized, ends with EOL mark
            assertThat(diffLines)
                    .anySatisfy(diffLine -> assertThat(diffLine)
                            .startsWith("+")
                            .contains(style.getTabMark())
                            .endsWith(style.getEolMark()));
            // @@ header must not contain whitespace visualization markers
            assertThat(diffLines)
                    .filteredOn(diffLine -> diffLine.startsWith("@@ "))
                    .allSatisfy(diffLine -> assertThat(diffLine)
                            .doesNotContain(style.getSpaceMark())
                            .doesNotContain(style.getTabMark()));
        }
    }

    @Nested
    class BlankLineVisualization {

        @Test
        void computeDiff_blankLineRemoved_visualizesAsParagraphMark() {
            // Given
            String original = "class A {\n\n    void a() {}\n}\n";
            String revised = "class A {\n    void a() {}\n}\n";
            WhitespaceVisualizationStyle style = ConsoleUnicodeDetector.resolveStyle();

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] diffLines = result.split(System.lineSeparator(), -1);
            assertThat(diffLines).anySatisfy(diffLine -> assertThat(diffLine).isEqualTo("-|" + style.getEolMark()));
        }
    }

    @Nested
    class HunkTruncation {

        @Test
        void computeDiff_moreThanMaxHunksPerFile_truncatesWithOmissionSummary() {
            // Given
            String original = TestCaseResourceUtils.readClasspathResourceAsString(
                    "/test-cases/core/diff/02-many-hunks/original.txt");
            String revised = TestCaseResourceUtils.readClasspathResourceAsString(
                    "/test-cases/core/diff/02-many-hunks/revised.txt");

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] diffLines = result.split(System.lineSeparator(), -1);
            long hunkHeaderCount = java.util.Arrays.stream(diffLines)
                    .filter(diffLine -> diffLine.startsWith("@@ "))
                    .count();
            assertThat(hunkHeaderCount).isEqualTo(3);
            assertThat(result).contains("more changed hunks omitted");
        }

        @Test
        void computeDiff_hunkWithMoreThanMaxChangedLines_truncatesWithLineOmissionSummary() {
            // Given
            String original = TestCaseResourceUtils.readClasspathResourceAsString(
                    "/test-cases/core/diff/03-large-hunk/original.txt");
            String revised = TestCaseResourceUtils.readClasspathResourceAsString(
                    "/test-cases/core/diff/03-large-hunk/revised.txt");

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] diffLines = result.split(System.lineSeparator(), -1);
            long removedLineCount = java.util.Arrays.stream(diffLines)
                    .filter(diffLine -> diffLine.startsWith("-"))
                    .count();
            long addedLineCount = java.util.Arrays.stream(diffLines)
                    .filter(diffLine -> diffLine.startsWith("+"))
                    .count();
            assertThat(removedLineCount).isLessThanOrEqualTo(20);
            assertThat(addedLineCount).isLessThanOrEqualTo(20);
            assertThat(result).contains("more removed").contains("more added").contains("lines omitted");
        }
    }
}
