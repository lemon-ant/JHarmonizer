package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.NonNull;

/**
 * Flow that rewrites source files in-place according to the configured ordering and formatting rules.
 * Optionally renames the original file to a backup before overwriting it.
 */
public class RestructureFlow extends AbstractOptOutFlow {

    private final boolean backupsEnabled;

    public RestructureFlow(@NonNull Formatter formatter, boolean backupsEnabled, @NonNull Sorter sorter) {
        super(formatter, sorter, RESTRUCTURE);
        this.backupsEnabled = backupsEnabled;
    }

    /**
     * Processes the source.
     *
     * @param srcFile the source file
     * @return the result
     */
    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SrcFile srcFile) {
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parse(srcFile);
        if (parsingResult.getSpoonAstModel().getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(srcFile, parsingResult, false, null, null, "all harmonization");
        }

        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        SortingSerializationAndFormattingResult sortingSerializationAndFormattingResult =
                sortAndFormatSource(srcFile, parsedSpoonAstModel, "sorting");
        SortingAndSerializationResult sortingAndSerializationResult =
                sortingSerializationAndFormattingResult.getSortingAndSerializationResult();
        FormatingResult formattingResult = sortingSerializationAndFormattingResult.getFormattingResult();
        SpoonAstModel sortedSpoonAstModel = sortingSerializationAndFormattingResult.getSortedSpoonAstModel();

        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormatedSrcCode());
        if (hasChanges) {
            if (backupsEnabled) {
                SourceFilesHandler.renameToBackup(srcFile.getPath());
            }
            SourceFilesHandler.overwrite(srcFile.getPath(), formattingResult.getFormatedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingAndSerializationResult.getSerializationResult().getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        !sortingAndSerializationResult.isSortingSkipped()
                                && isRelocated(
                                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                        sortedSpoonAstModel.getCompilationUnit()),
                        hasChanges,
                        false))
                .build();
    }
}
