package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
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
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtElement;

/**
 * Aggregated result of processing one source file through a flow.
 * Captures per-phase statistics (parsing, sorting, serialization, formatting),
 * the outcome status, detected element relocations, and a unified diff string.
 *
 * <p>The {@code stopRequested} flag signals that the entire processing pipeline
 * should be stopped after this file. This is used by the fail-fast check flow to
 * gracefully halt further processing while still preserving accumulated statistics.
 */
@Value
@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileProcessingResult {
    // TODO Make it non null???
    @Nullable
    String diff;

    @NonNull
    FileProcessingStatus fileProcessingStatus;

    @NonNull
    FormattingStatistic formattingStatistic;

    @NonNull
    ParsingStatistic parsingStatistic;

    @NonNull
    Path path;

    // TODO Make it non null
    @Nullable
    Collection<Pair<CtElement, Integer>> relocations;

    @NonNull
    SerializationStatistic serializationStatistic;

    boolean stopRequested;

    @NonNull
    SortingStatistic sortingStatistic;
}
