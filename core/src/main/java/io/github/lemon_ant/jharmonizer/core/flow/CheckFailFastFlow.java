package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus.defineFileProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildFullyOffFileSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildSyntheticParsingStatistic;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_FAIL_FAST;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.optout.OptOutFormattingRangeResolver;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Flow that signals pipeline stop when the first ordering or formatting violation is detected.
 * Instead of throwing exceptions, returns a {@link FileProcessingResult} with
 * {@code stopRequested = true} so the pipeline can gracefully shut down
 * while preserving all accumulated statistics.
 *
 * <p>Overrides {@link #preCheckSrcFiles} to skip remaining files as soon as a violation
 * has been detected (before the expensive mapping step), and overrides
 * {@link #postProcessResults} to propagate the stop flag via {@code peek} after
 * each result passes through.
 */
public class CheckFailFastFlow extends AbstractOptOutFlow {

    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    /**
     * Creates a flow that signals stop at the first ordering or formatting violation in a source file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     * @param printerConfig the printer configuration
     */
    public CheckFailFastFlow(
            @NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        super(formatter, sorter, printerConfig, CHECK_FAIL_FAST);
    }

    /**
     * Extends the base JVM-shutdown pre-check with an early stop-flag guard,
     * so that no further source files are processed once a violation has been detected.
     * The stop-flag is set by {@link #postProcessResults} after the first violating result passes through.
     *
     * @param srcFiles the incoming stream of source files
     * @return a stream that skips remaining files once the stop flag is set
     */
    @Override
    @NonNull
    protected Stream<SrcFile> preCheckSrcFiles(@NonNull Stream<SrcFile> srcFiles) {
        return super.preCheckSrcFiles(srcFiles).takeWhile(srcFile -> !stopFlag.get());
    }

    /**
     * Sets the stop flag after each result that requests stop passes through,
     * so subsequent files are skipped by the pre-check phase.
     *
     * @param results the stream of per-file results from the mapping phase
     * @return the same stream, with stop-flag propagation via {@code peek}
     */
    @Override
    @NonNull
    protected Stream<FileProcessingResult> postProcessResults(@NonNull Stream<FileProcessingResult> results) {
        return results.peek(fileProcessingResult -> {
            if (fileProcessingResult.isStopRequested()) {
                stopFlag.set(true);
            }
        });
    }

    /**
     * Processes the source file and returns a result that may signal pipeline stop.
     *
     * @param srcFile the source file
     * @return the result, with {@code stopRequested = true} if a violation was detected
     */
    @Override
    @NonNull
    FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
        getDebugStageRecorder().recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult;
        try {
            parsingResult = SrcAstTranslator.parse(srcFile, getPrinterConfig());
        } catch (SpoonModelBuildException exception) {
            return processSrcWithFormattingOnlyFallback(srcFile, exception);
        }
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, "all harmonization checks");
        }

        SortingAndSerializationResult sortingAndSerializationResult =
                sortAndSerializeOrReuseOriginalSrc(srcFile, parsedSpoonAstModel, "sorting checks");
        SpoonAstModel sortedSpoonAstModel = sortingAndSerializationResult.getSortedSpoonAstModel();
        getDebugStageRecorder()
            .recordSrcStage(
                srcFile.getPath(),
                FlowDebugStageRecorder.SrcFlowStage.SORTED,
                sortingAndSerializationResult.getSerializedSrcCode());

        List<MemberRelocation> memberRelocations = sortingAndSerializationResult.isSortingSkipped()
                ? List.of()
                : findRelocations(
                        sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit());
        if (!memberRelocations.isEmpty()) {
            return FileProcessingResult.builder()
                    .path(srcFile.getPath())
                    .memberRelocations(memberRelocations)
                    .diff("")
                    .parsingStatistic(parsingResult.getParsingStatistic())
                    .sortingStatistic(
                            sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                    .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                    .formattingStatistic(new FormattingStatistic(0, 0))
                    .fileProcessingStatus(defineFileProcessingStatus(true, false, true))
                    .stopRequested(true)
                    .build();
        }

        FormattingResult formattingResult = getFormatter()
                .formatSrc(
                        sortingAndSerializationResult.getSerializedSrcCode(),
                        srcFile.getPath(),
                        OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                                sortedSpoonAstModel.getOptOuts(),
                                sortingAndSerializationResult.getSerializedSrcWithSkippedTypeRanges()));
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormattedSrcCode());

        if (!srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode())) {
            String srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode());
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
                    .stopRequested(true)
                    .build();
        }

        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(null)
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
    private FileProcessingResult processSrcWithFormattingOnlyFallback(
            @NonNull SrcFile srcFile, @NonNull SpoonModelBuildException exception) {
        FormattingResult formattingResult = formatSrcAfterModelBuildFailure(srcFile, exception.getMessage());
        boolean hasFormattingChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        String srcDiff =
                hasFormattingChanges ? computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode()) : "";
        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(List.of())
                .diff(srcDiff)
                .parsingStatistic(buildSyntheticParsingStatistic(srcFile))
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .fileProcessingStatus(defineFileProcessingStatus(false, hasFormattingChanges, true))
                .stopRequested(hasFormattingChanges)
                .build();
    }

    @Override
    public boolean isSuccessful(boolean hasModifications) {
        return !hasModifications;
    }

    @Override
    public boolean isModifyingFlow() {
        return false;
    }
}
