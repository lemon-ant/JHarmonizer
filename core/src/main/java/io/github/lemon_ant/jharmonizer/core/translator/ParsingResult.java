package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
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
}
