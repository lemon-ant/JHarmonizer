package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.Value;
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
        GroupInheritanceContext rootInheritanceContext =
                new GroupInheritanceContext(false, UnifiedSeparator.NONE, Collections.emptyList());
        for (UnifiedMemberGroup unifiedRoot : unifiedRoots) {
            Pair<CompiledMemberGroup, Integer> rootResult =
                    compileGroupRecursively(unifiedRoot, currentIndex, rootInheritanceContext);
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
            UnifiedMemberGroup unifiedGroup, int startIndex, GroupInheritanceContext inheritedContext) {
        int runningIndex = startIndex;

        // 1) Build children first (DFS), threading the index forward
        List<CompiledMemberGroup> compiledChildren =
                new ArrayList<>(unifiedGroup.getMemberSubGroups().size());
        GroupInheritanceContext effectiveContext = resolveEffectiveContext(unifiedGroup, inheritedContext);
        boolean keepAccessorsTogether = effectiveContext.isKeepAccessorsTogether();
        UnifiedSeparator separator = effectiveContext.getSeparator();
        List<UnifiedOrderingRule> effectiveOrderingRules = effectiveContext.getOrderingRules();
        for (UnifiedMemberGroup unifiedChild : unifiedGroup.getMemberSubGroups()) {
            Pair<CompiledMemberGroup, Integer> childResult =
                    compileGroupRecursively(unifiedChild, runningIndex, effectiveContext);
            compiledChildren.add(childResult.getLeft());
            runningIndex = childResult.getRight(); // advance by everything created inside the child
        }

        // 2) Compile selector/sorting for the current node (whatever your project uses)
        CompiledMemberGroupSelectorBlock compiledMemberGroupSelectorBlock =
                compileSelectorBlock(unifiedGroup.getSelectorBlock());
        List<OrderingRule> compiledOrderingRules = OrderingRuleCompiler.compileOrderingRules(effectiveOrderingRules);

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
                .separator(separator)
                .build();

        // 5) Return pair: (group, next index after this node)
        return Pair.of(compiledCurrentGroup, nextFreeIndex);
    }

    @NonNull
    private static CompiledMemberGroupSelectorBlock compileSelectorBlock(
            UnifiedMemberGroupSelectorBlock selectorBlock) {
        List<Predicate<MemberDescriptor>> includes = selectorBlock.getIncludes().stream()
                .map(MemberGroupRuleLineCompiler::compileRuleLine)
                .toList();
        List<Predicate<MemberDescriptor>> excludes = selectorBlock.getExcludes().stream()
                .map(MemberGroupRuleLineCompiler::compileRuleLine)
                .toList();
        return new CompiledMemberGroupSelectorBlock(includes, excludes);
    }

    @NonNull
    private static GroupInheritanceContext resolveEffectiveContext(
            UnifiedMemberGroup unifiedGroup, GroupInheritanceContext inheritedContext) {
        boolean keepAccessorsTogether = Optional.ofNullable(unifiedGroup.getKeepAccessorsTogether())
                .orElse(inheritedContext.isKeepAccessorsTogether());
        UnifiedSeparator separator =
                Optional.ofNullable(unifiedGroup.getSeparator()).orElse(inheritedContext.getSeparator());
        List<UnifiedOrderingRule> orderingRules =
                Optional.ofNullable(unifiedGroup.getOrderingRules()).orElse(inheritedContext.getOrderingRules());
        List<UnifiedOrderingRule> normalizedOrderingRules =
                orderingRules == null ? Collections.emptyList() : orderingRules;
        return new GroupInheritanceContext(keepAccessorsTogether, separator, normalizedOrderingRules);
    }

    @Value
    private static final class GroupInheritanceContext {
        boolean keepAccessorsTogether;

        @NonNull
        UnifiedSeparator separator;

        @NonNull
        List<UnifiedOrderingRule> orderingRules;
    }
}
