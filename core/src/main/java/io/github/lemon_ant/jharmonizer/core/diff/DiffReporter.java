// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility for computing and formatting a human-readable unified diff between two Java source strings.
 *
 * <p>The output follows git-style unified diff format with:
 * <ul>
 *   <li>Hunk headers on separate lines ({@code @@ -start,len +start,len @@})</li>
 *   <li>A {@code |} separator between the diff prefix ({@code +}, {@code -}, or space) and the
 *       line content, to make it clear that the prefix is a marker and not part of the source</li>
 *   <li>Whitespace characters visualised in changed and context lines to aid diagnosis:
 *       Unicode markers ({@code ·}, {@code →→→→}, {@code ¶}) when both the display and encoder
 *       charsets agree on UTF-8 or another Unicode-capable encoding; Latin-1 markers
 *       ({@code ·}, {@code --->}, {@code ¶}) when both charsets agree on an ISO-8859-1-compatible
 *       encoding such as CP1252; ASCII markers ({@code .}, {@code --->}, no EOL) otherwise</li>
 *   <li>Output truncated to at most {@value #MAX_HUNKS_PER_FILE} hunks and
 *       {@value #MAX_CHANGED_LINES_PER_HUNK} changed lines per hunk</li>
 * </ul>
 *
 * <p>File header lines ({@code --- a/path} and {@code +++ b/path}) are intentionally omitted
 * because the file path is already reported by the caller before the diff output.
 */
@UtilityClass
public class DiffReporter {

    private static final int CONTEXT_SIZE = 3;
    private static final int MAX_HUNKS_PER_FILE = 3;
    private static final int MAX_CHANGED_LINES_PER_HUNK = 20;
    private static final String OMISSION_PREFIX = "... and ";

    /**
     * Computes a truncated, human-readable unified diff between two versions of a source file.
     *
     * <p>Whitespace visualization symbols are chosen by comparing the display charset
     * ({@code stdout.encoding}) and encoder charset ({@link java.nio.charset.Charset#defaultCharset()})
     * byte-by-byte for each candidate marker: Unicode markers when both produce identical bytes for
     * {@code →}; Latin-1 markers when both produce identical bytes for {@code ·} and {@code ¶};
     * ASCII markers otherwise.
     *
     * @param filePath the path of the source file, passed to the underlying diff library
     * @param originalText the original source text
     * @param revisedText the revised source text
     * @return a formatted unified diff string, or an empty string if the texts are identical
     */
    @NonNull
    public static String computeDiff(
            @NonNull String filePath, @NonNull String originalText, @NonNull String revisedText) {
        WhitespaceVisualizationStyle style = ConsoleUnicodeDetector.resolveStyle();
        List<String> originalLines = originalText.lines().toList();
        Patch<String> patch = DiffUtils.diff(originalLines, revisedText.lines().toList());
        if (patch.getDeltas().isEmpty()) {
            return "";
        }
        List<String> unifiedLines = UnifiedDiffUtils.generateUnifiedDiff(
                "a/" + filePath, "b/" + filePath, originalLines, patch, CONTEXT_SIZE);
        return formatUnifiedDiff(unifiedLines, style);
    }

    @NonNull
    private static String formatUnifiedDiff(List<String> unifiedLines, WhitespaceVisualizationStyle style) {
        List<Integer> hunkStarts = findHunkStartIndices(unifiedLines);
        int totalHunks = hunkStarts.size();
        int keptHunkCount = Math.min(MAX_HUNKS_PER_FILE, totalHunks);
        int omittedHunkCount = totalHunks - keptHunkCount;

        StringBuilder sb = new StringBuilder(1024);
        for (int hunkIndex = 0; hunkIndex < keptHunkCount; hunkIndex++) {
            int hunkStart = hunkStarts.get(hunkIndex);
            int hunkEnd = hunkIndex + 1 < totalHunks ? hunkStarts.get(hunkIndex + 1) : unifiedLines.size();
            sb.append(unifiedLines.get(hunkStart)).append(System.lineSeparator());
            formatHunkContent(unifiedLines.subList(hunkStart + 1, hunkEnd), sb, style);
        }
        if (omittedHunkCount > 0) {
            sb.append(OMISSION_PREFIX)
                    .append(omittedHunkCount)
                    .append(" more changed ")
                    .append(omittedHunkCount == 1 ? "hunk" : "hunks")
                    .append(" omitted")
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    @NonNull
    private static List<Integer> findHunkStartIndices(List<String> unifiedLines) {
        List<Integer> hunkStarts = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < unifiedLines.size(); lineIndex++) {
            if (unifiedLines.get(lineIndex).startsWith("@@ ")) {
                hunkStarts.add(lineIndex);
            }
        }
        return Collections.unmodifiableList(hunkStarts);
    }

    private static void formatHunkContent(
            List<String> contentLines, StringBuilder sb, WhitespaceVisualizationStyle style) {
        int truncationPoint = findTruncationPoint(contentLines);
        contentLines.subList(0, truncationPoint).forEach(diffLine -> appendVisualizedLine(sb, diffLine, style));
        if (truncationPoint < contentLines.size()) {
            List<String> remaining = contentLines.subList(truncationPoint, contentLines.size());
            int omittedRemoved = (int) remaining.stream()
                    .filter(diffLine -> diffLine.startsWith("-"))
                    .count();
            int omittedAdded = (int) remaining.stream()
                    .filter(diffLine -> diffLine.startsWith("+"))
                    .count();
            sb.append(buildOmissionSummary(omittedRemoved, omittedAdded)).append(System.lineSeparator());
        }
    }

    private static int findTruncationPoint(List<String> contentLines) {
        int changedCount = 0;
        for (int lineIndex = 0; lineIndex < contentLines.size(); lineIndex++) {
            String line = contentLines.get(lineIndex);
            boolean isChanged = line.startsWith("-") || line.startsWith("+");
            if (isChanged) {
                if (changedCount >= MAX_CHANGED_LINES_PER_HUNK) {
                    return lineIndex;
                }
                changedCount++;
            }
        }
        return contentLines.size();
    }

    private static void appendVisualizedLine(StringBuilder sb, String line, WhitespaceVisualizationStyle style) {
        if (line.isEmpty()) {
            sb.append(System.lineSeparator());
            return;
        }
        char prefix = line.charAt(0);
        String content = line.substring(1);
        sb.append(prefix)
                .append('|')
                .append(visualizeWhitespace(content, style))
                .append(System.lineSeparator());
    }

    @NonNull
    private static String buildOmissionSummary(int omittedRemoved, int omittedAdded) {
        if (omittedRemoved > 0 && omittedAdded > 0) {
            return OMISSION_PREFIX + omittedRemoved + " more removed / " + omittedAdded + " more added lines omitted";
        } else if (omittedRemoved > 0) {
            return OMISSION_PREFIX + omittedRemoved + " more removed lines omitted";
        } else {
            return OMISSION_PREFIX + omittedAdded + " more added lines omitted";
        }
    }

    @NonNull
    private static String visualizeWhitespace(String line, WhitespaceVisualizationStyle style) {
        if (line.isEmpty()) {
            return style.getEolMark();
        }
        return line.replace(" ", style.getSpaceMark()).replace("\t", style.getTabMark()) + style.getEolMark();
    }
}
