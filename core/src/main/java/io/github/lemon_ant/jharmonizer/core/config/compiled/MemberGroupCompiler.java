package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
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
        List<OrderingRule> compiledOrderingRules = mapOrderingRules(unifiedGroup.getOrderingRules());

        // 3) Assign post-order index to THIS node and advance index
        int assignedPostOrderIndex = runningIndex;
        int nextFreeIndex = runningIndex + 1;

        // 4) Build the current compiled group (assuming builder has postOrderIndex or similar)
        CompiledMemberGroup compiledCurrentGroup = CompiledMemberGroup.builder()
                .name(unifiedGroup.getGroupName())
                .selectorBlock(compiledMemberGroupSelectorBlock)
                .orderingRules(compiledOrderingRules)
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
    private static List<OrderingRule> mapOrderingRules(@NonNull List<UnifiedOrderingRule> unifiedOrderingRules) {
        // Unified model guarantees non-empty list; preserve order and map 1:1.
        return unifiedOrderingRules.stream().map(MemberGroupCompiler::mapOrderingRule).toList();
    }

    private static OrderingRule mapOrderingRule(@NonNull UnifiedOrderingRule unifiedOrderingRule) {
        return switch (unifiedOrderingRule) {
            case ALPHA -> OrderingRule.ALPHA;
            case PRESERVE -> OrderingRule.PRESERVE;
            case VISIBILITY_ASC -> OrderingRule.VISIBILITY_ASC;
            case VISIBILITY_DESC -> OrderingRule.VISIBILITY_DESC;
        };
    }
}
