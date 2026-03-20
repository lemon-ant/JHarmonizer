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
import lombok.AllArgsConstructor;
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
    protected final ParsingResult recordOriginalStageAndParseSource(@NonNull SrcFile srcFile) {
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        return SourceAstTranslator.parse(srcFile);
    }

    @NonNull
    protected final SortingAndSerializationResult sortAndSerializeOrReuseOriginalSource(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription) {
        Optional<JHarmonizerOptOutMode> fileOptOutMode =
                parsedSpoonAstModel.getOptOuts().getFileOptOutMode();
        boolean reuseOriginalSource = fileOptOutMode
                .map(mode -> mode == JHarmonizerOptOutMode.FULLY_OFF || mode == JHarmonizerOptOutMode.SORTING_OFF)
                .orElse(false);
        if (reuseOriginalSource) {
            JHarmonizerOptOutMode reuseMode = fileOptOutMode.orElseThrow();
            logFileOptOutSkip(srcFile, skippedOperationDescription, reuseMode);
            String originalSrcCode = srcFile.getSrcCode();
            return new SortingAndSerializationResult(
                    new SortingResult(parsedSpoonAstModel, new SortingStatistic(0)),
                    SerializationResult.skippedWithoutSerialization(
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
                        sortingAndSerializationResult.getSerializedSrcCode());
        FormattingResult formattingResult = getFormatter()
                .formatSource(
                        sortingAndSerializationResult.getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                parsedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult.getSerializedSourceWithSkippedTypeRanges()));
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());
        return new SortingSerializationAndFormattingResult(sortingAndSerializationResult, formattingResult);
    }

    @NonNull
    protected static FlowProcessingResult buildFullyOffFileSkippedResult(
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
                        FormattingStatistic.skipped(srcFile.getSrcCode().length()))
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
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
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

        @NonNull
        SerializationStatistic getSerializationStatistic() {
            return serializationResult.getSerializationStatistic();
        }

        @NonNull
        SerializedSourceWithSkippedTypeRanges getSerializedSourceWithSkippedTypeRanges() {
            return serializationResult.getSerializedSourceWithSkippedTypeRanges();
        }

        @NonNull
        String getSerializedSrcCode() {
            return getSerializedSourceWithSkippedTypeRanges().getSerializedSrcCode();
        }
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SortingSerializationAndFormattingResult {
        @NonNull
        SortingAndSerializationResult sortingAndSerializationResult;

        @NonNull
        FormattingResult formattingResult;

        @NonNull
        SpoonAstModel getSortedSpoonAstModel() {
            return sortingAndSerializationResult.getSortedSpoonAstModel();
        }

        @NonNull
        String getFormattedSrcCode() {
            return formattingResult.getFormattedSrcCode();
        }

        @NonNull
        FormattingStatistic getFormattingStatistic() {
            return formattingResult.getFormattingStatistic();
        }
    }
}
