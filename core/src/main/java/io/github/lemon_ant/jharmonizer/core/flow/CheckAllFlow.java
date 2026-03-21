package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

public class CheckAllFlow extends AbstractOptOutFlow {

    /**
     * Creates a flow that reports all ordering and formatting violations found in one source file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public CheckAllFlow(@NonNull Formatter formatter, @NonNull Sorter sorter) {
        super(formatter, sorter, CHECK_ALL);
    }

    /**
     * Processes the source.
     * @param srcFile the source file
     * @return the result
     */
    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull SrcFile srcFile) {
        getDebugStageRecorder().recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parse(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, true, "all harmonization checks");
        }

        SortingSerializationAndFormattingResult sortingSerializationAndFormattingResult =
                sortSerializeAndFormatSource(srcFile, parsedSpoonAstModel, "sorting checks");
        SortingAndSerializationResult sortingAndSerializationResult =
                sortingSerializationAndFormattingResult.getSortingAndSerializationResult();
        SpoonAstModel sortedSpoonAstModel = sortingSerializationAndFormattingResult.getSortedSpoonAstModel();

        boolean hasChanges =
                !srcFile.getSrcCode().equals(sortingSerializationAndFormattingResult.getFormattedSrcCode());
        List<Pair<CtElement, Integer>> elementRelocations = List.of();
        String srcDiff = "";
        if (hasChanges) {
            if (!sortingAndSerializationResult.isSortingSkipped()) {
                elementRelocations = findRelocations(
                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                        sortedSpoonAstModel.getCompilationUnit());
            }
            srcDiff = computeDiff(srcFile.getSrcCode(), sortingSerializationAndFormattingResult.getFormattedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(elementRelocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                .formattingStatistic(sortingSerializationAndFormattingResult.getFormattingStatistic())
                .flowProcessingStatus(
                        defineFlowProcessingStatus(!elementRelocations.isEmpty(), !srcDiff.isEmpty(), true))
                .build();
    }
}
