package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class SpoonSorter {
    private static final int MAX_MEMBERS_WITHOUT_SORTING = 1;

    // TODO Try to remove this field and make the class static util
    @NonNull
    private final CompiledConfig compiledConfig;

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

    public void sortCompilationUnitRecursively(
            @NonNull CtCompilationUnit compilationUnit, @NonNull Set<CtType<?>> sortingSkippedTypes) {
        reorderTopLevelTypes(compilationUnit, compiledConfig.getTopLevelTypesOrdering());
        File srcFile = compilationUnit.getFile();
        compilationUnit.getDeclaredTypes().forEach(type -> sortTypeRecursively(type, sortingSkippedTypes, srcFile));
    }

    private static void reorderTopLevelTypes(
            CtCompilationUnit compilationUnit, CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() <= MAX_MEMBERS_WITHOUT_SORTING) {
            return;
        }

        CtType<?> mainType =
                compiledTopLevelTypesOrdering.isMainTypeFirst() ? SpoonTypeUtils.findMainType(compilationUnit) : null;
        Comparator<MemberOrderingKey> orderingComparator =
                ComparatorUtils.buildMemberOnlyOrderingComparator(compiledTopLevelTypesOrdering.getOrderingRules());
        Comparator<CtType<?>> declaredTypeComparator =
                createTopLevelTypesComparator(compiledTopLevelTypesOrdering, mainType, orderingComparator);

        List<CtType<?>> sortedDeclaredTypes =
                declaredTypes.stream().sorted(declaredTypeComparator).toList();
        compilationUnit.setDeclaredTypes(sortedDeclaredTypes);
    }

    @NonNull
    private static Comparator<CtType<?>> createTopLevelTypesComparator(
            CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering,
            CtType<?> mainType,
            Comparator<MemberOrderingKey> orderingComparator) {
        // Top-level types are not members of a group, so accessor clustering never applies
        // at this level: we use the member-only comparator and a simple memoizing key provider.
        Function<CtTypeMember, MemberOrderingKey> orderingKeyProvider = OrderingKeyFactory.createOrderingKeyProvider();
        return Comparator.<CtType<?>>comparingInt(type ->
                        compareMainTypePriority(type, mainType, compiledTopLevelTypesOrdering.isMainTypeFirst()))
                .thenComparingInt(type -> findTopLevelTypeGroupIndex(type, compiledTopLevelTypesOrdering))
                .thenComparing(orderingKeyProvider, orderingComparator);
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
    private void sortTypeRecursively(
            CtType<?> currentType, Set<CtType<?>> sortingSkippedTypes, @Nullable File srcFile) {
        if (sortingSkippedTypes.contains(currentType)) {
            return;
        }

        MemberDescriptor topLevelTypeDescriptor = SpoonMemberDescriptorFactory.describeMember(currentType);

        CompiledMemberGroup rootMemberGroup = compiledConfig
                .matchRootGroup(topLevelTypeDescriptor)
                .orElseThrow(() ->
                        new IllegalStateException("No matching root member group for top-level type: qualifiedName="
                                + currentType.getQualifiedName()
                                + ", descriptor=" + topLevelTypeDescriptor));
        if (log.isDebugEnabled()) {
            log.debug(
                    "Root group selected: file={}, class={}, group={}",
                    srcFile != null ? srcFile.getPath() : "<virtual>",
                    currentType.getQualifiedName(),
                    rootMemberGroup.getName() != null ? rootMemberGroup.getName() : "<unnamed>");
        }
        currentType
                .getNestedTypes()
                .forEach(nestedType -> sortTypeRecursively(nestedType, sortingSkippedTypes, srcFile));
        currentType.setTypeMembers(sortTypeMembers(currentType, rootMemberGroup));
    }

    @NonNull
    private static List<CtTypeMember> sortTypeMembers(CtType<?> type, CompiledMemberGroup rootMemberGroup) {
        if (type.getTypeMembers().size() <= MAX_MEMBERS_WITHOUT_SORTING) {
            return type.getTypeMembers();
        }

        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor = SpoonMemberDescriptorFactory.describeMembers(type);

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
        return flattenMembers(orderedMemberGroupBlocks);
    }
}
