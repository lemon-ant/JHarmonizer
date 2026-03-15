package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@UtilityClass
class OptOutFlowSupport {
    static FlowProcessingResult buildFileOptOutSkippedResult(
            Path path,
            String sourceCode,
            ParsingResult parsingResult,
            boolean checkingOnly,
            @Nullable Collection<Pair<CtElement, Integer>> relocations,
            @Nullable String diff) {
        return FlowProcessingResult.builder()
                .path(path)
                .relocations(relocations)
                .diff(diff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(new SerializationStatistic(sourceCode.length(), 0))
                .formatingStatistic(new FormatingStatistic(sourceCode.length(), 0))
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, checkingOnly))
                .build();
    }

    static SerializationResult createOriginalSourceSerializationResult(String sourceCode) {
        return new SerializationResult(new SerializationStatistic(sourceCode.length(), 0), sourceCode, List.of());
    }
}
