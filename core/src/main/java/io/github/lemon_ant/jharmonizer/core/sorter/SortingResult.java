package io.github.lemon_ant.jharmonizer.core.sorter;

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

    /**
     * Checks whether this sorting result matches another object.
     * @param obj the obj
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SortingResult) obj;
        return Objects.equals(this.sortedSpoonAstModel, that.sortedSpoonAstModel)
                && Objects.equals(this.sortingStatistic, that.sortingStatistic);
    }

    /**
     * Returns the hash code of this sorting result.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = sortedSpoonAstModel.hashCode();
        result = 31 * result + sortingStatistic.hashCode();
        return result;
    }
}
