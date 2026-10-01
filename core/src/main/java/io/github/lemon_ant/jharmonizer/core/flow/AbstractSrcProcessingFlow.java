// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus.defineFileProcessingStatus;
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
import io.github.lemon_ant.jharmonizer.core.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
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

/** Shared source-processing pipeline for reordering, checking, formatting, and failure handling. */
@Slf4j
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
abstract class AbstractSrcProcessingFlow implements IFlow {

    @NonNull
    private final Formatter formatter;

    @NonNull
    @Getter(AccessLevel.NONE)
    private final PrinterConfig printerConfig;

    @NonNull
    private final Sorter sorter;

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
    @NonNull
    @Override
    public final Stream<FileProcessingResult> processStream(@NonNull Stream<SrcFile> srcFiles) {
        Stream<SrcFile> preCheckedSrcFiles = preCheckSrcFiles(srcFiles);
        Stream<FileProcessingResult> mappedResults = preCheckedSrcFiles.map(this::processSrcSafely);
        return postProcessResults(mappedResults);
    }

    /**
     * Creates the shared source-processing pipeline.
     * @param formatter the formatter applied after sorting
     * @param sorter the declaration and annotation sorter
     * @param printerConfig the source printer settings
     */
    protected AbstractSrcProcessingFlow(
            @NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        this.formatter = formatter;
        this.sorter = sorter;
        this.printerConfig = printerConfig;
    }

    /**
     * Performs the two-step check: sorting first, then formatting only if sorting passed.
     * If sorting violations are detected the formatting step is skipped because formatting an
     * incorrectly sorted file would produce meaningless results.
     *
     * <p>The {@code stopOnViolation} flag controls whether the returned result signals a pipeline
     * stop when a violation is found. Pass {@code true} for fail-fast flows and {@code false} for
     * check-all flows.
     *
     * @param srcFile the source file being processed
     * @param parsedSpoonAstModel the parsed Spoon AST model for the source file
     * @param parsingResult the parsing result, used to populate per-phase statistics
     * @param stopOnViolation whether to set {@code stopRequested = true} when a violation is detected
     * @return the processing result for the source file
     */
    @NonNull
    protected final FileProcessingResult checkSortingThenFormattingIfOrdered(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull ParsingResult parsingResult,
            boolean stopOnViolation) {
        SortingAndSerializationResult sortingAndSerializationResult =
                sortAndSerializeOrReuseOriginalSrc(srcFile, parsedSpoonAstModel, "sorting checks", true);
        SpoonAstModel sortedSpoonAstModel = sortingAndSerializationResult.getSortedSpoonAstModel();
        SortingResult sortingResult = sortingAndSerializationResult.getSortingResult();

        List<MemberRelocation> memberRelocations = sortingResult.getMemberRelocations();
        boolean annotationsReordered = sortingResult.isAnnotationsReordered();
        if (sortingResult.isReordered()) {
            String annotationSrcDiff = annotationsReordered
                    ? computeDiff(
                            srcFile.getPath().toString(),
                            srcFile.getSrcCode(),
                            sortingAndSerializationResult.getSerializedSrcCode())
                    : "";
            return FileProcessingResult.builder()
                    .path(srcFile.getPath())
                    .memberRelocations(memberRelocations)
                    .diff(annotationSrcDiff)
                    .parsingStatistic(parsingResult.getParsingStatistic())
                    .sortingStatistic(
                            sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                    .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                    .formattingStatistic(new FormattingStatistic(0, 0))
                    .fileProcessingStatus(defineFileProcessingStatus(true, false, true))
                    .stopRequested(stopOnViolation)
                    .build();
        }

        FormattingResult formattingResult = getFormatter()
                .formatSrc(
                        sortingAndSerializationResult.getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                sortedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult.getSerializedSrcWithSkippedTypeRanges()));

        if (!srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode())) {
            String srcDiff = computeDiff(
                    srcFile.getPath().toString(), srcFile.getSrcCode(), formattingResult.getFormattedSrcCode());
            return FileProcessingResult.builder()
                    .path(srcFile.getPath())
                    .memberRelocations(List.of())
                    .diff(srcDiff)
                    .parsingStatistic(parsingResult.getParsingStatistic())
                    .sortingStatistic(
                            sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                    .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                    .formattingStatistic(formattingResult.getFormattingStatistic())
                    .fileProcessingStatus(defineFileProcessingStatus(false, true, true))
                    .stopRequested(stopOnViolation)
                    .build();
        }

        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(List.of())
                .diff("")
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .fileProcessingStatus(defineFileProcessingStatus(false, false, true))
                .stopRequested(false)
                .build();
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
        return getFormatter().formatSrc(srcFile.getSrcCode(), srcFile.getPath(), List.of());
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
     * Sorts and serializes enabled input, or preserves source text for a file-level sorting opt-out.
     * @param srcFile source file being processed
     * @param parsedSpoonAstModel freshly parsed model
     * @param skippedOperationDescription operation description for opt-out logging
     * @param collectMemberRelocations whether the flow needs the detailed member report
     * @return sorting and serialization output
     */
    @NonNull
    protected final SortingAndSerializationResult sortAndSerializeOrReuseOriginalSrc(
            @NonNull SrcFile srcFile,
            @NonNull SpoonAstModel parsedSpoonAstModel,
            @NonNull String skippedOperationDescription,
            boolean collectMemberRelocations) {
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
                    new SerializationResult(
                            new SerializationStatistic(originalSrcCode.length(), 0),
                            new SerializedSrcWithSkippedTypeRanges(
                                    originalSrcCode,
                                    reuseMode == JHarmonizerOptOutMode.SORTING_OFF
                                            ? OptOutFormattingRangeResolver.resolveFullyOffTypeRanges(
                                                    parsedSpoonAstModel.getOptOuts(), originalSrcCode)
                                            : Map.of())),
                    new SortingResult(false, List.of(), false, parsedSpoonAstModel, new SortingStatistic(0)));
        }

