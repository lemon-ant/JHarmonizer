package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
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
import java.util.List;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

/**
 * Flow that stops immediately when the first ordering or formatting violation is detected.
 * Throws {@link NotOrderedException} if member order is wrong, or
 * {@link NotFormattedException} if the formatted output differs from the original.
 */
public class CheckFailFastFlow extends AbstractOptOutFlow {

    /**
     * Creates a flow that stops at the first ordering or formatting violation in a source file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     * @param printerConfig the printer configuration
     */
    public CheckFailFastFlow(
            @NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        super(formatter, sorter, printerConfig, FlowType.CHECK_FAIL_FAST);
    }

    /**
     * Processes the source.
     * @param srcFile the source file
     * @return the result
     */
    @Override
    public @NonNull FlowProcessingResult processSrc(@NonNull SrcFile srcFile) {
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
            throw new NotOrderedException(srcFile.getPath(), elementRelocations);
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
            throw new NotFormattedException(srcFile.getPath(), srcDiff);
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff("")
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }

    @NonNull
    private FlowProcessingResult processSrcWithFormattingOnlyFallback(
            @NonNull SrcFile srcFile, @NonNull SpoonModelBuildException exception) {
        FormattingResult formattingResult = formatSrcWithoutSorting(srcFile, exception.getMessage());
        if (!srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode())) {
            String srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode());
            throw new NotFormattedException(srcFile.getPath(), srcDiff);
        }
        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff("")
                .parsingStatistic(buildSyntheticParsingStatistic(srcFile))
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }
}
