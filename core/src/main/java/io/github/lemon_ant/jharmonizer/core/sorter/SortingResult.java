// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;

/** Sorted source context, changed annotation orders, requested member diagnostics, and timing for one file. */
@Value
public class SortingResult {

    @NonNull
    List<MemberRelocation> memberRelocations;

    /** Whether Spoon declaration order changed, including top-level types. */
    boolean membersReordered;

    /** Sorted fragment lists for changed annotation groups, indexed by original group start. */
    @NonNull
    Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart;

    /** The input model with its shared AST sorted in place. */
    @NonNull
    SpoonAstModel sortedSpoonAstModel;

    @NonNull
    SortingStatistic sortingStatistic;

    /**
     * Retains the sorting output. The supplied diagnostics list backs a read-only view and must stay unchanged.
     * @param memberRelocations requested member diagnostics
     * @param membersReordered whether Spoon declaration order changed
     * @param sortedAnnotationSrcFragmentsByGroupSrcStart read-only map of changed annotation orders
     * @param sortedSpoonAstModel working model after declaration sorting
     * @param sortingStatistic time for sorting and requested diagnostics
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification =
                    "The sorter hands off an unmodifiable map and unmodifiable fragment lists; retain that view.")
    public SortingResult(
            @NonNull List<MemberRelocation> memberRelocations,
            boolean membersReordered,
            @NonNull Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart,
            @NonNull SpoonAstModel sortedSpoonAstModel,
            @NonNull SortingStatistic sortingStatistic) {
        this.memberRelocations = Collections.unmodifiableList(memberRelocations);
        this.membersReordered = membersReordered;
        this.sortedAnnotationSrcFragmentsByGroupSrcStart = sortedAnnotationSrcFragmentsByGroupSrcStart;
        this.sortedSpoonAstModel = sortedSpoonAstModel;
        this.sortingStatistic = sortingStatistic;
    }

    /** Returns whether any source annotation group changed order. */
    public boolean isAnnotationsReordered() {
        return !sortedAnnotationSrcFragmentsByGroupSrcStart.isEmpty();
    }

    /** Returns whether any declaration or annotation order changed. */
    public boolean isReordered() {
        return isAnnotationsReordered() || membersReordered;
    }
}
