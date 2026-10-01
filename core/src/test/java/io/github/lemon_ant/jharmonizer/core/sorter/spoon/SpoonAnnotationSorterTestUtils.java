// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Gives cross-package tests access to the production annotation sorting and group assembly pipeline. */
@UtilityClass
public class SpoonAnnotationSorterTestUtils {

    /**
     * Sorts groups without excluded type scopes.
     * @param groups scanned annotation groups
     * @param comparator requested annotation order
     * @return complete immutable groups ready for printing
     */
    @NonNull
    public static List<AnnotationSrcGroup> sortAnnotationGroups(
            @NonNull List<AnnotationSrcGroup> groups, @NonNull Comparator<AnnotationDescriptor> comparator) {
        return SpoonAnnotationSorter.sort(groups, Set.of(), comparator).getElementsInSortedOrder();
    }
}
