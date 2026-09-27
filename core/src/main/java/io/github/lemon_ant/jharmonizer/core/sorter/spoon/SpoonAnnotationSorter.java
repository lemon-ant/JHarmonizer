// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcGroup;
import java.util.Comparator;
import java.util.List;
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
     * Returns sorted source annotation groups, respecting disabled type scopes.
     * @param sortingSkippedTypes types excluded by opt-out directives
     * @param annotationSrcGroups source annotation groups from the processing model
     * @param annotationComparator precompiled annotation comparator
     * @return immutable groups with enabled annotations sorted and other groups preserved
     */
    @NonNull
    static List<AnnotationSrcGroup> sort(
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull List<AnnotationSrcGroup> annotationSrcGroups,
            @NonNull Comparator<AnnotationDescriptor> annotationComparator) {
        // The printer reads annotation order from these groups; reordering AST annotation lists would duplicate the
        // work.
        return annotationSrcGroups.stream()
                .map(annotationSrcGroup ->
                        sortAnnotationFragments(annotationSrcGroup, sortingSkippedTypes, annotationComparator))
                .toList();
    }

    private static boolean containsAnnotation(SourcePosition typePosition, int annotationStartOffset) {
        return typePosition.isValidPosition()
                && typePosition.getSourceStart() <= annotationStartOffset
                && annotationStartOffset <= typePosition.getSourceEnd();
    }

    @NonNull
    private static AnnotationSrcGroup sortAnnotationFragments(
            AnnotationSrcGroup annotationSrcGroup,
            Set<CtType<?>> sortingSkippedTypes,
            Comparator<AnnotationDescriptor> annotationComparator) {
        List<AnnotationSrcFragment> annotationSrcFragments = annotationSrcGroup.getFragmentsInPrintOrder();
        if (annotationSrcFragments.size() < MINIMUM_SORTABLE_ANNOTATION_COUNT
                || sortingSkippedTypes.stream()
                        .anyMatch(skippedType -> containsAnnotation(
                                skippedType.getPosition(),
                                annotationSrcFragments.get(0).getStart()))) {
            return annotationSrcGroup;
        }
        return annotationSrcGroup.withFragmentsInPrintOrder(annotationSrcFragments.stream()
                .sorted(Comparator.comparing(AnnotationSrcFragment::getDescriptor, annotationComparator))
                .toList());
    }
}
