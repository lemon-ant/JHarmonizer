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

/**
 * Result of sorting all members in a single source file.
 * Bundles the working model after sorting, selected annotation orders, relocation diagnostics, change flags, and timing.
 * The model retains its shared mutable AST; selected annotation orders remain separate from its source groups.
 */
@Value
public class SortingResult {

    @NonNull
    List<MemberRelocation> memberRelocations;

    /** Whether this sorting invocation changed Spoon declaration order, including top-level types. */
    boolean membersReordered;

    @NonNull
    Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart;

    /** Same model supplied to the sorter, after its shared AST's declaration order has been sorted in place. */
    @NonNull
    SpoonAstModel sortedSpoonAstModel;

    @NonNull
    SortingStatistic sortingStatistic;

    /**
     * Retains sorting output with an unmodifiable view of its member diagnostics.
     * @param memberRelocations the requested diagnostics; callers must not modify this list after handoff
     * @param membersReordered whether this invocation changed Spoon declaration order, including top-level types
     * @param sortedAnnotationSrcFragmentsByGroupSrcStart read-only changed annotation orders by original group start
     * @param sortedSpoonAstModel the supplied working model after in-place declaration sorting
     * @param sortingStatistic the sorting and relocation-detection timing
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

    /**
     * Reports whether this invocation selected a different annotation order for any source group.
     * @return whether the changed-order map is non-empty
     */
    public boolean isAnnotationsReordered() {
        return !sortedAnnotationSrcFragmentsByGroupSrcStart.isEmpty();
    }

    /**
     * Reports whether declaration or annotation order changed.
     * @return whether either independent sorting flag is set
     */
    public boolean isReordered() {
        return isAnnotationsReordered() || membersReordered;
    }
}
