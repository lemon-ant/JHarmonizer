package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.BooleanUtils;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Root coordinator for sorting:
 * - (later) reorders top-level types in the compilation unit,
 * - recursively processes nested types (depth-first),
 * - applies member sorting to each type via
 */
@AllArgsConstructor
public class SpoonSorter {
    private static final int SINGLE_TOP_LEVEL_TYPE_COUNT = 1;

    @NonNull
    CompiledConfig compiledConfig;

    /**
     * Flattens blocks into a single member list preserving the block order.
     * Group boundary markers are expected to be applied before flattening.
     */
    @NonNull
    private static List<CtTypeMember> flattenMembers(List<MemberGroupBlock> memberGroupBlocks) {
        return memberGroupBlocks.stream()
                .flatMap(memberGroupBlock -> memberGroupBlock.getTypeMembers().stream())
                .toList();
    }

    /**
     * Entry point used by Sorter: sorts all types (top-level + nested) in the compilation unit.
     */
    public void sortCompilationUnitRecursively(@NonNull CtCompilationUnit compilationUnit) {
        reorderTopLevelTypes(compilationUnit, compiledConfig.getTopLevelTypesOrdering());

        compilationUnit.getDeclaredTypes().forEach(this::sortTypeRecursively);
    }

    private static void reorderTopLevelTypes(
            CtCompilationUnit compilationUnit, CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() <= SINGLE_TOP_LEVEL_TYPE_COUNT) {
            return;
        }

        CtType<?> mainType =
                compiledTopLevelTypesOrdering.isMainTypeFirst() ? SpoonTypeUtils.findMainType(compilationUnit) : null;
        Comparator<SortableTypeMember.OrderingKey> orderingComparator =
                ComparatorUtils.buildOrderingComparator(compiledTopLevelTypesOrdering.getOrderingRules());
        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.OrderingKey.getOrderingKeyProvider();
        Comparator<CtType<?>> declaredTypeComparator = Comparator.<CtType<?>>comparingInt(type ->
                        compareMainTypePriority(type, mainType, compiledTopLevelTypesOrdering.isMainTypeFirst()))
                .thenComparingInt(type -> findTopLevelTypeGroupIndex(type, compiledTopLevelTypesOrdering))
                .thenComparing(orderingKeyProvider, orderingComparator);

        List<CtType<?>> sortedDeclaredTypes =
                declaredTypes.stream().sorted(declaredTypeComparator).toList();
        compilationUnit.setDeclaredTypes(sortedDeclaredTypes);
    }

    private static int findTopLevelTypeGroupIndex(
            CtType<?> topLevelType, CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering) {
        MemberDescriptor topLevelTypeDescriptor = SpoonMemberDescriptorFactory.describeMember(topLevelType);
        List<Predicate<MemberDescriptor>> topLevelTypesSelectors =
                compiledTopLevelTypesOrdering.getTopLevelTypesSelectors();

        for (int selectorIndex = 0; selectorIndex < topLevelTypesSelectors.size(); selectorIndex++) {
            Predicate<MemberDescriptor> topLevelTypeSelector = topLevelTypesSelectors.get(selectorIndex);
            if (topLevelTypeSelector.test(topLevelTypeDescriptor)) {
                return selectorIndex;
            }
        }

        return topLevelTypesSelectors.size();
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static int compareMainTypePriority(CtType<?> topLevelType, CtType<?> mainType, boolean mainTypeFirst) {
        return mainTypeFirst ? BooleanUtils.toInteger(topLevelType != mainType) : 0;
    }

    /**
     * Depth-first recursion:
     * 1) process nested types,
     * 2) sort current type members,
     * 3) apply group boundary markers and flatten.
     * <p>
     * This order keeps the logic deterministic and ensures nested types are already "clean"
     * when the outer type is printed.
     */
    private void sortTypeRecursively(CtType<?> currentType) {
        MemberDescriptor topLevelTypeDescriptor = SpoonMemberDescriptorFactory.describeMember(currentType);

        CompiledMemberGroup rootMemberGroup = compiledConfig
                .matchRootGroup(topLevelTypeDescriptor)
                .orElseThrow(() ->
                        new IllegalStateException("No matching root member group for top-level type: qualifiedName="
                                + currentType.getQualifiedName()
                                + ", descriptor=" + topLevelTypeDescriptor));
        currentType.getNestedTypes().forEach(this::sortTypeRecursively);

        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor =
                SpoonMemberDescriptorFactory.describeMembers(currentType);

        Map<CtTypeMember, CompiledMemberGroup> naturalGroupByMember =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, typeMember2Descriptor);

        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(naturalGroupByMember);

        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupByMember, memberDependencyGraph);

        List<MemberGroupBlock> memberGroupBlocks =
                TypeMemberGrouper.groupMembersByEffectiveGroups(effectiveGroupByMember);

        List<MemberGroupBlock> orderedMemberGroupBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(memberGroupBlocks, memberDependencyGraph);

        GroupBoundaryMarker.markGroupBoundaries(orderedMemberGroupBlocks);

        List<CtTypeMember> flattenedSortedMembers = flattenMembers(orderedMemberGroupBlocks);

        // Apply to Spoon model.
        currentType.setTypeMembers(flattenedSortedMembers);
    }
}
