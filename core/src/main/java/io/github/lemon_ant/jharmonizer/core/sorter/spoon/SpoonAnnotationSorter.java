// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.OrderChangeCollector.ElementOrdering;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationGapLayout;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

/** Orders original annotation fragments for source-preserving printing. */
@UtilityClass
class SpoonAnnotationSorter {
    private static final int MINIMUM_SORTABLE_ANNOTATION_COUNT = 2;

    /**
     * Sorts annotations within source groups without changing group order, respecting disabled type scopes.
     * The result's change flag reports annotation reordering within any group, not group permutations.
     * @param annotationSrcGroupsInSrcOrder source annotation groups in their original scope order
     * @param sortingSkippedTypes types excluded by opt-out directives
     * @param annotationComparator precompiled annotation comparator
     * @return immutable groups in source order and their annotation-order change flag
     */
    @NonNull
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static ElementOrdering<AnnotationSrcGroup> sort(
            @NonNull List<AnnotationSrcGroup> annotationSrcGroupsInSrcOrder,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull Comparator<AnnotationDescriptor> annotationComparator) {
        List<AnnotationSrcGroup> preparedAnnotationSrcGroupsInSrcOrder =
                new ArrayList<>(annotationSrcGroupsInSrcOrder.size());
        boolean annotationsReordered = false;
        for (AnnotationSrcGroup annotationSrcGroup : annotationSrcGroupsInSrcOrder) {
            AnnotationSrcGroup preparedAnnotationSrcGroup =
                    sortAnnotationSrcFragments(annotationSrcGroup, sortingSkippedTypes, annotationComparator);
            preparedAnnotationSrcGroupsInSrcOrder.add(preparedAnnotationSrcGroup);
            // A group is replaced only when its annotations move; the group's source slot stays unchanged.
            annotationsReordered |= preparedAnnotationSrcGroup != annotationSrcGroup;
        }
        return new ElementOrdering<>(
                Collections.unmodifiableList(preparedAnnotationSrcGroupsInSrcOrder), annotationsReordered);
    }

    @NonNull
    private static AnnotationSrcGroup assembleAnnotationSrcGroupWithSortedAnnotations(
            AnnotationSrcGroup annotationSrcGroupBeforeSorting,
            List<AnnotationSrcFragment> annotationSrcFragmentsInSortedOrder) {
        List<AnnotationSrcGap> preparedAnnotationSrcGapsInSrcOrder = IntStream.range(
                        0, annotationSrcFragmentsInSortedOrder.size())
                .mapToObj(destinationSlotIndex -> prepareGapForPrecedingAnnotation(
                        annotationSrcFragmentsInSortedOrder.get(destinationSlotIndex),
                        annotationSrcGroupBeforeSorting
                                .getAnnotationSrcGapsInSrcOrder()
                                .get(destinationSlotIndex)))
                .toList();
        return new AnnotationSrcGroup(
                annotationSrcFragmentsInSortedOrder,
                preparedAnnotationSrcGapsInSrcOrder,
                annotationSrcGroupBeforeSorting.getStart(),
                annotationSrcGroupBeforeSorting.getEndExclusive());
    }

    private static boolean containsAnnotation(SourcePosition typePosition, int annotationStartOffset) {
        return typePosition.isValidPosition()
                && typePosition.getSourceStart() <= annotationStartOffset
                && annotationStartOffset <= typePosition.getSourceEnd();
    }

    /** Prepares a destination gap for the annotation that precedes it in the sorted group. */
    @NonNull
    private static AnnotationSrcGap prepareGapForPrecedingAnnotation(
            AnnotationSrcFragment preceedingAnnotationSrcFragment, AnnotationSrcGap destinationAnnotationSrcGap) {
        AnnotationGapLayout destinationGapLayout = destinationAnnotationSrcGap.getLayout();
        String selectedWhitespacePrefix;
        if (preceedingAnnotationSrcFragment.getEndExclusive() == destinationAnnotationSrcGap.getStart()) {
            // The annotation originally preceded this gap. Restore its original prefix, even after repeated sorts.
            selectedWhitespacePrefix = destinationGapLayout.getLeadingWhitespace();
        } else if (preceedingAnnotationSrcFragment.getTrailingLineSeparatorCount()
                > destinationGapLayout.getRetainedLineSeparatorCount()) {
            // The gap lacks the line breaks required after this annotation's trailing comments.
            // Copy its prepared separators to end // comments or preserve the blank line after a lower comment block,
            // then use the destination indentation for the following token.
            selectedWhitespacePrefix =
                    preceedingAnnotationSrcFragment.getTrailingLineSeparators() + destinationGapLayout.getIndentation();
        } else {
            // The gap already has enough retained separators, or this annotation needs none.
            // Use the prepared relocation prefix: original whitespace, or one line break plus indentation
            // when the original annotation takes its attached lower comment block and blank lines with it.
            selectedWhitespacePrefix = destinationGapLayout.getRelocatedLeadingWhitespace();
        }
        return new AnnotationSrcGap(
                destinationAnnotationSrcGap.getEndExclusive(),
                destinationGapLayout,
                selectedWhitespacePrefix,
                destinationAnnotationSrcGap.getStart());
    }

    @NonNull
    private static AnnotationSrcGroup sortAnnotationSrcFragments(
            AnnotationSrcGroup annotationSrcGroup,
            Set<CtType<?>> sortingSkippedTypes,
            Comparator<AnnotationDescriptor> annotationComparator) {
        List<AnnotationSrcFragment> originalAnnotationSrcFragments = annotationSrcGroup.getAnnotationSrcFragments();
        if (originalAnnotationSrcFragments.size() < MINIMUM_SORTABLE_ANNOTATION_COUNT
                || sortingSkippedTypes.stream()
                        .anyMatch(skippedType -> containsAnnotation(
                                skippedType.getPosition(),
                                originalAnnotationSrcFragments.get(0).getAnnotationStart()))) {
            return annotationSrcGroup;
        }
        ElementOrdering<AnnotationSrcFragment> annotationOrdering = OrderChangeCollector.collectOrderedElements(
                originalAnnotationSrcFragments.size(),
                originalAnnotationSrcFragments.iterator(),
                originalAnnotationSrcFragments.stream()
                        .sorted(Comparator.comparing(AnnotationSrcFragment::getDescriptor, annotationComparator)));
        return annotationOrdering.isReordered()
                ? assembleAnnotationSrcGroupWithSortedAnnotations(
                        annotationSrcGroup, annotationOrdering.getElementsInSortedOrder())
                : annotationSrcGroup;
    }
}
