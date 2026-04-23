// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class TrailingCommentAstDiagnosticTest {

    private static final String SRC = "// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>\n"
            + "// SPDX-License-Identifier: Apache-2.0\n"
            + "package io.github.lemon_ant.jharmonizer.core.e2e;\n\n"
            + "public class AbstractTaskListener {\n\n"
            + "    private volatile int value;  // needs to stay inline\n"
            + "}\n";

    @Test
    void inspectSpoonAstModel() {
        SrcFile srcFile = createSrcFile(SRC, Path.of("AbstractTaskListener.java"));
        PrinterConfig config = new PrinterConfig(true, true, false);
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(srcFile, config);

        CtType<?> type = model.getMainType().orElseThrow();
        System.out.println("=== Type: " + type.getSimpleName());
        System.out.println("  Type position: " + type.getPosition().getSourceStart() + " -> "
                + type.getPosition().getSourceEnd());

        List<CtTypeMember> members = type.getTypeMembers();
        System.out.println("  Number of type members: " + members.size());
        for (CtTypeMember member : members) {
            System.out.println("  --- Member: " + member.getClass().getSimpleName() + " | " + member.getSimpleName());
            System.out.println("      implicit: " + member.isImplicit());
            System.out.println("      position valid: " + member.getPosition().isValidPosition());
            if (member.getPosition().isValidPosition()) {
                int start = member.getPosition().getSourceStart();
                int end = member.getPosition().getSourceEnd();
                System.out.println("      sourceStart=" + start + " sourceEnd=" + end);
                System.out.println("      source text: [" + SRC.substring(start, end + 1) + "]");
            }
            List<CtComment> comments = member.getComments();
            System.out.println("      comments attached: " + comments.size());
            for (CtComment comment : comments) {
                int cs = comment.getPosition().getSourceStart();
                int ce = comment.getPosition().getSourceEnd();
                System.out.println("        comment: [" + SRC.substring(cs, ce + 1) + "] start=" + cs + " end=" + ce);
                System.out.println(
                        "          commentLine=" + comment.getPosition().getLine() + " memberLine="
                                + member.getPosition().getLine());
                System.out.println("          isBefore="
                        + (comment.getPosition().getEndLine()
                                < member.getPosition().getLine()));
            }
            if (member instanceof CtField<?> field) {
                System.out.println("      field modifiers: " + field.getModifiers());
            }
        }

        // Also check what SrcCodeUtils gives as the field boundary
        int fieldStart = -1, fieldEnd = -1;
        for (CtTypeMember member : members) {
            if (member instanceof CtField<?>) {
                fieldStart = member.getPosition().getSourceStart();
                fieldEnd = member.getPosition().getSourceEnd();
            }
        }
        if (fieldStart >= 0) {
            System.out.println("=== Field boundaries in source:");
            System.out.println("  Field text by position: [" + SRC.substring(fieldStart, fieldEnd + 1) + "]");
            System.out.println("  Char after fieldEnd: [" + SRC.charAt(fieldEnd + 1) + "]");
            // Check what the next char is
            System.out.println(
                    "  Text from fieldEnd to EOL: [" + SRC.substring(fieldEnd, SRC.indexOf('\n', fieldEnd) + 1) + "]");
        }
    }
}
