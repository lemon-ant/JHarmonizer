package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;
import static io.github.lemon_ant.jharmonizer.core.flow.OptOutFlowSupport.buildFileOptOutSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.OptOutFlowSupport.createOriginalSourceSerializationResult;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@Slf4j
@AllArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class CheckAllFlow implements IFlow {

    private final Formatter formatter;
    private final Sorter sorter;
    private final FlowDebugStageRecorder debugStageRecorder = new FlowDebugStageRecorder(CHECK_ALL);

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SrcFile srcFile) {
        debugStageRecorder.recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());

        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.OFF)) {
            log.info("Skipping all harmonization checks for {} because of @jharmonizer:off", srcFile.getPath());
            return buildFileOptOutSkippedResult(
                    srcFile.getPath(), srcFile.getSrcCode(), parsingResult, true, List.of(), "");
        }

        SortingResult sortingResult;
        SerializationResult serializationResult;
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.SORT_OFF)) {
            log.info("Skipping sorting checks for {} because of @jharmonizer:sort-off", srcFile.getPath());
            sortingResult = new SortingResult(parsedSpoonAstModel, new SortingStatistic(0));
            serializationResult = createOriginalSourceSerializationResult(srcFile.getSrcCode());
        } else {
            sortingResult = sorter.sort(parsedSpoonAstModel);
            serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        }
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());

        FormatingResult formattingResult = formatter.formatSource(
                serializationResult.getSerializedSrcCode(),
                srcFile.getPath(),
                serializationResult.getFormattingExclusionRanges());
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.FORMATTED, formattingResult.getFormatedSrcCode());

        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormatedSrcCode());
        List<Pair<CtElement, Integer>> elementRelocations;
        String srcDiff;
        if (hasChanges) {
            elementRelocations = findRelocations(
                    sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());
            srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormatedSrcCode());
        } else {
            elementRelocations = List.of();
            srcDiff = "";
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(elementRelocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formattingResult.getFormatingStatistic())
                .flowProcessingStatus(
                        defineFlowProcessingStatus(!elementRelocations.isEmpty(), !srcDiff.isEmpty(), true))
                .build();
    }
}
