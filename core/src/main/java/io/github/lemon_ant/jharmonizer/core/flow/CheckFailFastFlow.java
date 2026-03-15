package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.DirectiveFlowSupport.buildFileDirectiveSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.DirectiveFlowSupport.createOriginalSourceSerializationResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@Slf4j
@AllArgsConstructor
public class CheckFailFastFlow implements IFlow {

    private final Formatter formatter;
    private final Sorter sorter;
    private final FlowDebugStageRecorder debugStageRecorder = new FlowDebugStageRecorder(FlowType.CHECK_FAIL_FAST);

    @Override
    public @NonNull FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile) {
        debugStageRecorder.recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());

        // Parse
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getDirectives().hasFileDirectiveMode(JHarmonizerDirectiveMode.OFF)) {
            log.info("Skipping all harmonization checks for {} because of @jharmonizer:off", srcFile.getPath());
            return buildFileDirectiveSkippedResult(
                    srcFile.getPath(), srcFile.getSrcCode(), parsingResult, true, null, "");
        }

        // Sort (Fail Fast)
        SortingResult sortingResult;
        SerializationResult serializationResult;
        if (parsedSpoonAstModel.getDirectives().hasFileDirectiveMode(JHarmonizerDirectiveMode.SORT_OFF)) {
            log.info("Skipping sorting checks for {} because of @jharmonizer:sort-off", srcFile.getPath());
            sortingResult = new SortingResult(parsedSpoonAstModel, new SortingStatistic(0));
            serializationResult = createOriginalSourceSerializationResult(srcFile.getSrcCode());
        } else {
            sortingResult = sorter.sort(parsedSpoonAstModel);
            serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        }
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        List<Pair<CtElement, Integer>> elementRelocations = findRelocations(
                sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());

        // Serialize
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());

        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(srcFile.getPath(), elementRelocations);
        }

        // Format (Fail Fast)
        FormatingResult formatingResult = formatter.formatSource(
                serializationResult.getSerializedSrcCode(),
                srcFile.getPath(),
                serializationResult.getFormattingExclusionRanges());
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.FORMATTED, formatingResult.getFormatedSrcCode());

        if (!srcFile.getSrcCode().equals(formatingResult.getFormatedSrcCode())) {
            String srcDiff = computeDiff(srcFile.getSrcCode(), formatingResult.getFormatedSrcCode());
            throw new NotFormattedException(srcFile.getPath(), srcDiff);
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff("")
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formatingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }
}
