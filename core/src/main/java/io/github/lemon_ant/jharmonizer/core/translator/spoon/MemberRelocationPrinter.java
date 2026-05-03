// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.DeclarationHeaderRenderer.renderDeclarationHeader;
import static java.lang.System.lineSeparator;

import io.github.lemon_ant.jharmonizer.core.diff.DiffReporter;
import io.github.lemon_ant.jharmonizer.core.diff.WhitespaceVisualizationStyle;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Formats {@link MemberRelocation} violations into human-readable log messages.
 *
 * <p>Produces a single multi-line string that can be passed directly to a logger,
 * listing the file path and the detected ordering violations with placement context
 * (predecessor / relocated chunk / successor).
 */
@UtilityClass
public class MemberRelocationPrinter {

    private static final int MAX_PATH_DISPLAY_LENGTH = 120;
    private static final int MAX_DISPLAYED_VIOLATIONS = 5;
    private static final int MAX_DISPLAYED_CHUNK_MEMBERS = 3;
    private static final int INITIAL_OUTPUT_BUFFER_CAPACITY = 256;

    /**
     * Formats the relocations into a human-readable string.
     *
     * <p>The path is placed on its own indented line after the header, abbreviated to
     * at most {@value MAX_PATH_DISPLAY_LENGTH} characters. Each numbered entry shows
     * only the declaring type name (no repeated "ordering violation" label), then the
     * predecessor, violating member (marked with {@code -->}), and successor snippets.
     * At most {@value MAX_DISPLAYED_VIOLATIONS} entries are printed; if there are more,
     * a footer line reports the total count.
     *
     * <p>Example output for a class where {@code void b()} should come after {@code void a()}:
     * <pre>
     * Detected member ordering violations in:
     *   Sample.java
     *   [1] com.example.Sample:
     *         public void a() { … }
     *     --> public void b() { … }
     * </pre>
     *
     * @param path        the path of the file where the relocations were detected
     * @param relocations the collection of relocations to format
     * @return a formatted string representing the relocations
     */
    @NonNull
    public static String printRelocations(@NonNull Path path, @NonNull Collection<MemberRelocation> relocations) {
        WhitespaceVisualizationStyle style = DiffReporter.resolveStyle();
        List<MemberRelocation> relocationList = List.copyOf(relocations);
        int totalCount = relocationList.size();
        int displayedCount = Math.min(totalCount, MAX_DISPLAYED_VIOLATIONS);
        StringBuilder sb = new StringBuilder(INITIAL_OUTPUT_BUFFER_CAPACITY);
        sb.append("Detected member ordering violations in:")
                .append(lineSeparator())
                .append("  ")
                .append(abbreviatePathForDisplay(path, MAX_PATH_DISPLAY_LENGTH));
        for (int i = 0; i < displayedCount; i++) {
            sb.append(lineSeparator());
            appendRelocationEntry(sb, relocationList.get(i), i + 1, style);
        }
        if (totalCount > MAX_DISPLAYED_VIOLATIONS) {
            sb.append(lineSeparator())
                    .append(String.format("  %s %d violations total", style.getEllipsisMark(), totalCount));
        }
        return sb.toString();
    }

    private static void appendRelocationEntry(
            StringBuilder sb, MemberRelocation relocation, int index, WhitespaceVisualizationStyle style) {
        CtTypeMember firstMember = relocation.getRelocatedMembers().get(0);
        String typeName = firstMember.getDeclaringType() != null
                ? firstMember.getDeclaringType().getQualifiedName()
                : "<file root>";
        sb.append(String.format("  [%d] %s:", index, typeName));
        if (relocation.getSortedPredecessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format(
                            "        %s", renderDeclarationHeader(relocation.getSortedPredecessor(), style)));
        }
        appendChunkLines(sb, relocation.getRelocatedMembers(), style);
        if (relocation.getSortedSuccessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format(
                            "        %s", renderDeclarationHeader(relocation.getSortedSuccessor(), style)));
        }
    }

    private static void appendChunkLines(
            StringBuilder sb, List<CtTypeMember> members, WhitespaceVisualizationStyle style) {
        if (members.size() <= MAX_DISPLAYED_CHUNK_MEMBERS) {
            for (CtTypeMember member : members) {
                sb.append(lineSeparator()).append(String.format("    --> %s", renderDeclarationHeader(member, style)));
            }
        } else {
            int omittedCount = members.size() - 2;
            sb.append(lineSeparator())
                    .append(String.format("    --> %s", renderDeclarationHeader(members.get(0), style)))
                    .append(lineSeparator())
                    .append(String.format("    %s (%d members omitted)", style.getChunkOmissionMark(), omittedCount))
                    .append(lineSeparator())
                    .append(String.format(
                            "    --> %s", renderDeclarationHeader(members.get(members.size() - 1), style)));
        }
    }
}
