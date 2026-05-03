/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildFormattingOnlyFallbackResult;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.optout.OptOutFormattingRangeResolver;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.JvmShutdownSignal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter(AccessLevel.PROTECTED)
abstract class AbstractOptOutFlow implements IFlow {

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

    /**
     * Processes a single source file with the current flow strategy.
     *
     * @param srcFile the source file to process
     * @return the processing result for the source file
     */
    @NonNull
    abstract FileProcessingResult processSrc(@NonNull SrcFile srcFile);

    /**
     * Processes a stream of source files through three explicit phases:
     * <ol>
     *   <li><b>Pre-check</b> — delegates to {@link #preCheckSrcFiles} for any flow-specific
     *       filtering (default: skips files when a JVM shutdown signal is detected).</li>
     *   <li><b>Mapping</b> — applies per-file processing for each source file.</li>
     *   <li><b>Post-processing</b> — delegates to {@link #postProcessResults} for any
     *       flow-specific result-stream transformations.</li>
     * </ol>
     *
     * @param srcFiles the stream of source files to process
     * @return a stream of per-file processing results
     */
    @Override
    @NonNull
    public final Stream<FileProcessingResult> processStream(@NonNull Stream<SrcFile> srcFiles) {
        Stream<SrcFile> preCheckedSrcFiles = preCheckSrcFiles(srcFiles);
        Stream<FileProcessingResult> mappedResults = preCheckedSrcFiles.map(this::processSrcSafely);
        return postProcessResults(mappedResults);
    }

    /**
     * Hook for subclasses to apply pre-processing filters to the source file stream.
     * The default implementation skips remaining files when a JVM shutdown signal is detected.
     * Subclasses may override to add additional filtering, and should call
     * {@code super.preCheckSrcFiles(srcFiles)} to preserve the base shutdown guard.
     *
     * @param srcFiles the incoming stream of source files
     * @return the filtered stream of source files to process
     */
    @NonNull
    protected Stream<SrcFile> preCheckSrcFiles(@NonNull Stream<SrcFile> srcFiles) {
        return srcFiles.takeWhile(srcFile -> !JvmShutdownSignal.isShuttingDown());
    }

    /**
     * Hook for subclasses to apply post-mapping transformations to the result stream.
     * The default implementation returns the stream unchanged.
     * Subclasses may override to add steps such as early-termination signalling.
     *
     * @param results the stream of per-file processing results from the mapping phase
     * @return the post-processed result stream
     */
    @NonNull
    protected Stream<FileProcessingResult> postProcessResults(@NonNull Stream<FileProcessingResult> results) {
        return results;
    }

    @NonNull
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.GuardLogStatement"})
    private FileProcessingResult processSrcSafely(SrcFile srcFile) {
        try {
            return processSrc(srcFile);
        } catch (RuntimeException exception) {
            log.warn(
                    "Unexpected internal processing error for file {}: {}",
                    srcFile.getPath(),
                    describeRuntimeFailure(exception));
            log.debug("Stack trace for processing error in file {}", srcFile.getPath(), exception);
            return FileProcessingResult.builder()
                    .path(srcFile.getPath())
                    .memberRelocations(List.of())
                    .diff("")
                    .parsingStatistic(FlowResultUtils.buildSyntheticParsingStatistic(srcFile))
                    .sortingStatistic(new SortingStatistic(0))
                    .serializationStatistic(new SerializationStatistic(0, 0))
                    .formattingStatistic(new FormattingStatistic(0, 0))
                    .fileProcessingStatus(FileProcessingStatus.ERROR)
                    .stopRequested(false)
                    .build();
        }
    }

    @NonNull
    private static String describeRuntimeFailure(@NonNull RuntimeException exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionType;
        }
        return exceptionType + ": " + exceptionMessage;
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
            FlowResultUtils.logFileOptOutSkip(srcFile, skippedOperationDescription, reuseMode);
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
    protected final FormattingResult formatSrcAfterModelBuildFailure(
            @NonNull SrcFile srcFile, @NonNull String failureMessage) {
        if (JvmShutdownSignal.isShuttingDown()) {
            log.debug("Skipping sorting for {} after model build failure (JVM is shutting down).", srcFile.getPath());
        } else {
            log.warn(
                    "Skipping sorting for {} because Spoon model creation failed ({}). Trying formatting only.",
                    srcFile.getPath(),
                    failureMessage);
        }
        FormattingResult formattingResult =
                getFormatter().formatSrc(srcFile.getSrcCode(), srcFile.getPath(), List.of());
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());
        return formattingResult;
    }

    /**
     * Builds a fallback processing result when Spoon model creation fails and only formatting can be applied.
     * Subclasses that must signal a stop at the first violation should override
     * {@link #isStopRequestedOnFormattingChange()} to return {@code true}.
     *
     * @param srcFile the source file whose model build failed
     * @param failureMessage the failure message from the model-build exception
     * @return the formatting-only fallback processing result
     */
    @NonNull
    protected final FileProcessingResult processSrcWithFormattingOnlyFallback(
            @NonNull SrcFile srcFile, @NonNull String failureMessage) {
        FormattingResult formattingResult = formatSrcAfterModelBuildFailure(srcFile, failureMessage);
        return buildFormattingOnlyFallbackResult(srcFile, formattingResult, isStopRequestedOnFormattingChange());
    }

    /**
     * Returns whether the stop-requested flag should be set when a formatting-only fallback detects changes.
     * The default is {@code false}; override to return {@code true} in flows that must
     * signal a stop at the first violation.
     *
     * @return {@code true} if the stop-requested flag should be set when formatting changes are detected
     */
    protected boolean isStopRequestedOnFormattingChange() {
        return false;
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
