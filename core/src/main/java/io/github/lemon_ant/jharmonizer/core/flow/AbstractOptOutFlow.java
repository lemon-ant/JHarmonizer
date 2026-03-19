package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.optout.OptOutFormattingRangeResolver;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
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
    protected final SortingAndSerializationResult sortAndSerializeOrReuseOriginalSource(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription) {
        Optional<JHarmonizerOptOutMode> fileOptOutMode =
                parsedSpoonAstModel.getOptOuts().getFileOptOutMode();
        if (fileOptOutMode.isPresent()
                && switch (fileOptOutMode.get()) {
                    case FULLY_OFF, SORTING_OFF -> true;
                }) {
            JHarmonizerOptOutMode mode = fileOptOutMode.get();
            logFileOptOutSkip(srcFile, skippedOperationDescription, mode);
            String originalSrcCode = srcFile.getSrcCode();
            return new SortingAndSerializationResult(
                    new SortingResult(parsedSpoonAstModel, new SortingStatistic(0)),
                    new SerializationResult(
                            new SerializationStatistic(originalSrcCode.length(), 0),
                            new SerializedSourceWithSkippedTypeRanges(originalSrcCode, Map.of())),
                    true);
        }

        SortingResult sortingResult = getSorter().sort(parsedSpoonAstModel);
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        return new SortingAndSerializationResult(sortingResult, serializationResult, false);
    }

    /**
     * Performs the shared sorting, serialization, formatting, and debug-stage recording pipeline.
     *
     * @param srcFile the source file being processed
     * @param parsedSpoonAstModel the parsed Spoon AST model for the source file
     * @param sortingDescription the human-readable sorting description used in skip logging
     * @return the combined sorting and formatting pipeline result
     */
    @NonNull
    protected final SortingSerializationAndFormattingResult sortSerializeAndFormatSource(
            @NonNull SrcFile srcFile, @NonNull SpoonAstModel parsedSpoonAstModel, @NonNull String sortingDescription) {
        SortingAndSerializationResult sortingAndSerializationResult =
                sortAndSerializeOrReuseOriginalSource(srcFile, parsedSpoonAstModel, sortingDescription);
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.SORTED,
                        sortingAndSerializationResult
                                .getSerializationResult()
                                .getSerializedSourceWithSkippedTypeRanges()
                                .getSerializedSrcCode());
        FormattingResult formattingResult = getFormatter()
                .formatSource(
                        sortingAndSerializationResult
                                .getSerializationResult()
                                .getSerializedSourceWithSkippedTypeRanges()
                                .getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                parsedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult
                                        .getSerializationResult()
                                        .getSerializedSourceWithSkippedTypeRanges()));
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());
        return new SortingSerializationAndFormattingResult(sortingAndSerializationResult, formattingResult);
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
                .formattingStatistic(
                        new FormattingStatistic(srcFile.getSrcCode().length(), 0))
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, checkingOnly))
                .build();
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

    @Value
    static class SortingAndSerializationResult {
        @NonNull
        SortingResult sortingResult;

        @NonNull
        SerializationResult serializationResult;

        boolean sortingSkipped;

        @NonNull
        SpoonAstModel getSortedSpoonAstModel() {
            return sortingResult.getSortedSpoonAstModel();
        }
    }

    @Value
    static class SortingSerializationAndFormattingResult {
        @NonNull
        SortingAndSerializationResult sortingAndSerializationResult;

        @NonNull
        FormattingResult formattingResult;

        @NonNull
        SpoonAstModel getSortedSpoonAstModel() {
            return sortingAndSerializationResult.getSortedSpoonAstModel();
        }
    }
}
