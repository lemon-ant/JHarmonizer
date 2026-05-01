// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

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
        void computeDiff_singleChange_producesGitStyleHeaderLinesOnSeparateLines() {
            // Given
            String original = "class A {\n    void a() {}\n    void b() {}\n}\n";
            String revised = "class A {\n    void b() {}\n    void a() {}\n}\n";

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] lines = result.split(System.lineSeparator(), -1);
            assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("--- a/" + FILE_PATH));
            assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("+++ b/" + FILE_PATH));
            assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("@@ "));
            // Each header must be its own line and not merged with content
            assertThat(lines)
                    .filteredOn(line -> line.startsWith("@@ "))
                    .allSatisfy(line -> assertThat(line).doesNotContain("·").doesNotContain("→→→→"));
        }
    }

    @Nested
    class WhitespaceVisualization {

        @Test
        void computeDiff_changedLinesContainingSpacesAndTabs_visualizesWhitespaceInContentLinesOnly() {
            // Given
            String original = "class A {\n    void a() {}\n}\n";
            String revised = "class A {\n\tvoid a() {}\n}\n";

            // When
            String result = computeDiff(FILE_PATH, original, revised);

            // Then
            String[] lines = result.split(System.lineSeparator(), -1);
            // Removed line: spaces → ·
            assertThat(lines)
                    .anySatisfy(line ->
                            assertThat(line).startsWith("-").contains("····").endsWith("¶"));
            // Added line: tab → →→→→
            assertThat(lines)
                    .anySatisfy(line ->
                            assertThat(line).startsWith("+").contains("→→→→").endsWith("¶"));
            // @@ header must not contain whitespace visualization markers
            assertThat(lines)
                    .filteredOn(line -> line.startsWith("@@ "))
                    .allSatisfy(line -> assertThat(line)
                            .doesNotContain("·")
                            .doesNotContain("→→→→")
                            .doesNotEndWith("¶"));
            // --- and +++ headers must not contain visualization markers
            assertThat(lines)
                    .filteredOn(line -> line.startsWith("--- ") || line.startsWith("+++ "))
                    .allSatisfy(line -> assertThat(line)
                            .doesNotContain("·")
                            .doesNotContain("→→→→")
                            .doesNotEndWith("¶"));
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
            String[] lines = result.split(System.lineSeparator(), -1);
            long hunkHeaderCount = java.util.Arrays.stream(lines)
                    .filter(line -> line.startsWith("@@ "))
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
            String[] lines = result.split(System.lineSeparator(), -1);
            long removedLineCount = java.util.Arrays.stream(lines)
                    .filter(line -> line.startsWith("-") && !line.startsWith("---"))
                    .count();
            long addedLineCount = java.util.Arrays.stream(lines)
                    .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                    .count();
            assertThat(removedLineCount).isLessThanOrEqualTo(20);
            assertThat(addedLineCount).isLessThanOrEqualTo(20);
            assertThat(result).contains("more removed").contains("more added").contains("lines omitted");
        }
    }
}
