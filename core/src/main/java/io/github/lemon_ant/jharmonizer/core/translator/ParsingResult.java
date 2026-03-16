package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of parsing one Java source file into a Spoon AST model.
 * Bundles the parsed model with its associated timing and size statistics.
 */
@Value
public class ParsingResult {
    @NonNull
    ParsingStatistic parsingStatistic;

    @NonNull
    SpoonAstModel spoonAstModel;

    /**
     * Checks whether this parsing result matches another object.
     * @param obj the obj
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ParsingResult) obj;
        return Objects.equals(this.spoonAstModel, that.spoonAstModel)
                && Objects.equals(this.parsingStatistic, that.parsingStatistic);
    }

    /**
     * Returns the hash code of this parsing result.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = spoonAstModel.hashCode();
        result = 31 * result + parsingStatistic.hashCode();
        return result;
    }
}
