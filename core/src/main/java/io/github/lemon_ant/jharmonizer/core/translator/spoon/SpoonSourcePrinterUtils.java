package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Internal utilities shared between the custom Spoon source printer and its callers.
 * Provides helpers for detecting the dominant line separator, locating indentation boundaries,
 * and deciding whether a member group separator is needed before or after a given member.
 */
@UtilityClass
public class SpoonSourcePrinterUtils {

    public static final String GROUP_HEADER_METADATA = "GROUP_HEADER";

    static int findIndentationStart(int start, String sourceCode) {
        int pos = start - 1;
        while (pos >= 0) {
            char c = sourceCode.charAt(pos);
            if (c != '\t' && c != ' ') {
                break;
            }
            pos--;
        }
        return pos + 1;
    }

    @NonNull
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidReassigningLoopVariables"})
    static String detectDominantLineSeparator(@NonNull String source) {
        if (source.isEmpty()) {
            return System.lineSeparator();
        }

        int crlfCount = 0;
        int lfCount = 0;
        int crCount = 0;

        for (int index = 0; index < source.length(); index++) {
            char currentChar = source.charAt(index);

            if (currentChar == '\r') {
                boolean hasNextChar = (index + 1) < source.length();
                if (hasNextChar && source.charAt(index + 1) == '\n') {
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

        // Pick the dominant one; if tie, prefer CRLF > LF > CR (can be adjusted).
        if (crlfCount >= lfCount && crlfCount >= crCount) {
            return "\r\n";
        }
        if (lfCount >= crCount) {
            return "\n";
        }
        return "\r";
    }

    static boolean needsSeparatorAfter(CtTypeMember member) {
        // Add the separator in any way if it's not field
        boolean isNotField = !(member instanceof CtField);

        return isNotField || !member.getAnnotations().isEmpty();
    }

    static boolean needsSeparatorBefore(CtTypeMember member, boolean first) {
        // Has annotations on the member
        boolean hasAnnotations = !member.getAnnotations().isEmpty();

        // Has comments above
        boolean hasCommentsAbove = member.getComments().stream()
                .anyMatch(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine());

        // Add the separator in any way if it's not field
        boolean isNotField = !(member instanceof CtField);

        return hasAnnotations || hasCommentsAbove || !first && isNotField;
    }
}
