package io.github.lemon_ant.jharmonizer.core.flow;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.file.Path;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

/**
 * Aggregated result of processing one source file through a flow.
 * Captures per-phase statistics (parsing, sorting, serialization, formatting),
 * the outcome status, detected element relocations, and a unified diff string.
 */
@Value
@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowProcessingResult {
    @Nullable
    String diff;

    @NonNull
    FlowProcessingStatus flowProcessingStatus;

    @NonNull
    FormatingStatistic formatingStatistic;

    @NonNull
    ParsingStatistic parsingStatistic;

    @NonNull
    Path path;

    @Nullable
    Collection<Pair<CtElement, Integer>> relocations;

    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    SortingStatistic sortingStatistic;
}
