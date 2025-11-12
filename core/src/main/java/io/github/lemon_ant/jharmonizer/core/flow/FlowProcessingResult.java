package io.github.lemon_ant.jharmonizer.core.flow;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.file.Path;
import java.util.Collection;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@Value
public class FlowProcessingResult {
    @NonNull
    Path path;

    @Nullable
    Collection<Pair<CtElement, Integer>> relocations;

    @Nullable
    String diff;

    @NonNull
    ParsingStatistic parsingStatistic;

    @NonNull
    SortingStatistic sortingStatistic;

    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    FormatingStatistic formatingStatistic;
}
