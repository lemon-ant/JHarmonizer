// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildFullyOffFileSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_ALL;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.flow.FlowDebugStageRecorder.SrcFlowStage;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import lombok.NonNull;

public class CheckAllFlow extends AbstractOptOutFlow {

    /**
     * Creates a flow that reports all ordering and formatting violations found in one source file.
     * Sorting is checked first; if violations are detected, formatting is skipped for that file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     * @param printerConfig the printer configuration
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public CheckAllFlow(@NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        super(formatter, sorter, printerConfig, CHECK_ALL);
    }

    @Override
    public boolean isModifyingFlow() {
        return false;
    }

    @Override
    public boolean isSuccessful(boolean hasModifications) {
        return !hasModifications;
    }

    /**
     * Processes the source file by checking sorting first.
     * If sorting violations are detected, only the sorting report is produced and formatting is skipped,
     * because formatting on an incorrectly sorted file would produce meaningless results.
     * Processing always continues to the next file regardless of violations.
     *
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
            return processSrcWithFormattingOnlyFallback(srcFile, exception.getMessage());
        }
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, "all harmonization checks");
        }
        return checkSortThenFormat(srcFile, parsedSpoonAstModel, parsingResult, false);
    }
}
