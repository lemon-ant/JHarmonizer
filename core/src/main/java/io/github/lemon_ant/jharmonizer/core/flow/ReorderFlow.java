package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.REORDER;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
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
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterSpacingConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.NonNull;

/**
 * Flow that rewrites source files in-place according to the configured ordering and formatting rules.
 * Optionally renames the original file to a backup before overwriting it.
 */
public class ReorderFlow extends AbstractOptOutFlow {

    private final boolean backupsEnabled;

    /**
     * Creates a flow that rewrites sources in place and optionally saves backups before overwriting files.
     *
     * @param formatter the formatter used after sorting
     * @param backupsEnabled whether rewritten files should keep a backup copy
     * @param sorter the sorter used to reorder members
     * @param spacingConfig the printer spacing configuration
     */
    public ReorderFlow(
            @NonNull Formatter formatter,
            boolean backupsEnabled,
            @NonNull Sorter sorter,
            @NonNull PrinterSpacingConfig spacingConfig) {
        super(formatter, sorter, spacingConfig, REORDER);
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
    public FlowProcessingResult processSrc(@NonNull SrcFile srcFile) {
        getDebugStageRecorder().recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult;
        try {
            parsingResult = SrcAstTranslator.parse(srcFile, getSpacingConfig());
        } catch (SpoonModelBuildException exception) {
            return processSrcWithFormattingOnlyFallback(srcFile, exception);
        }
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, "all harmonization");
        }

        SortingSerializationAndFormattingResult sortingSerializationAndFormattingResult =
                sortSerializeAndFormatSrc(srcFile, parsedSpoonAstModel, "sorting");
        SortingAndSerializationResult sortingAndSerializationResult =
                sortingSerializationAndFormattingResult.getSortingAndSerializationResult();
        SpoonAstModel sortedSpoonAstModel = sortingSerializationAndFormattingResult.getSortedSpoonAstModel();

        boolean hasChanges =
                !srcFile.getSrcCode().equals(sortingSerializationAndFormattingResult.getFormattedSrcCode());
        if (hasChanges) {
            if (backupsEnabled) {
                SrcFilesHandler.renameToBackup(srcFile.getPath());
            }
            SrcFilesHandler.overwrite(srcFile.getPath(), sortingSerializationAndFormattingResult.getFormattedSrcCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(
                        sortingAndSerializationResult.getSortingResult().getSortingStatistic())
                .serializationStatistic(sortingAndSerializationResult.getSerializationStatistic())
                .formattingStatistic(sortingSerializationAndFormattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        !sortingAndSerializationResult.isSortingSkipped()
                                && isRelocated(
                                        sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                        sortedSpoonAstModel.getCompilationUnit()),
                        hasChanges,
                        false))
                .build();
    }

    @NonNull
    private FlowProcessingResult processSrcWithFormattingOnlyFallback(
            @NonNull SrcFile srcFile, @NonNull SpoonModelBuildException exception) {
        FormattingResult formattingResult = formatSrcWithoutSorting(srcFile, exception.getMessage());
        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        if (hasChanges) {
            if (backupsEnabled) {
                SrcFilesHandler.renameToBackup(srcFile.getPath());
            }
            SrcFilesHandler.overwrite(srcFile.getPath(), formattingResult.getFormattedSrcCode());
        }
        return FlowProcessingResult.builder()
                .path(srcFile.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(buildSyntheticParsingStatistic(srcFile))
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(formattingResult.getFormattingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, hasChanges, false))
                .build();
    }
}
