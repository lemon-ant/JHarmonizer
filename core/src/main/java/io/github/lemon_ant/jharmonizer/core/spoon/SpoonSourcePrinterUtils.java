package io.github.lemon_ant.jharmonizer.core.spoon;

import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
public class SpoonSourcePrinterUtils {

    public static final String START_OF_GROUP_METADATA_MARKER = "START_OF_GROUP";

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

        // The member was marked as the first member of a group
        boolean isStartOfGroup = member.getMetadata(START_OF_GROUP_METADATA_MARKER) != null;

        // Add the separator in any way if it's not field
        boolean isNotField = !(member instanceof CtField);

        return hasAnnotations || hasCommentsAbove || !first && (isNotField || isStartOfGroup);
    }
}
