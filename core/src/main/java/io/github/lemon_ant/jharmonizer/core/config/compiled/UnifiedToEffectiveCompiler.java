package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledGroupSortingBehavior.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

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
public class UnifiedToEffectiveCompiler {

    /**
     * Compiles a full Unified configuration into an Effective configuration.
     */
    @NonNull
    public static CompiledConfig compile(@NonNull UnifiedConfig unifiedConfig) {
        // TODO Today we only compile the member groups; top-level type ordering may feed into future phases.
        List<CompiledGroup> typeRoots = compileTopLevelGroups(unifiedConfig.getRootMemberGroups());

        return new CompiledConfig(typeRoots);
    }

    @NonNull
    private static List<CompiledGroup> compileTopLevelGroups(@NonNull List<UnifiedMemberGroup> unifiedGroups) {
        return unifiedGroups.stream()
                .map(UnifiedToEffectiveCompiler::compileGroupRecursively)
                .toList();
    }

    @NonNull
    private static CompiledGroup compileGroupRecursively(@NonNull UnifiedMemberGroup unifiedGroup) {
        String groupName = unifiedGroup.getGroupName();

        CompiledSelectorBlock compiledSelectorBlock = compileSelectorBlock(unifiedGroup.getSelectorBlock());
        CompiledGroupSortingBehavior sortingBehavior = mapSortingBehavior(unifiedGroup.getSortingBehavior());

        // Recursively compile children first (DFS order preserved)
        List<UnifiedMemberGroup> childGroups = unifiedGroup.getMemberSubGroups();
        List<CompiledGroup> compiledChildren = new ArrayList<>(childGroups.size());
        for (UnifiedMemberGroup child : childGroups) {
            compiledChildren.add(compileGroupRecursively(child));
        }

        // orderIndex will be assigned by CompiledConfig.assignPostOrderIndexes(...)
        return new CompiledGroup(groupName, /*orderIndex*/ 0, compiledSelectorBlock, sortingBehavior, compiledChildren);
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
    private static CompiledGroupSortingBehavior mapSortingBehavior(UnifiedSortingBehavior unifiedSortingBehavior) {
        SortKey sortKey = mapSortKeys(unifiedSortingBehavior.getUnifiedSortKeys());
        boolean keepAccessorsTogether = unifiedSortingBehavior.isKeepAccessorsTogether();
        // Separator handling can be added to Effective model later; keep a placeholder string for now (null ==
        // unspecified).
        return new CompiledGroupSortingBehavior(sortKey, keepAccessorsTogether, null);
    }

    // TODO Complete model and mapper
    @NonNull
    private static SortKey mapSortKeys(List<UnifiedSortKey> unifiedSortKeys) {
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
