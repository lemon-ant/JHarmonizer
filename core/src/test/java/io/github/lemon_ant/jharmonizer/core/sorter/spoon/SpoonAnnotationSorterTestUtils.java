// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroupRenderer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Gives cross-package tests access to annotation ordering and prepared-fragment rendering. */
@UtilityClass
public class SpoonAnnotationSorterTestUtils {

    /**
     * Finds a changed order or retains the group's original annotation sequence.
     * @param group original scanned group
     * @param annotationOrderByGroupSrcStart changed orders keyed by group source start
     * @return selected order of existing fragments
     */
    @NonNull
    public static List<AnnotationSrcFragment> findAnnotationOrder(
            @NonNull AnnotationSrcGroup group,
            @NonNull Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart) {
        return annotationOrderByGroupSrcStart.getOrDefault(
                group.getStart(), group.getAnnotationSrcFragmentsInSrcOrder());
    }

    /**
     * Renders a group's selected order through the production renderer.
     * @param group original scanned group
     * @param annotationOrderByGroupSrcStart changed orders keyed by group source start
     * @return replacement text assembled from scanner-prepared fragments
     */
    @NonNull
    public static String renderAnnotationGroup(
            @NonNull AnnotationSrcGroup group,
            @NonNull Map<Integer, List<AnnotationSrcFragment>> annotationOrderByGroupSrcStart) {
        return AnnotationSrcGroupRenderer.renderReplacement(
                group, annotationOrderByGroupSrcStart.get(group.getStart()));
    }

    /**
     * Sorts groups without excluded type scopes.
     * @param groups scanned annotation groups
     * @param comparator requested annotation order
     * @return immutable changed annotation orders keyed by original group start
     */
    @NonNull
    public static Map<Integer, List<AnnotationSrcFragment>> sortAnnotationGroups(
            @NonNull List<AnnotationSrcGroup> groups, @NonNull Comparator<AnnotationDescriptor> comparator) {
        return SpoonAnnotationSorter.sort(groups, Set.of(), comparator);
    }
}
