package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

public class CheckAllFlow extends AbstractOptOutFlow {

    public CheckAllFlow(@NonNull Formatter formatter, @NonNull Sorter sorter) {
        super(formatter, sorter, CHECK_ALL);
    }

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SrcFile sourceFile) {
        ParsingResult parsingResult = parseSourceFile(sourceFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel
                .getOptOuts()
                .hasFileOptOutMode(io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(
                    sourceFile, parsingResult, true, List.of(), "", "all harmonization checks");
        }

        SortingPassResult sortingPassResult =
                sortOrReuseOriginalSource(sourceFile, parsedSpoonAstModel, "sorting checks");
        SpoonAstModel sortedSpoonAstModel = sortingPassResult.getSortedSpoonAstModel();
        recordSortedStage(sourceFile, sortingPassResult.getSerializationResult());

        FormatingResult formattingResult = formatter.formatSource(
                sortingPassResult.getSerializationResult().getSerializedSrcCode(),
                sourceFile.getPath(),
                sortingPassResult.getSerializationResult().getFormattingExclusionRanges());
        recordFormattedStage(sourceFile, formattingResult);

        boolean hasSourceChanges = !sourceFile.getSrcCode().equals(formattingResult.getFormatedSrcCode());
        List<Pair<CtElement, Integer>> elementRelocations;
        String sourceDiff;
        if (hasSourceChanges && !sortingPassResult.isSortingSkipped()) {
            elementRelocations = findRelocations(
                    sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());
            sourceDiff = computeDiff(sourceFile.getSrcCode(), formattingResult.getFormatedSrcCode());
        } else if (hasSourceChanges) {
            elementRelocations = List.of();
            sourceDiff = computeDiff(sourceFile.getSrcCode(), formattingResult.getFormatedSrcCode());
        } else {
            elementRelocations = List.of();
            sourceDiff = "";
        }

        return FlowProcessingResult.builder()
                .path(sourceFile.getPath())
                .relocations(elementRelocations)
                .diff(sourceDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingPassResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingPassResult.getSerializationResult().getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(
                        defineFlowProcessingStatus(!elementRelocations.isEmpty(), !sourceDiff.isEmpty(), true))
                .build();
    }
}
