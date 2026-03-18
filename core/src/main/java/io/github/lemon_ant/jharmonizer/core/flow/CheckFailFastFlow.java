package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
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

    public CheckFailFastFlow(@NonNull Formatter formatter, @NonNull Sorter sorter) {
        super(formatter, sorter, FlowType.CHECK_FAIL_FAST);
    }

    /**
     * Processes the source.
     * @param srcFile the source file
     * @return the result
     */
    @Override
    public @NonNull FlowProcessingResult processSource(@NonNull SrcFile srcFile) {
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parse(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(srcFile, parsingResult, true, null, "", "all harmonization checks");
        }

        SortingAndSerializationResult sortingAndSerializationResult =
                sortOrReuseOriginalSource(srcFile, parsedSpoonAstModel, "sorting checks");
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
                        sortingAndSerializationResult
                                .getSerializationResult()
                                .getSerializedSourceWithSkippedTypeRanges()
                                .getSerializedSrcCode());

        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(srcFile.getPath(), elementRelocations);
        }

        FormattingResult formattingResult = getFormatter()
                .formatSource(
                        sortingAndSerializationResult
                                .getSerializationResult()
                                .getSerializedSourceWithSkippedTypeRanges()
                                .getSerializedSrcCode(),
                        srcFile.getPath(),
                        sortingAndSerializationResult
                                .getSerializationResult()
                                .getFormattingSkippedRanges(
                                        sortedSpoonAstModel.getOptOuts().getFormattingSkippedTypes()));
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
                .serializationStatistic(
                        sortingAndSerializationResult.getSerializationResult().getSerializationStatistic())
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }
}
