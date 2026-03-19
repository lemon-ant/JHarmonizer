package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
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
        getDebugStageRecorder()
                .recordSrcStage(srcFile.getPath(), FlowDebugStageRecorder.SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult = SourceAstTranslator.parse(srcFile);
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFileOptOutSkippedResult(
                    srcFile, parsingResult, true, List.of(), "", "all harmonization checks");
        }

        SortingSerializationAndFormattingResult sortingSerializationAndFormattingResult =
                sortAndFormatSource(srcFile, parsedSpoonAstModel, "sorting checks");
        SortingAndSerializationResult sortingAndSerializationResult =
                sortingSerializationAndFormattingResult.getSortingAndSerializationResult();
        FormattingResult formattingResult = sortingSerializationAndFormattingResult.getFormattingResult();
        SpoonAstModel sortedSpoonAstModel = sortingSerializationAndFormattingResult.getSortedSpoonAstModel();

        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        List<Pair<CtElement, Integer>> elementRelocations;
        String srcDiff;
        if (hasChanges && !sortingAndSerializationResult.isSortingSkipped()) {
            elementRelocations = findRelocations(
                    sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());
            srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode());
        } else if (hasChanges) {
            elementRelocations = List.of();
            srcDiff = computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode());
        } else {
            elementRelocations = List.of();
            srcDiff = "";
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(elementRelocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(
                        sortingAndSerializationResult.getSerializationResult().getSerializationStatistic())
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(
                        defineFlowProcessingStatus(!elementRelocations.isEmpty(), !srcDiff.isEmpty(), true))
                .build();
    }
}
