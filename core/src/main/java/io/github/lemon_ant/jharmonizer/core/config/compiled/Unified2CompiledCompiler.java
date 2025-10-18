package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledGroupSortingBehavior.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior.UnifiedSortKey;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Translates strict Unified configuration into the Effective runtime model.
 * The Effective model is branch-free at runtime: each rule-line is compiled into a single predicate.
 * <p>
 * This compiler performs the following steps:
 * - Compiles selector blocks (includes/excludes) into {@link CompiledSelectorBlock}
 * - Maps sorting behavior knobs to {@link CompiledGroupSortingBehavior}
 * - Builds a DFS-ordered tree of {@link CompiledGroup} nodes and assigns post-order indexes
 */
@UtilityClass
@SuppressWarnings("PMD")
public class Unified2CompiledCompiler {

    /**
     * Compiles a full Unified configuration into an Effective configuration.
     */
    @NonNull
    public static CompiledConfig compile(@NonNull UnifiedConfig unifiedConfig) {
        // TODO Today we only compile the member groups; top-level type ordering may feed into future phases.
        List<CompiledGroup> typeRoots = compileTopLevelGroups(unifiedConfig.getRootMemberGroups());

        return new CompiledConfig(
                typeRoots,
                unifiedConfig.getTopLevelTypesOrdering(),
                unifiedConfig.getFormatting(),
                unifiedConfig.getHeaderLine());
    }

    /**
     * Entry point: builds all root groups and assigns post-order indices in one pass.
     * Indices are assigned AFTER children (post-order). Start index can be 0.
     */
    @NonNull
    private static List<CompiledGroup> compileTopLevelGroups(@NonNull List<UnifiedMemberGroup> unifiedRoots) {
        int currentIndex = 0;
        List<CompiledGroup> compiledRoots = new ArrayList<>(unifiedRoots.size());
        for (UnifiedMemberGroup unifiedRoot : unifiedRoots) {
            Pair<CompiledGroup, Integer> rootResult = compileGroupRecursively(unifiedRoot, currentIndex);
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
    private static Pair<CompiledGroup, Integer> compileGroupRecursively(
            @NonNull UnifiedMemberGroup unifiedGroup, int startIndex) {
        int runningIndex = startIndex;

        // 1) Build children first (DFS), threading the index forward
        List<CompiledGroup> compiledChildren =
                new ArrayList<>(unifiedGroup.getMemberSubGroups().size());
        for (UnifiedMemberGroup unifiedChild : unifiedGroup.getMemberSubGroups()) {
            Pair<CompiledGroup, Integer> childResult = compileGroupRecursively(unifiedChild, runningIndex);
            compiledChildren.add(childResult.getLeft());
            runningIndex = childResult.getRight(); // advance by everything created inside the child
        }

        // 2) Compile selector/sorting for the current node (whatever your project uses)
        CompiledSelectorBlock compiledSelectorBlock = compileSelectorBlock(unifiedGroup.getSelectorBlock());
        CompiledGroupSortingBehavior compiledSortingBehavior =
                compileSortingBehavior(unifiedGroup.getSortingBehavior());

        // 3) Assign post-order index to THIS node and advance index
        int assignedPostOrderIndex = runningIndex;
        int nextFreeIndex = runningIndex + 1;

        // 4) Build the current compiled group (assuming builder has postOrderIndex or similar)
        CompiledGroup compiledCurrentGroup = CompiledGroup.builder()
                .name(unifiedGroup.getGroupName())
                .selectorBlock(compiledSelectorBlock)
                .groupSortingBehavior(compiledSortingBehavior)
                .compiledSubGroups(compiledChildren)
                .orderIndex(assignedPostOrderIndex) // <-- your field; rename if different
                .build();

        // 5) Return pair: (group, next index after this node)
        return Pair.of(compiledCurrentGroup, nextFreeIndex);
    }

    @NonNull
    private static CompiledSelectorBlock compileSelectorBlock(UnifiedSelectorBlock selectorBlock) {
        var includes = selectorBlock.getIncludes().stream()
                .map(RuleLineCompiler::compileRuleLine)
                .toList();
        var excludes = selectorBlock.getExcludes().stream()
                .map(RuleLineCompiler::compileRuleLine)
                .toList();
        return new CompiledSelectorBlock(includes, excludes);
    }

    // TODO Complete model and mapper
    @NonNull
    private static CompiledGroupSortingBehavior compileSortingBehavior(UnifiedSortingBehavior unifiedSortingBehavior) {
        SortKey sortKey = mapSortKeys(unifiedSortingBehavior.getUnifiedSortKeys());
        boolean keepAccessorsTogether = unifiedSortingBehavior.isKeepAccessorsTogether();
        // Separator handling can be added to Effective model later; keep a placeholder string for now (null ==
        // unspecified).
        return new CompiledGroupSortingBehavior(sortKey, keepAccessorsTogether);
    }

    // TODO Complete model and mapper
    @NonNull
    private static SortKey mapSortKeys(List<UnifiedSortingBehavior.UnifiedSortKey> unifiedSortKeys) {
        // We currently support a single effective sort key knob; pick the first meaningful item.
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
