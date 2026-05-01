package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus.defineFileProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildFullyOffFileSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildSyntheticParsingStatistic;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.util.List;
import lombok.NonNull;

public class CheckAllFlow extends AbstractOptOutFlow {

    /**
     * Creates a flow that reports all ordering and formatting violations found in one source file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     * @param printerConfig the printer configuration
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public CheckAllFlow(@NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        super(formatter, sorter, printerConfig, CHECK_ALL);
    }

    /**
     * Processes the source.
     * @param srcFile the source file
     * @return the result
     */
    @NonNull
    @Override
    FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
        getDebugStageRecorder().recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult;
        try {
            parsingResult = SrcAstTranslator.parse(srcFile, getPrinterConfig());
        } catch (SpoonModelBuildException exception) {
            return processSrcWithFormattingOnlyFallback(srcFile, exception);
        }
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, "all harmonization checks");
        }

        SortingSerializationAndFormattingResult sortingSerializationAndFormattingResult =
                sortSerializeAndFormatSrc(srcFile, parsedSpoonAstModel, "sorting checks");
        SortingAndSerializationResult sortingAndSerializationResult =
                sortingSerializationAndFormattingResult.getSortingAndSerializationResult();
        SpoonAstModel sortedSpoonAstModel = sortingSerializationAndFormattingResult.getSortedSpoonAstModel();

        boolean hasChanges =
                !srcFile.getSrcCode().equals(sortingSerializationAndFormattingResult.getFormattedSrcCode());
        List<MemberRelocation> memberRelocations = List.of();
        String srcDiff = "";
        if (hasChanges) {
            if (!sortingAndSerializationResult.isSortingSkipped()) {
                memberRelocations = findRelocations(
                        sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit());
            }
            srcDiff = computeDiff(srcFile.getSrcCode(), sortingSerializationAndFormattingResult.getFormattedSrcCode());
        }

        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(memberRelocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                .formattingStatistic(sortingSerializationAndFormattingResult.getFormattingStatistic())
                .fileProcessingStatus(
                        defineFileProcessingStatus(!memberRelocations.isEmpty(), !srcDiff.isEmpty(), true))
                .build();
    }

    @NonNull
    private FileProcessingResult processSrcWithFormattingOnlyFallback(
            @NonNull SrcFile srcFile, @NonNull SpoonModelBuildException exception) {
        FormattingResult formattingResult = formatSrcAfterModelBuildFailure(srcFile, exception.getMessage());
        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        String srcDiff = hasChanges ? computeDiff(srcFile.getSrcCode(), formattingResult.getFormattedSrcCode()) : "";
        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(List.of())
                .diff(srcDiff)
                .parsingStatistic(buildSyntheticParsingStatistic(srcFile))
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .fileProcessingStatus(defineFileProcessingStatus(false, hasChanges, true))
                .build();
    }

    @Override
    public boolean isSuccessful(boolean hasModifications) {
        return !hasModifications;
    }

    @Override
    public boolean isModifyingFlow() {
        return false;
    }
}
