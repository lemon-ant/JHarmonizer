package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Flow that rewrites source files in-place according to the configured ordering and formatting rules.
 * Optionally renames the original file to a backup before overwriting it.
 */
@Slf4j
@AllArgsConstructor
public class RestructureFlow implements IFlow {

    private final Formatter formatter;
    private final boolean backupsEnabled;
    private final Sorter sorter;
    private final FlowDebugStageRecorder debugStageRecorder = new FlowDebugStageRecorder(RESTRUCTURE);

    /**
     * Processes the source.
     * @param srcFile the source file
     * @return the result
     */
    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile) {
        debugStageRecorder.recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());

        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);

        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());

        FormattingResult formattingResult =
                formatter.formatSource(serializationResult.getSerializedSrcCode(), srcFile.getPath());
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.FORMATTED, formattingResult.getFormattedSrcCode());

        boolean hasChanged = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        if (hasChanged) {
            if (backupsEnabled) {
                SourceFilesHandler.renameToBackup(srcFile.getPath());
            }
            SourceFilesHandler.overwrite(srcFile.getPath(), formattingResult.getFormattedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        isRelocated(
                                sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                sortedSpoonAstModel.getCompilationUnit()),
                        hasChanged,
                        false))
                .build();
    }
}
