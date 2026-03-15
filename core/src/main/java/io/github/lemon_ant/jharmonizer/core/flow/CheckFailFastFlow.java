package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

public class CheckFailFastFlow extends AbstractOptOutFlow {

    public CheckFailFastFlow(@NonNull Formatter formatter, @NonNull Sorter sorter) {
        super(formatter, sorter, FlowType.CHECK_FAIL_FAST);
    }

    @Override
    public @NonNull FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile sourceFile) {
        ParsingResult parsingResult = parseSourceFile(sourceFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel
                .getOptOuts()
                .hasFileOptOutMode(io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(sourceFile, parsingResult, true, null, "", "all harmonization checks");
        }

        SortingPassResult sortingPassResult =
                sortOrReuseOriginalSource(sourceFile, parsedSpoonAstModel, "sorting checks");
        SpoonAstModel sortedSpoonAstModel = sortingPassResult.getSortedSpoonAstModel();
        List<Pair<CtElement, Integer>> elementRelocations = sortingPassResult.isSortingSkipped()
                ? List.of()
                : findRelocations(
                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                        sortedSpoonAstModel.getCompilationUnit());

        recordSortedStage(sourceFile, sortingPassResult.getSerializationResult());

        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(sourceFile.getPath(), elementRelocations);
        }

        FormatingResult formattingResult = formatter.formatSource(
                sortingPassResult.getSerializationResult().getSerializedSrcCode(),
                sourceFile.getPath(),
                sortingPassResult.getSerializationResult().getFormattingExclusionRanges());
        recordFormattedStage(sourceFile, formattingResult);

        if (!sourceFile.getSrcCode().equals(formattingResult.getFormatedSrcCode())) {
            String sourceDiff = computeDiff(sourceFile.getSrcCode(), formattingResult.getFormatedSrcCode());
            throw new NotFormattedException(sourceFile.getPath(), sourceDiff);
        }

        return FlowProcessingResult.builder()
                .path(sourceFile.getPath())
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
