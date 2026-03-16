package io.github.lemon_ant.jharmonizer.core.sorter;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Objects;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SortingResult) obj;
        return Objects.equals(this.sortedSpoonAstModel, that.sortedSpoonAstModel)
                && Objects.equals(this.sortingStatistic, that.sortingStatistic);
    }

    @Override
    public int hashCode() {
        int result = sortedSpoonAstModel.hashCode();
        result = 31 * result + sortingStatistic.hashCode();
        return result;
    }
}
