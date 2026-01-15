package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            Pair<CompiledMemberGroup, Integer> rootResult = compileGroupRecursively(unifiedRoot, currentIndex, false);
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
            UnifiedMemberGroup unifiedGroup, int startIndex, boolean inheritedKeepAccessorsTogether) {
        int runningIndex = startIndex;

        // 1) Build children first (DFS), threading the index forward
        List<CompiledMemberGroup> compiledChildren =
                new ArrayList<>(unifiedGroup.getMemberSubGroups().size());
        boolean keepAccessorsTogether =
                Optional.ofNullable(unifiedGroup.getKeepAccessorsTogether()).orElse(inheritedKeepAccessorsTogether);
        for (UnifiedMemberGroup unifiedChild : unifiedGroup.getMemberSubGroups()) {
            Pair<CompiledMemberGroup, Integer> childResult =
                    compileGroupRecursively(unifiedChild, runningIndex, keepAccessorsTogether);
            compiledChildren.add(childResult.getLeft());
            runningIndex = childResult.getRight(); // advance by everything created inside the child
        }

        // 2) Compile selector/sorting for the current node (whatever your project uses)
        CompiledMemberGroupSelectorBlock compiledMemberGroupSelectorBlock =
                compileSelectorBlock(unifiedGroup.getSelectorBlock());
        SortKey sortKey = mapSortKeys(unifiedGroup.getSortKeys());

        // 3) Assign post-order index to THIS node and advance index
        int assignedPostOrderIndex = runningIndex;
        int nextFreeIndex = runningIndex + 1;

        // 4) Build the current compiled group (assuming builder has postOrderIndex or similar)
        CompiledMemberGroup compiledCurrentGroup = CompiledMemberGroup.builder()
                .name(unifiedGroup.getGroupName())
                .selectorBlock(compiledMemberGroupSelectorBlock)
                .sortKey(sortKey)
                .keepAccessorsTogether(keepAccessorsTogether)
                .compiledSubGroups(compiledChildren)
                .orderIndex(assignedPostOrderIndex)
                .separator(unifiedGroup.getSeparator())
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
    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private static SortKey mapSortKeys(List<UnifiedSortKey> unifiedSortKeys) {
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
