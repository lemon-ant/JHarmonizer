package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

/**
 * Flow that stops immediately when the first ordering or formatting violation is detected.
 * Throws {@link NotOrderedException} if member order is wrong, or
 * {@link NotFormattedException} if the formatted output differs from the original.
 */
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

        // Sort (Fail Fast)
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        List<Pair<CtElement, Integer>> elementRelocations = findRelocations(
                sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());

        // Serialize
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortedSpoonAstModel);
        debugStageRecorder.recordSrcStage(
                srcFile.getPath(), SrcFlowStage.SORTED, serializationResult.getSerializedSrcCode());

        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(srcFile.getPath(), elementRelocations);
        }

        // Format (Fail Fast)
        FormatingResult formatingResult =
                formatter.formatSource(serializationResult.getSerializedSrcCode(), srcFile.getPath());
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
