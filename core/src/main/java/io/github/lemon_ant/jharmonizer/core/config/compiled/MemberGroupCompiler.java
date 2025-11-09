package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupSortingBehavior.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior.UnifiedSortKey;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
class MemberGroupCompiler {
    /**
     * Entry point: builds all root groups and assigns post-order indices in one pass.
     * Indices are assigned AFTER children (post-order). Start index can be 0.
     */
    @NonNull
    // TODO Rename, it compiles the entire groups forest recursively
    static List<CompiledMemberGroup> compileTopLevelGroups(@NonNull List<UnifiedMemberGroup> unifiedRoots) {
        int currentIndex = 0;
        List<CompiledMemberGroup> compiledRoots = new ArrayList<>(unifiedRoots.size());
        for (UnifiedMemberGroup unifiedRoot : unifiedRoots) {
            Pair<CompiledMemberGroup, Integer> rootResult = compileGroupRecursively(unifiedRoot, currentIndex);
            compiledRoots.add(rootResult.getLeft());
            currentIndex = rootResult.getRight();
        }
        return compiledRoots;
    }

    /**
     * Recursive builder:
     * 1) Recursively builds all children, threading the index through.
     * 2) On unwind, assigns post-order index to the current group.
     * 3) Returns (compiledGroup, nextFreeIndex).
     */
    @NonNull
    private static Pair<CompiledMemberGroup, Integer> compileGroupRecursively(
            UnifiedMemberGroup unifiedGroup, int startIndex) {
        int runningIndex = startIndex;

        // 1) Build children first (DFS), threading the index forward
        List<CompiledMemberGroup> compiledChildren =
                new ArrayList<>(unifiedGroup.getMemberSubGroups().size());
        for (UnifiedMemberGroup unifiedChild : unifiedGroup.getMemberSubGroups()) {
            Pair<CompiledMemberGroup, Integer> childResult = compileGroupRecursively(unifiedChild, runningIndex);
            compiledChildren.add(childResult.getLeft());
            runningIndex = childResult.getRight(); // advance by everything created inside the child
        }

        // 2) Compile selector/sorting for the current node (whatever your project uses)
        CompiledMemberGroupSelectorBlock compiledMemberGroupSelectorBlock =
                compileSelectorBlock(unifiedGroup.getSelectorBlock());
        CompiledMemberGroupSortingBehavior compiledSortingBehavior =
                compileSortingBehavior(unifiedGroup.getSortingBehavior());

        // 3) Assign post-order index to THIS node and advance index
        int assignedPostOrderIndex = runningIndex;
        int nextFreeIndex = runningIndex + 1;

        // 4) Build the current compiled group (assuming builder has postOrderIndex or similar)
        CompiledMemberGroup compiledCurrentGroup = CompiledMemberGroup.builder()
                .name(unifiedGroup.getGroupName())
                .selectorBlock(compiledMemberGroupSelectorBlock)
                .groupSortingBehavior(compiledSortingBehavior)
                .compiledSubGroups(compiledChildren)
                .orderIndex(assignedPostOrderIndex)
                .build();

        // 5) Return pair: (group, next index after this node)
        return Pair.of(compiledCurrentGroup, nextFreeIndex);
    }

    @NonNull
    private static CompiledMemberGroupSelectorBlock compileSelectorBlock(
            UnifiedMemberGroupSelectorBlock selectorBlock) {
        var includes = selectorBlock.getIncludes().stream()
                .map(MemberGroupRuleLineCompiler::compileRuleLine)
                .toList();
        var excludes = selectorBlock.getExcludes().stream()
                .map(MemberGroupRuleLineCompiler::compileRuleLine)
                .toList();
        return new CompiledMemberGroupSelectorBlock(includes, excludes);
    }

    // TODO Complete model and mapper
    @NonNull
    private static CompiledMemberGroupSortingBehavior compileSortingBehavior(
            UnifiedSortingBehavior unifiedSortingBehavior) {
        SortKey sortKey = mapSortKeys(unifiedSortingBehavior.getUnifiedSortKeys());
        boolean keepAccessorsTogether = unifiedSortingBehavior.isKeepAccessorsTogether();
        // Separator handling can be added to Compiled model later; keep a placeholder string for now (null ==
        // unspecified).
        return new CompiledMemberGroupSortingBehavior(keepAccessorsTogether, sortKey);
    }

    // TODO Complete model and mapper
    @NonNull
    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private static SortKey mapSortKeys(List<UnifiedSortingBehavior.UnifiedSortKey> unifiedSortKeys) {
        // We currently support a single Compiled sort key knob; pick the first meaningful item.
        for (UnifiedSortKey key : unifiedSortKeys) {
            return switch (key) {
                case ALPHA -> SortKey.ALPHA;
                case PRESERVE -> SortKey.PRESERVE;
                default ->
                    // VISIBILITY_ASC / VISIBILITY_DESC / SIGNATURE fall back to a stable source order for now.
                    SortKey.SOURCE_ORDER;
            };
        }
        // Safety: default to PRESERVE if the list is somehow empty (should be validated earlier)
        return SortKey.PRESERVE;
    }
}
