package io.github.lemon_ant.jharmonizer.core.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for computing and formatting a human-readable unified diff between two Java source strings.
 * Whitespace characters in changed lines are visualised with special markers to aid diagnosis.
 */
@Slf4j
@UtilityClass
// TODO Review the entire class
public class DiffReporter {

    @NonNull
    public static String computeDiff(@NonNull String originalText, @NonNull String generatedText) {
        Patch<String> diff = DiffUtils.diff(
                originalText.lines().toList(), generatedText.lines().toList());
        return format(diff);
    }

    private static void appendDiffLine(StringBuilder sb, String prefix, String line) {
        if (line != null) {
            sb.append(prefix).append(visualizeWhitespace(line)).append(System.lineSeparator());
        }
    }

    private static String format(Patch<String> diffs) {
        StringBuilder diffBuilder = new StringBuilder();

        diffs.getDeltas().stream()
                .sorted(Comparator.comparingInt(delta -> delta.getSource().getPosition()))
                .forEach(delta -> {
                    List<String> originalLines = delta.getSource().getLines();
                    List<String> revisedLines = delta.getTarget().getLines();

                    int deltaStartLine = delta.getSource().getPosition();
                    int comparableLineCount = Math.min(originalLines.size(), revisedLines.size());

                    IntStream.range(0, comparableLineCount)
                            .forEach(relativeLineIndex -> processDelta(
                                    deltaStartLine, relativeLineIndex, originalLines, revisedLines, diffBuilder));
                });

        return diffBuilder.toString();
    }

    private static String getLineSafe(List<String> list, int index) {
        return (index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    private static void processDelta(
            int startLine, int i, List<String> original, List<String> revised, StringBuilder sb) {
        int lineNumber = startLine + i;
        sb.append("Line ").append(lineNumber).append(":\n");
        appendDiffLine(sb, "- ", getLineSafe(original, i));
        appendDiffLine(sb, "+", getLineSafe(revised, i));

        sb.append(System.lineSeparator());
    }

    private static String visualizeWhitespace(String line) {
        if (StringUtils.isBlank(line)) {
            return "[blank line]";
        }

        return line.replace(" ", "·") // spaces
                        .replace("\t", "→→→→") // tabs
                + "¶"; // End of the line marker
    }
}
