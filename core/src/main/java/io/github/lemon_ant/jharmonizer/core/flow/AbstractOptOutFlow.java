package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingStatistic;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtElement;

@Getter(AccessLevel.PROTECTED)
abstract class AbstractOptOutFlow implements IFlow {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractOptOutFlow.class);

    @NonNull
    private final Formatter formatter;

    @NonNull
    private final Sorter sorter;

    @NonNull
    private final FlowDebugStageRecorder debugStageRecorder;

    protected AbstractOptOutFlow(@NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull FlowType flowType) {
        this.formatter = formatter;
        this.sorter = sorter;
        this.debugStageRecorder = new FlowDebugStageRecorder(flowType);
    }

    @NonNull
    protected final SortingPassResult sortOrReuseOriginalSource(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription) {
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.SORTING_OFF)) {
            logFileOptOutSkip(srcFile, skippedOperationDescription, JHarmonizerOptOutMode.SORTING_OFF);
            return new SortingPassResult(
                    new SortingResult(parsedSpoonAstModel, new SortingStatistic(0)),
                    createOriginalSourceSerializationResult(srcFile),
                    true);
        }

        SortingResult sortingResult = getSorter().sort(parsedSpoonAstModel);
        return new SortingPassResult(
                sortingResult, SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel()), false);
    }

    @NonNull
    protected static FlowProcessingResult buildFileOptOutSkippedResult(
            @NonNull SrcFile srcFile,
            @NonNull ParsingResult parsingResult,
            boolean checkingOnly,
            @Nullable Collection<Pair<CtElement, Integer>> relocations,
            @Nullable String srcDiff,
            @NonNull String skippedOperationDescription) {
        logFileOptOutSkip(srcFile, skippedOperationDescription, JHarmonizerOptOutMode.FULLY_OFF);
        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(relocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formatingStatistic(new FormatingStatistic(srcFile.getSrcCode().length(), 0))
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, checkingOnly))
                .build();
    }

    @NonNull
    private static SerializationResult createOriginalSourceSerializationResult(@NonNull SrcFile srcFile) {
        return new SerializationResult(
                new SerializationStatistic(srcFile.getSrcCode().length(), 0), srcFile.getSrcCode(), List.of());
    }

    private static void logFileOptOutSkip(
            @NonNull SrcFile srcFile,
            @NonNull String skippedOperationDescription,
            @NonNull JHarmonizerOptOutMode optOutMode) {
        if (LOG.isInfoEnabled()) {
            LOG.info(
                    "Skipping {} for {} because of {} ({})",
                    skippedOperationDescription,
                    srcFile.getPath(),
                    optOutMode.getDisplayName(),
                    optOutMode.getToken());
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    protected static class SortingPassResult {
        @NonNull
        private final SortingResult sortingResult;

        @NonNull
        private final SerializationResult serializationResult;

        private final boolean sortingSkipped;

        @NonNull
        SpoonAstModel getSortedSpoonAstModel() {
            return sortingResult.getSortedSpoonAstModel();
        }
    }
}
