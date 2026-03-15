package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.DirectiveFlowSupport.buildFileDirectiveSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.DirectiveFlowSupport.createOriginalSourceSerializationResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.RESTRUCTURE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.directive.JHarmonizerDirectiveMode;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
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
        if (parsingResult.getSpoonAstModel().getDirectives().hasFileDirectiveMode(JHarmonizerDirectiveMode.OFF)) {
            log.info("Skipping all harmonization for {} because of @jharmonizer:off", srcFile.getPath());
            return buildFileDirectiveSkippedResult(
                    srcFile.getPath(), srcFile.getSrcCode(), parsingResult, false, null, null);
        }

        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        SortingResult sortingResult;
        SerializationResult serializationResult;
        if (parsedSpoonAstModel.getDirectives().hasFileDirectiveMode(JHarmonizerDirectiveMode.SORT_OFF)) {
            log.info("Skipping sorting for {} because of @jharmonizer:sort-off", srcFile.getPath());
            sortingResult = new SortingResult(parsedSpoonAstModel, new SortingStatistic(0));
            serializationResult = createOriginalSourceSerializationResult(srcFile.getSrcCode());
        } else {
            sortingResult = sorter.sort(parsedSpoonAstModel);
            serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
            debugStageRecorder.recordSrcStage(
                    srcFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());
        }
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();

        FormatingResult formatingResult = formatter.formatSource(
                serializationResult.getSerializedSrcCode(),
                srcFile.getPath(),
                serializationResult.getFormattingExclusionRanges());
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.FORMATTED, formatingResult.getFormatedSrcCode());

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
