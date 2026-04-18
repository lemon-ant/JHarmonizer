package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus.defineFileProcessingStatus;
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
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.JvmShutdownSignal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

/**
 * Flow that signals pipeline stop when the first ordering or formatting violation is detected.
 * Instead of throwing exceptions, returns a {@link FileProcessingResult} with
 * {@code stopRequested = true} so the pipeline can gracefully shut down
 * while preserving all accumulated statistics.
 *
 * <p>Overrides {@link #processStream} to add early termination via
 * {@code takeWhile} (before processing) and stop-flag propagation via
 * {@code peek} (after processing), so files are not processed once a
 * violation has been detected.
 */
public class CheckFailFastFlow extends AbstractOptOutFlow {

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
     * Extends the base stream pipeline with early termination logic.
     * The {@code takeWhile} gate is placed before the per-file processing map
     * so that remaining source files are not processed once a violation has
     * been detected. The {@code peek} after processing sets the stop flag
     * when a result requests stop.
     *
     * @param srcFiles the stream of source files to process
     * @return a stream that terminates early when a violation is detected
     */
    @Override
    @NonNull
    public Stream<FileProcessingResult> processStream(@NonNull Stream<SrcFile> srcFiles) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        return srcFiles.takeWhile(srcFile -> !stopFlag.get() && !JvmShutdownSignal.isShuttingDown())
                .map(this::processSrcSafely)
                .peek(fileProcessingResult -> {
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
    protected FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
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
        List<Pair<CtElement, Integer>> elementRelocations = sortingAndSerializationResult.isSortingSkipped()
                ? List.of()
                : findRelocations(
                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                        sortedSpoonAstModel.getCompilationUnit());

        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.SORTED,
                        sortingAndSerializationResult.getSerializedSrcCode());

        if (!elementRelocations.isEmpty()) {
            return FileProcessingResult.builder()
                    .path(srcFile.getPath())
                    .relocations(elementRelocations)
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
                    .relocations(List.of())
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
                .relocations(null)
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
        FormattingResult formattingResult = formatSrcWithoutSorting(srcFile, exception.getMessage());
        boolean hasFormattingChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        String srcDiff =
                hasFormattingChanges ? computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode()) : "";
        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(List.of())
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
