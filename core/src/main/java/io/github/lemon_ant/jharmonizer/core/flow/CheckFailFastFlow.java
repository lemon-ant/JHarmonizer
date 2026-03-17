package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
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
    public @NonNull FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile) {
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(srcFile, parsingResult, true, null, "", "all harmonization checks");
        }

        SortingPassResult sortingPassResult = sortOrReuseOriginalSource(srcFile, parsedSpoonAstModel, "sorting checks");
        SpoonAstModel sortedSpoonAstModel = sortingPassResult.getSortedSpoonAstModel();
        List<Pair<CtElement, Integer>> elementRelocations = sortingPassResult.isSortingSkipped()
                ? List.of()
                : findRelocations(
                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                        sortedSpoonAstModel.getCompilationUnit());

        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.SORTED,
                        sortingPassResult.getSerializationResult().getSerializedSrcCode());

        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(srcFile.getPath(), elementRelocations);
        }

        FormatingResult formattingResult = getFormatter()
                .formatSource(
                        sortingPassResult.getSerializationResult().getSerializedSrcCode(),
                        srcFile.getPath(),
                        sortingPassResult.getSerializationResult().getFormattingExclusionRanges());
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.FORMATTED,
                        formattingResult.getFormatedSrcCode());

        if (!srcFile.getSrcCode().equals(formattingResult.getFormatedSrcCode())) {
            String srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormatedSrcCode());
            throw new NotFormattedException(srcFile.getPath(), srcDiff);
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff("")
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingPassResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingPassResult.getSerializationResult().getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }
}
