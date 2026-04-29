package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of sorting all members in a single compilation unit.
 * Bundles the reordered Spoon AST model with the associated timing statistics.
 */
@Value
public class SortingResult {
    @NonNull
    SpoonAstModel sortedSpoonAstModel;

    @NonNull
    SortingStatistic sortingStatistic;
}
