package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.SKIPPED;

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
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("PMD.CouplingBetweenObjects")
abstract class AbstractOptOutFlow implements IFlow {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractOptOutFlow.class);

    @NonNull
    private final Formatter formatter;

    @NonNull
    private final Sorter sorter;

    @NonNull
    private final PrinterConfig printerConfig;

    @NonNull
    private final FlowDebugStageRecorder debugStageRecorder;

    protected AbstractOptOutFlow(
            @NonNull Formatter formatter,
            @NonNull Sorter sorter,
            @NonNull PrinterConfig printerConfig,
            @NonNull FlowType flowType) {
        this.formatter = formatter;
        this.sorter = sorter;
        this.printerConfig = printerConfig;
        this.debugStageRecorder = new FlowDebugStageRecorder(flowType);
    }

    @NonNull
    protected final SortingAndSerializationResult sortAndSerializeOrReuseOriginalSrc(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription) {
        Optional<JHarmonizerOptOutMode> fileOptOutMode =
                parsedSpoonAstModel.getOptOuts().getFileOptOutMode();
        boolean reuseOriginalSrc = fileOptOutMode
                .map(mode -> mode == JHarmonizerOptOutMode.FULLY_OFF || mode == JHarmonizerOptOutMode.SORTING_OFF)
                .orElse(false);
        if (reuseOriginalSrc) {
            JHarmonizerOptOutMode reuseMode = fileOptOutMode.orElseThrow();
            logFileOptOutSkip(srcFile, skippedOperationDescription, reuseMode);
            String originalSrcCode = srcFile.getSrcCode();
            return new SortingAndSerializationResult(
                    new SortingResult(parsedSpoonAstModel, new SortingStatistic(0)),
                    new SerializationResult(
                            new SerializationStatistic(originalSrcCode.length(), 0),
                            new SerializedSrcWithSkippedTypeRanges(
                                    originalSrcCode,
                                    reuseMode == JHarmonizerOptOutMode.SORTING_OFF
                                            ? OptOutFormattingRangeResolver.resolveFullyOffTypeRanges(
                                                    parsedSpoonAstModel.getOptOuts(), originalSrcCode)
                                            : Map.of())),
                    true);
        }

        SortingResult sortingResult = getSorter().sort(parsedSpoonAstModel);
        SerializationResult serializationResult = SrcAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
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
    protected final SortingSerializationAndFormattingResult sortSerializeAndFormatSrc(
            @NonNull SrcFile srcFile, @NonNull SpoonAstModel parsedSpoonAstModel, @NonNull String sortingDescription) {
        SortingAndSerializationResult sortingAndSerializationResult =
                sortAndSerializeOrReuseOriginalSrc(srcFile, parsedSpoonAstModel, sortingDescription);
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.SORTED,
                        sortingAndSerializationResult.getSerializedSrcCode());
        FormattingResult formattingResult = getFormatter()
                .formatSrc(
                        sortingAndSerializationResult.getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                parsedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult.getSerializedSrcWithSkippedTypeRanges()));
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());
        return new SortingSerializationAndFormattingResult(sortingAndSerializationResult, formattingResult);
    }

    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    protected final FormattingResult formatSrcWithoutSorting(@NonNull SrcFile srcFile, @NonNull String failureMessage) {
        LOG.warn(
                "Skipping sorting for {} because Spoon model creation failed ({}). Trying formatting only.",
                srcFile.getPath(),
                failureMessage);
        FormattingResult formattingResult =
                getFormatter().formatSrc(srcFile.getSrcCode(), srcFile.getPath(), List.of());
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());
        return formattingResult;
    }

    @NonNull
    protected static ParsingStatistic buildSyntheticParsingStatistic(@NonNull SrcFile srcFile) {
        String srcCode = srcFile.getSrcCode();
        return new ParsingStatistic(srcCode.length(), srcCode.getBytes(StandardCharsets.UTF_8).length, 0, 0, 0, 0);
    }

    @NonNull
    protected static FlowProcessingResult buildFullyOffFileSkippedResult(
            @NonNull SrcFile srcFile,
            @NonNull ParsingResult parsingResult,
            @NonNull String skippedOperationDescription) {
        logFileOptOutSkip(srcFile, skippedOperationDescription, JHarmonizerOptOutMode.FULLY_OFF);
        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(
                        new FormattingStatistic(srcFile.getSrcCode().length(), 0))
                .flowProcessingStatus(SKIPPED)
                .build();
    }

    private static void logFileOptOutSkip(
            SrcFile srcFile, String skippedOperationDescription, JHarmonizerOptOutMode optOutMode) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(
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
        SerializedSrcWithSkippedTypeRanges getSerializedSrcWithSkippedTypeRanges() {
            return serializationResult.getSerializedSrcWithSkippedTypeRanges();
        }

        @NonNull
        String getSerializedSrcCode() {
            return getSerializedSrcWithSkippedTypeRanges().getSerializedSrcCode();
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
