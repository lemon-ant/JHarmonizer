package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
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

abstract class AbstractOptOutFlow implements IFlow {
    protected final Formatter formatter;
    protected final Sorter sorter;
    private final FlowDebugStageRecorder debugStageRecorder;

    protected AbstractOptOutFlow(@NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull FlowType flowType) {
        this.formatter = formatter;
        this.sorter = sorter;
        this.debugStageRecorder = new FlowDebugStageRecorder(flowType);
    }

    protected final ParsingResult parseSourceFile(@NonNull SrcFile sourceFile) {
        debugStageRecorder.recordSrcStage(sourceFile.getPath(), SrcFlowStage.ORIGINAL, sourceFile.getSrcCode());
        return SourceAstTranslator.parseSourceFile(sourceFile);
    }

    protected final void recordSortedStage(
            @NonNull SrcFile sourceFile, @NonNull SerializationResult serializationResult) {
        debugStageRecorder.recordSrcStage(
                sourceFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());
    }

    protected final void recordFormattedStage(@NonNull SrcFile sourceFile, @NonNull FormatingResult formattingResult) {
        debugStageRecorder.recordSrcStage(
                sourceFile.getPath(), SrcFlowStage.FORMATTED, formattingResult.getFormatedSrcCode());
    }

    @NonNull
    protected final SortingPassResult sortOrReuseOriginalSource(
            @NonNull SrcFile sourceFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription) {
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.SORTING_OFF)) {
            logFileOptOutSkip(sourceFile, skippedOperationDescription, JHarmonizerOptOutMode.SORTING_OFF);
            return new SortingPassResult(
                    new SortingResult(parsedSpoonAstModel, new SortingStatistic(0)),
                    createOriginalSourceSerializationResult(sourceFile),
                    true);
        }

        SortingResult sortingResult = sorter.sort(parsedSpoonAstModel);
        return new SortingPassResult(
                sortingResult, SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel()), false);
    }

    @NonNull
    protected final FlowProcessingResult buildFileOptOutSkippedResult(
            @NonNull SrcFile sourceFile,
            @NonNull ParsingResult parsingResult,
            boolean checkingOnly,
            @Nullable Collection<Pair<CtElement, Integer>> relocations,
            @Nullable String sourceDiff,
            @NonNull String skippedOperationDescription) {
        logFileOptOutSkip(sourceFile, skippedOperationDescription, JHarmonizerOptOutMode.FULLY_OFF);
        return FlowProcessingResult.builder()
                .path(sourceFile.getPath())
                .relocations(relocations)
                .diff(sourceDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(sourceFile.getSrcCode().length(), 0))
                .formatingStatistic(
                        new FormatingStatistic(sourceFile.getSrcCode().length(), 0))
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, checkingOnly))
                .build();
    }

    @NonNull
    private static SerializationResult createOriginalSourceSerializationResult(@NonNull SrcFile sourceFile) {
        return new SerializationResult(
                new SerializationStatistic(sourceFile.getSrcCode().length(), 0), sourceFile.getSrcCode(), List.of());
    }

    private void logFileOptOutSkip(
            @NonNull SrcFile sourceFile,
            @NonNull String skippedOperationDescription,
            @NonNull JHarmonizerOptOutMode optOutMode) {
        Logger logger = LoggerFactory.getLogger(getClass());
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Skipping {} for {} because of {} ({})",
                    skippedOperationDescription,
                    sourceFile.getPath(),
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
