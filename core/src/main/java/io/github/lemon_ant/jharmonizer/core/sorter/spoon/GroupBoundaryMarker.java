package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Default marker:
 * - sets START_OF_GROUP marker on the first member of each non-empty group.
 * <p>
 * Note: current printer only checks a boolean marker; we keep separator enum in the block for future enhancements.
 */
// TODO Useless class???
@UtilityClass
class GroupBoundaryMarker {

    /**
     * Metadata key used by the printer to detect group boundaries.
     */
    // TODO Public
    public static final String GROUP_START_METADATA_KEY = SpoonSourcePrinterUtils.START_OF_GROUP_METADATA_MARKER;

    /**
     * Optional metadata key for future: store UnifiedSeparator on the first element of the group.
     * Printer can later decide between NEW_LINE / HEADER / NONE.
     */
    // TODO Public
    public static final String GROUP_SEPARATOR_METADATA_KEY = "GROUP_SEPARATOR";

    void markGroupBoundaries(@NonNull List<@NonNull MemberGroupBlock> orderedBlocks) {
        for (MemberGroupBlock memberGroupBlock : orderedBlocks) {
            if (memberGroupBlock.getTypeMembers().isEmpty()) {
                continue;
            }

            CtTypeMember firstGroupMember = memberGroupBlock.getTypeMembers().getFirst();

            // Boolean start marker currently used by SpoonSourcePrinterUtils.
            firstGroupMember.putMetadata(GROUP_START_METADATA_KEY, Boolean.TRUE);

            // Future-proof: store the requested separator directive for this group boundary.
            firstGroupMember.putMetadata(
                    GROUP_SEPARATOR_METADATA_KEY,
                    memberGroupBlock.getCompiledMemberGroup().getSeparator());
        }
    }
}
