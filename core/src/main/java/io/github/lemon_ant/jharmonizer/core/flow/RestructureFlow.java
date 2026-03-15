package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.NonNull;

public class RestructureFlow extends AbstractOptOutFlow {

    private final boolean backupsEnabled;

    public RestructureFlow(@NonNull Formatter formatter, boolean backupsEnabled, @NonNull Sorter sorter) {
        super(formatter, sorter, RESTRUCTURE);
        this.backupsEnabled = backupsEnabled;
    }

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile sourceFile) {
        ParsingResult parsingResult = parseSourceFile(sourceFile);
        if (parsingResult
                .getSpoonAstModel()
                .getOptOuts()
                .hasFileOptOutMode(io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(sourceFile, parsingResult, false, null, null, "all harmonization");
        }

        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        SortingPassResult sortingPassResult = sortOrReuseOriginalSource(sourceFile, parsedSpoonAstModel, "sorting");
        SpoonAstModel sortedSpoonAstModel = sortingPassResult.getSortedSpoonAstModel();
        recordSortedStage(sourceFile, sortingPassResult.getSerializationResult());

        FormatingResult formattingResult = formatter.formatSource(
                sortingPassResult.getSerializationResult().getSerializedSrcCode(),
                sourceFile.getPath(),
                sortingPassResult.getSerializationResult().getFormattingExclusionRanges());
        recordFormattedStage(sourceFile, formattingResult);

        boolean hasSourceChanges = !sourceFile.getSrcCode().equals(formattingResult.getFormatedSrcCode());
        if (hasSourceChanges) {
            if (backupsEnabled) {
                SourceFilesHandler.renameToBackup(sourceFile.getPath());
            }
            SourceFilesHandler.overwrite(sourceFile.getPath(), formattingResult.getFormatedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(sourceFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingPassResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingPassResult.getSerializationResult().getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        !sortingPassResult.isSortingSkipped()
                                && isRelocated(
                                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                        sortedSpoonAstModel.getCompilationUnit()),
                        hasSourceChanges,
                        false))
                .build();
    }
}
