package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RestructureFlow implements IFlow {

    private final Formatter formatter;
    private final boolean backupsEnabled;
    private final Sorter sorter;
    private final FlowDebugStageRecorder debugStageRecorder = new FlowDebugStageRecorder(RESTRUCTURE);

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile) {
        debugStageRecorder.recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());

        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        FormatingResult formatingResult =
                formatter.formatSource(serializationResult.getSerializedSrcCode(), srcFile.getPath());

        boolean hasChanged = !srcFile.getSrcCode().equals(formatingResult.getFormatedSrcCode());
        if (hasChanged) {
            if (backupsEnabled) {
                SourceFilesHandler.renameToBackup(srcFile.getPath());
            }
            SourceFilesHandler.overwrite(srcFile.getPath(), formatingResult.getFormatedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formatingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        isRelocated(
                                sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                sortedSpoonAstModel.getCompilationUnit()),
                        hasChanged,
                        false))
                .build();
    }
}