        SortingResult sortingResult = getSorter().sort(parsedSpoonAstModel, collectMemberRelocations);
        SerializationResult serializationResult =
                SrcAstTranslator.serialize(sortingResult.getSortedSpoonAstModel(), printerConfig);
        return new SortingAndSerializationResult(serializationResult, sortingResult);
    }

    /**
     * Performs the shared sorting, serialization, and formatting pipeline.
     *
     * @param srcFile the source file being processed
     * @param parsedSpoonAstModel the parsed Spoon AST model for the source file
     * @param sortingDescription the human-readable sorting description used in skip logging
     * @return the combined sorting and formatting pipeline result
     */
    @NonNull
    protected final SortingSerializationAndFormattingResult sortSerializeAndFormatSrc(
            @NonNull SrcFile srcFile, @NonNull SpoonAstModel parsedSpoonAstModel, @NonNull String sortingDescription) {
        // REORDER reports change flags; detailed relocation lists are only consumed by check flows.
        SortingAndSerializationResult sortingAndSerializationResult =
                sortAndSerializeOrReuseOriginalSrc(srcFile, parsedSpoonAstModel, sortingDescription, false);
        FormattingResult formattingResult = getFormatter()
                .formatSrc(
                        sortingAndSerializationResult.getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                parsedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult.getSerializedSrcWithSkippedTypeRanges()));
        return new SortingSerializationAndFormattingResult(formattingResult, sortingAndSerializationResult);
    }

    /**
     * Processes a single source file with the current flow strategy.
     *
     * @param srcFile the source file to process
     * @return the processing result for the source file
     */
    @NonNull
    abstract FileProcessingResult processSrc(@NonNull SrcFile srcFile);

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

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SortingAndSerializationResult {

        @NonNull
        SerializationResult serializationResult;

        @NonNull
        SortingResult sortingResult;

        @NonNull
        SerializationStatistic getSerializationStatistic() {
            return serializationResult.getSerializationStatistic();
        }

        @NonNull
        String getSerializedSrcCode() {
            return getSerializedSrcWithSkippedTypeRanges().getSerializedSrcCode();
        }

        @NonNull
        SerializedSrcWithSkippedTypeRanges getSerializedSrcWithSkippedTypeRanges() {
            return serializationResult.getSerializedSrcWithSkippedTypeRanges();
        }

        @NonNull
        SpoonAstModel getSortedSpoonAstModel() {
            return sortingResult.getSortedSpoonAstModel();
        }
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SortingSerializationAndFormattingResult {

        @NonNull
        FormattingResult formattingResult;

        @NonNull
        SortingAndSerializationResult sortingAndSerializationResult;

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
