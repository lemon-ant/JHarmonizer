// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.markBlankLine;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.markHeader;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/** Marks the first member in each group with its requested header or blank line. */
@UtilityClass
class GroupBoundaryMarker {

    /**
     * Attaches separator metadata to each non-empty group.
     * @param orderedBlocks the ordered blocks
     */
    void markGroupBoundaries(@NonNull List<@NonNull MemberGroupBlock> orderedBlocks) {
        orderedBlocks.stream()
                .filter(memberGroupBlock -> !memberGroupBlock.getTypeMembers().isEmpty())
                .forEach(GroupBoundaryMarker::markGroupBoundary);
    }

    private static void markGroupBoundary(MemberGroupBlock memberGroupBlock) {
        CtTypeMember firstMember = memberGroupBlock.getTypeMembers().get(0);
        CompiledMemberGroup group = memberGroupBlock.getCompiledMemberGroup();
        UnifiedSeparator separator = group.getSeparator();
        if (separator == UnifiedSeparator.NONE) {
            // Preserve any existing separator when no new marker is requested.
            return;
        }
        String headerText = group.getName();
        if (separator == UnifiedSeparator.HEADER && headerText != null) {
            markHeader(firstMember, headerText);
        } else {
            markBlankLine(firstMember);
        }
    }
}
