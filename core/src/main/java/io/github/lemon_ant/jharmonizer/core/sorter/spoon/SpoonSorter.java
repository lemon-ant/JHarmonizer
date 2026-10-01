// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.OrderChangeCollector.ElementOrdering;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.jspecify.annotations.Nullable;
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
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class SpoonSorter {
    private static final int MAX_MEMBERS_WITHOUT_SORTING = 1;

    @NonNull
    private static final String SORTING_STARTED_METADATA = SpoonSorter.class.getName() + ".sortingStarted";

    // TODO Try to remove this field and make the class static util
    @NonNull
    private final CompiledConfig compiledConfig;

    /**
     * Sorts declaration order in the AST and finalizes sorted annotation source groups.
     * @param spoonAstModel freshly parsed model; each shared AST may be sorted only once
     * @return model with the updated AST, immutable annotation groups, and invocation change flags
     * @throws IllegalStateException if sorting was already attempted for the shared compilation unit
     */
    @NonNull
    public SpoonSortingResult sortCompilationUnitRecursively(@NonNull SpoonAstModel spoonAstModel) {
        CtCompilationUnit compilationUnit = claimCompilationUnitForSorting(spoonAstModel);
        Set<CtType<?>> sortingSkippedTypes = spoonAstModel.getOptOuts().getSortingSkippedTypes();
        boolean membersReordered = reorderTopLevelTypes(compilationUnit, compiledConfig.getTopLevelTypesOrdering());
        File srcFile = compilationUnit.getFile();
        for (CtType<?> type : compilationUnit.getDeclaredTypes()) {
            membersReordered |= sortTypeRecursively(type, sortingSkippedTypes, srcFile);
        }
        ElementOrdering<AnnotationSrcGroup> annotationOrdering = SpoonAnnotationSorter.sort(
                spoonAstModel.getAnnotationSrcGroups(), sortingSkippedTypes, compiledConfig.getAnnotationComparator());
        return new SpoonSortingResult(
                annotationOrdering.isReordered(),
                membersReordered,
                spoonAstModel.withAnnotationSrcGroups(annotationOrdering.getElementsInSortedOrder()));
    }

    @NonNull
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private static CtCompilationUnit claimCompilationUnitForSorting(SpoonAstModel spoonAstModel) {
        CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
        // Model views share this unit. Claim it before any mutation, including unchanged or failed attempts.
        // Spoon copies metadata when cloning a unit; a consumed clone also requires reparsing before sorting.
        // Only this short metadata claim uses the monitor; sorting and I/O must stay outside it.
        synchronized (compilationUnit) {
            if (Boolean.TRUE.equals(compilationUnit.getMetadata(SORTING_STARTED_METADATA))) {
                throw new IllegalStateException(
                        "Spoon AST has already been submitted for sorting: " + spoonAstModel.getPath());
            }
            compilationUnit.putMetadata(SORTING_STARTED_METADATA, Boolean.TRUE);
        }
        return compilationUnit;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static int compareMainTypePriority(CtType<?> topLevelType, CtType<?> mainType, boolean mainTypeFirst) {
        return mainTypeFirst ? BooleanUtils.toInteger(topLevelType != mainType) : 0;
    }

    @NonNull
    // TODO Move to the ComparatorUtils?
    private static Comparator<CtType<?>> createTopLevelTypesComparator(
            CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering,
            CtType<?> mainType,
            Comparator<OrderingKey> orderingComparator) {
        // Top-level types are not members of a group, so accessor clustering never applies
        // at this level: we use a simple memoizing key provider.
        Function<CtTypeMember, OrderingKey> orderingKeyProvider = OrderingKeyFactory.createOrderingKeyProvider();
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

    private static boolean reorderTopLevelTypes(
            CtCompilationUnit compilationUnit, CompiledTopLevelTypesOrdering compiledTopLevelTypesOrdering) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() <= MAX_MEMBERS_WITHOUT_SORTING) {
            return false;
        }

        CtType<?> mainType =
                compiledTopLevelTypesOrdering.isMainTypeFirst() ? SpoonTypeUtils.findMainType(compilationUnit) : null;
        Comparator<OrderingKey> orderingComparator =
                ComparatorUtils.buildOrderingKeyComparator(compiledTopLevelTypesOrdering.getOrderingRules());
        Comparator<CtType<?>> declaredTypeComparator =
                createTopLevelTypesComparator(compiledTopLevelTypesOrdering, mainType, orderingComparator);

        ElementOrdering<CtType<?>> declaredTypeOrdering = OrderChangeCollector.collectOrderedElements(
                declaredTypes.size(),
                declaredTypes.iterator(),
                declaredTypes.stream().sorted(declaredTypeComparator));
        compilationUnit.setDeclaredTypes(declaredTypeOrdering.getElementsInSortedOrder());
        return declaredTypeOrdering.isReordered();
    }

    @NonNull
    private static ElementOrdering<CtTypeMember> sortTypeMembers(CtType<?> type, CompiledMemberGroup rootMemberGroup) {
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
        return OrderChangeCollector.collectOrderedElements(
                typeMember2Descriptor.size(),
                streamExplicitSrcTypeMembers(type).iterator(),
                orderedMemberGroupBlocks.stream()
                        .flatMap(memberGroupBlock -> memberGroupBlock.getTypeMembers().stream()));
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
    private boolean sortTypeRecursively(
            CtType<?> currentType,
            Set<CtType<?>> sortingSkippedTypes, /*TODO We use only srcFile.getPath*/
            @Nullable File srcFile) {
        if (sortingSkippedTypes.contains(currentType)) {
            return false;
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
        boolean nestedMembersReordered = false;
        for (CtType<?> nestedType : currentType.getNestedTypes()) {
            nestedMembersReordered |= sortTypeRecursively(nestedType, sortingSkippedTypes, srcFile);
        }
        // Skip this scope before building a graph or result wrapper, after processing any nested scopes.
        if (currentType.getTypeMembers().size() <= MAX_MEMBERS_WITHOUT_SORTING) {
            return nestedMembersReordered;
        }
        ElementOrdering<CtTypeMember> memberOrdering = sortTypeMembers(currentType, rootMemberGroup);
        currentType.setTypeMembers(memberOrdering.getElementsInSortedOrder());
        return nestedMembersReordered || memberOrdering.isReordered();
    }

    /** Final model and order changes produced by one sorting invocation. */
    @Value
    @AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static class SpoonSortingResult {
        boolean annotationsReordered;

        /** Whether Spoon declaration order changed, including top-level types. */
        boolean membersReordered;

        @NonNull
        SpoonAstModel sortedSpoonAstModel;

        /**
         * Reports whether declaration or annotation order changed.
         * @return whether any source elements were reordered
         */
        public boolean isReordered() {
            return annotationsReordered || membersReordered;
        }
    }
}
