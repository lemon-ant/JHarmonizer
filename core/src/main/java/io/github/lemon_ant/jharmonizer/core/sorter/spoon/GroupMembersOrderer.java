package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * MVP implementation:
 * - applies a simple comparator chain based on group's current single sortKey,
 * - preserves source order as a safe fallback.
 *
 * Later:
 * - replace with stable topo-sort for dependency edges,
 * - implement keepAccessorsTogether bundling,
 * - support multi-key sortKeys.
 */
@UtilityClass
class GroupMembersOrderer {

    @NonNull
    List<@NonNull MemberGroupBlock> orderMembersInsideGroups(
            @NonNull List<@NonNull MemberGroupBlock> unorderedBlocks,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        List<MemberGroupBlock> orderedBlocks = new ArrayList<>();

        for (MemberGroupBlock memberGroupBlock : unorderedBlocks) {
            List<CtTypeMember> groupMembers = new ArrayList<>(memberGroupBlock.getTypeMembers());

            Comparator<CtTypeMember> memberComparator =
                    buildComparatorForGroup(memberGroupBlock.getCompiledMemberGroup());

            groupMembers.sort(memberComparator);

            orderedBlocks.add(
                    new MemberGroupBlock(memberGroupBlock.getCompiledMemberGroup(), List.copyOf(groupMembers)));
        }

        return List.copyOf(orderedBlocks);
    }

    private static Comparator<CtTypeMember> buildComparatorForGroup(CompiledMemberGroup groupSortingBehavior) {
        // TODO keepAccessorsTogether

        List<SortKey> sortKeys = groupSortingBehavior.getSortKeys();

        Comparator<CtTypeMember> combinedComparator = null;

        for (SortKey sortKey : sortKeys) {
            Comparator<CtTypeMember> comparatorForSortKey =
                    switch (sortKey) {
                        case PRESERVE, SOURCE_ORDER -> Comparator.comparingInt(GroupMembersOrderer::safeSourceStart);

                        case ALPHA ->
                            Comparator.comparing(GroupMembersOrderer::safeSimpleName)
                                    .thenComparingInt(GroupMembersOrderer::safeSourceStart);

                        case VISIBILITY_ASC, VISIBILITY_DESC, SIGNATURE ->
                            // TODO Replace with real visibility/signature comparators once implemented.
                            Comparator.comparingInt(GroupMembersOrderer::safeSourceStart);
                    };

            combinedComparator = (combinedComparator == null)
                    ? comparatorForSortKey
                    : combinedComparator.thenComparing(comparatorForSortKey);
        }

        if (combinedComparator == null) {
            throw new IllegalStateException("Sort keys collection produced no comparator");
        }

        return combinedComparator;
    }

    private static int safeSourceStart(@NonNull CtTypeMember typeMember) {
        return typeMember.getPosition().isValidPosition()
                ? typeMember.getPosition().getSourceStart()
                : Integer.MAX_VALUE;
    }

    private static @NonNull String safeSimpleName(@NonNull CtTypeMember typeMember) {
        return typeMember.toString();
    }
}
