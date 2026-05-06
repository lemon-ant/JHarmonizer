// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Default marker:
 * - sets START_OF_GROUP marker on the first member of each non-empty group.
 * <p>
 * Note: current printer only checks a boolean marker; we keep separator enum in the block for future enhancements.
 */
@UtilityClass
class GroupBoundaryMarker {
    /**
     * Performs the mark group boundaries.
     * @param orderedBlocks the ordered blocks
     */
    void markGroupBoundaries(@NonNull List<@NonNull MemberGroupBlock> orderedBlocks) {
        orderedBlocks.stream()
                .filter(memberGroupBlock -> !memberGroupBlock.getTypeMembers().isEmpty())
                .map(memberGroupBlock -> Pair.of(
                        memberGroupBlock.getTypeMembers().get(0),
                        switch (memberGroupBlock.getCompiledMemberGroup().getSeparator()) {
                            case NEW_LINE -> SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE;
                            case HEADER ->
                                Optional.ofNullable(memberGroupBlock
                                                .getCompiledMemberGroup()
                                                .getName())
                                        .orElse(SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE);
                            case NONE -> null;
                        }))
                .filter(firstMemberAndSeparatorText -> firstMemberAndSeparatorText.getValue() != null)
                .forEach(firstMemberAndSeparatorText -> writeGroupBoundaryMetadata(
                        firstMemberAndSeparatorText.getKey(), firstMemberAndSeparatorText.getValue()));
    }

    private static void writeGroupBoundaryMetadata(CtTypeMember firstMember, String separatorText) {
        firstMember.putMetadata(SpoonSrcPrinterUtils.GROUP_HEADER_METADATA, separatorText);
    }
}
