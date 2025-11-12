package io.github.lemon_ant.jharmonizer.core.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import java.util.Comparator;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
// TODO Review the entire class
public class DiffReporter {

    public static String computeDiff(String originalText, String generatedText) {
        Patch<String> diff = DiffUtils.diff(
                originalText.lines().toList(), generatedText.lines().toList());
        return format(diff);
    }

    private static String format(Patch<String> diffs) {
        StringBuilder sb = new StringBuilder();

        List<AbstractDelta<String>> deltas = diffs.getDeltas();
        deltas.sort(Comparator.comparingInt(d -> d.getSource().getPosition()));
        for (AbstractDelta<String> delta : deltas) {
            List<String> original = delta.getSource().getLines();
            List<String> revised = delta.getTarget().getLines();

            int startLine = delta.getSource().getPosition();
            int maxLines = Math.min(original.size(), revised.size());

            for (int i = 0; i < maxLines; i++) {
                processDelta(startLine, i, original, revised, sb);
            }
        }
        return sb.toString();
    }

    private static void processDelta(
            int startLine, int i, List<String> original, List<String> revised, StringBuilder sb) {
        int lineNumber = startLine + i;
        sb.append("Line ").append(lineNumber).append(":\n");
        appendDiffLine(sb, "- ", getLineSafe(original, i));
        appendDiffLine(sb, "+", getLineSafe(revised, i));

        sb.append(System.lineSeparator());
    }

    private static String getLineSafe(List<String> list, int index) {
        return (index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    private static String visualizeWhitespace(String line) {
        if (StringUtils.isBlank(line)) {
            return "[blank line]";
        }

        return line.replace(" ", "·") // spaces
                        .replace("\t", "→→→→") // tabs
                + "¶"; // End of the line marker
    }

    private static void appendDiffLine(StringBuilder sb, String prefix, String line) {
        if (line != null) {
            sb.append(prefix).append(visualizeWhitespace(line)).append(System.lineSeparator());
        }
    }
}
