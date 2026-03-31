package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of parsing one Java source file into a Spoon AST model.
 * Bundles the parsed model with its associated timing and source statistics
 * (source-length and UTF-8 byte size).
 */
@Value
public class ParsingResult {
    @NonNull
    ParsingStatistic parsingStatistic;

    @NonNull
    SpoonAstModel spoonAstModel;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ParsingResult) obj;
        return Objects.equals(this.spoonAstModel, that.spoonAstModel)
                && Objects.equals(this.parsingStatistic, that.parsingStatistic);
    }

    @Override
    public int hashCode() {
        int result = spoonAstModel.hashCode();
        result = 31 * result + parsingStatistic.hashCode();
        return result;
    }
}
