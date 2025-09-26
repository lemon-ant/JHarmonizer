package io.github.lemon_ant.jharmonizer.core.parser;

import io.github.lemon_ant.jharmonizer.core.parser.spoon.SpoonASTModel;
import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

@Value
public class ParsingResult {
    @NonNull
    SpoonASTModel spoonAstModel;

    @NonNull
    ParsingStatistic parsingStatistic;

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
