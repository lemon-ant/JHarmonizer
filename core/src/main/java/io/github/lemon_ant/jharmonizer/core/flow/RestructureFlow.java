package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

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
    public FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile) {
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
        if (parsingResult.getSpoonAstModel().getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(srcFile, parsingResult, false, null, null, "all harmonization");
        }

        // CPD-OFF
        /* TODO @Copilot Investigate and fix code repetition and delete CPD comment
        [INFO] --- pmd:3.28.0:cpd-check (check-sources) @ jharmonizer-core ---
        [WARNING] CPD Failure: Found 20 lines of duplicated code at locations:
        [WARNING]     W:\JHarmonizer\core\src\main\java\io\github\lemon_ant\jharmonizer\core\flow\CheckAllFlow.java line 44
        [WARNING]     W:\JHarmonizer\core\src\main\java\io\github\lemon_ant\jharmonizer\core\flow\RestructureFlow.java line 47 */
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        SortingPassResult sortingPassResult = sortOrReuseOriginalSource(srcFile, parsedSpoonAstModel, "sorting");
        SpoonAstModel sortedSpoonAstModel = sortingPassResult.getSortedSpoonAstModel();
        getDebugStageRecorder()
                .recordSrcStage(
                        srcFile.getPath(),
                        FlowDebugStageRecorder.SrcFlowStage.SORTED,
                        sortingPassResult.getSerializationResult().getSerializedSrcCode());

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
                .sortingStatistic(sortingPassResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingPassResult.getSerializationResult().getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        !sortingPassResult.isSortingSkipped()
                                && isRelocated(
                                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                        sortedSpoonAstModel.getCompilationUnit()),
                        hasChanges,
                        false))
                .build();
    }
}
