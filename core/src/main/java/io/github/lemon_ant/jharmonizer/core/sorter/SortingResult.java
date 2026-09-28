// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of sorting all members in a single compilation unit.
 * Bundles the reordered Spoon AST model, prepared relocation diagnostics, a combined change flag, and timing statistics.
 */
@Value
public class SortingResult {

    @NonNull
    List<MemberRelocation> memberRelocations;

    /** Whether declaration or annotation order differs from the original source order. */
    boolean relocationsDetected;

    @NonNull
    SpoonAstModel sortedSpoonAstModel;

    @NonNull
    SortingStatistic sortingStatistic;

    /**
     * Retains sorting output with an unmodifiable view of its member diagnostics.
     * @param memberRelocations the prepared diagnostics; callers must not modify this list after handoff
     * @param relocationsDetected whether declaration or annotation order differs from the original source order
     * @param sortedSpoonAstModel the working model after sorting
     * @param sortingStatistic the sorting and relocation-detection timing
     */
    public SortingResult(
            @NonNull List<MemberRelocation> memberRelocations,
            boolean relocationsDetected,
            @NonNull SpoonAstModel sortedSpoonAstModel,
            @NonNull SortingStatistic sortingStatistic) {
        this.memberRelocations = Collections.unmodifiableList(memberRelocations);
        this.relocationsDetected = relocationsDetected;
        this.sortedSpoonAstModel = sortedSpoonAstModel;
        this.sortingStatistic = sortingStatistic;
    }
}
