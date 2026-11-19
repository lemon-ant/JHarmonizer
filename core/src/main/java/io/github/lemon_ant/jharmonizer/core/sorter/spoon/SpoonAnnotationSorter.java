// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.findEffectiveMemberStart;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.OrderChangeCollector.ElementOrdering;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.sorting.Dependencies;
import io.github.lemon_ant.jharmonizer.sorting.Dependencies.Dependency;
import io.github.lemon_ant.jharmonizer.sorting.Groups;
import io.github.lemon_ant.jharmonizer.sorting.SimplifiedDependencyAwareSorter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

/** Orders original annotation fragments for source-preserving printing. */
@UtilityClass
class SpoonAnnotationSorter {
    private static final int MINIMUM_SORTABLE_ANNOTATION_COUNT = 2;

    /**
     * Sorts annotations within source groups, respecting confirmed and potential dependencies and disabled type scopes.
     * Retains each original group and records only permutations that change its annotation order.
     * @param annotationSrcGroupsInSrcOrder source annotation groups in their original scope order
     * @param sortingSkippedTypes types excluded by opt-out directives
     * @param annotationComparator precompiled annotation comparator
     * @return read-only changed annotation orders indexed by original group source start; empty when no order changes
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static Map<Integer, List<AnnotationSrcFragment>> sort(
            @NonNull List<AnnotationSrcGroup> annotationSrcGroupsInSrcOrder,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull Comparator<AnnotationDescriptor> annotationComparator) {
        Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart = new HashMap<>();
        for (AnnotationSrcGroup annotationSrcGroup : annotationSrcGroupsInSrcOrder) {
            ElementOrdering<AnnotationSrcFragment> annotationOrdering = sortAnnotationSrcFragments(
                    annotationSrcGroup.getAnnotationSrcFragmentsInSrcOrder(),
                    sortingSkippedTypes,
                    annotationComparator);
            if (annotationOrdering.isReordered()) {
                annotationOrderByGroupSrcStart.put(
                        annotationSrcGroup.getStart(), annotationOrdering.getElementsInSortedOrder());
            }
        }
        return annotationOrderByGroupSrcStart.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(annotationOrderByGroupSrcStart);
    }

    private static boolean containsAnnotation(CtType<?> type, AnnotationSrcFragment annotationSrcFragment) {
        SourcePosition typePosition = type.getPosition();
        int annotationStartOffset = annotationSrcFragment.getAnnotationStart();
        return typePosition.isValidPosition()
                && findEffectiveMemberStart(type) <= annotationStartOffset
                && annotationStartOffset <= typePosition.getSourceEnd();
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static List<AnnotationSrcFragment> orderAnnotationSrcFragments(
            List<AnnotationSrcFragment> originalAnnotationSrcFragments,
            Comparator<AnnotationDescriptor> annotationComparator) {
        Map<String, AnnotationSrcFragment> previousAnnotationByOrderPreservationGroupKey = new HashMap<>();
        List<Dependency<AnnotationSrcFragment>> dependencies = new ArrayList<>();
        for (AnnotationSrcFragment annotationSrcFragment : originalAnnotationSrcFragments) {
            for (String orderPreservationGroupKey :
                    annotationSrcFragment.getDescriptor().getOrderPreservationGroupKeys()) {
                AnnotationSrcFragment previousAnnotation = previousAnnotationByOrderPreservationGroupKey.put(
                        orderPreservationGroupKey, annotationSrcFragment);
                if (previousAnnotation != null) {
                    dependencies.add(new Dependency<>(annotationSrcFragment, previousAnnotation));
                }
            }
        }
        // Java exposes repeated and mixed direct/container annotations in source order. Encode that order as
        // dependencies: overriding comparisons for annotations with the same group key would violate transitivity.
        return SimplifiedDependencyAwareSorter.sort(
                originalAnnotationSrcFragments,
                Groups.empty(),
                new Dependencies<>(dependencies),
                Comparator.comparing(AnnotationSrcFragment::getDescriptor, annotationComparator));
    }

    @NonNull
    private static ElementOrdering<AnnotationSrcFragment> sortAnnotationSrcFragments(
            List<AnnotationSrcFragment> originalAnnotationSrcFragments,
            Set<CtType<?>> sortingSkippedTypes,
            Comparator<AnnotationDescriptor> annotationComparator) {
        if (originalAnnotationSrcFragments.size() < MINIMUM_SORTABLE_ANNOTATION_COUNT
                || sortingSkippedTypes.stream()
                        .anyMatch(skippedType ->
                                containsAnnotation(skippedType, originalAnnotationSrcFragments.get(0)))) {
            return new ElementOrdering<>(originalAnnotationSrcFragments, false);
        }
        return OrderChangeCollector.collectOrderedElements(
                originalAnnotationSrcFragments.size(),
                originalAnnotationSrcFragments.iterator(),
                orderAnnotationSrcFragments(originalAnnotationSrcFragments, annotationComparator).stream());
    }
}
