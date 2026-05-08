// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowResultUtils.buildFullyOffFileSkippedResult;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowType.CHECK_FAIL_FAST;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Flow that signals pipeline stop when the first ordering or formatting violation is detected.
 * Instead of throwing exceptions, returns a {@link FileProcessingResult} with
 * {@code stopRequested = true} so the pipeline can gracefully shut down
 * while preserving all accumulated statistics.
 *
 * <p>Overrides {@link #preCheckSrcFiles} to skip remaining files as soon as a violation
 * has been detected (before the expensive mapping step), and overrides
 * {@link #postProcessResults} to propagate the stop flag via {@code peek} after
 * each result passes through.
 */
public class CheckFailFastFlow extends AbstractOptOutFlow {
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    /**
     * Creates a flow that signals stop at the first ordering or formatting violation in a source file.
     *
     * @param formatter the formatter used after sorting
     * @param sorter the sorter used to reorder members
     * @param printerConfig the printer configuration
     */
    public CheckFailFastFlow(
            @NonNull Formatter formatter, @NonNull Sorter sorter, @NonNull PrinterConfig printerConfig) {
        super(formatter, sorter, printerConfig, CHECK_FAIL_FAST);
    }

    @Override
    public boolean isModifyingFlow() {
        return false;
    }

    @Override
    public boolean isSuccessful(boolean hasModifications) {
        return !hasModifications;
    }

    @Override
    protected boolean isStopRequestedOnFormattingChange() {
        return true;
    }

    /**
     * Sets the stop flag after each result that requests stop passes through,
     * so subsequent files are skipped by the pre-check phase.
     *
     * @param results the stream of per-file results from the mapping phase
     * @return the same stream, with stop-flag propagation via {@code peek}
     */
    @Override
    @NonNull
    protected Stream<FileProcessingResult> postProcessResults(@NonNull Stream<FileProcessingResult> results) {
        return results.peek(fileProcessingResult -> {
            if (fileProcessingResult.isStopRequested()) {
                stopFlag.set(true);
            }
        });
    }

    /**
     * Extends the base JVM-shutdown pre-check with an early stop-flag guard,
     * so that no further source files are processed once a violation has been detected.
     * The stop-flag is set by {@link #postProcessResults} after the first violating result passes through.
     *
     * @param srcFiles the incoming stream of source files
     * @return a stream that skips remaining files once the stop flag is set
     */
    @Override
    @NonNull
    protected Stream<SrcFile> preCheckSrcFiles(@NonNull Stream<SrcFile> srcFiles) {
        return super.preCheckSrcFiles(srcFiles).takeWhile(srcFile -> !stopFlag.get());
    }

    /**
     * Processes the source file and returns a result that may signal pipeline stop.
     *
     * @param srcFile the source file
     * @return the result, with {@code stopRequested = true} if a violation was detected
     */
    @Override
    @NonNull
    FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
        getDebugStageRecorder().recordSrcStage(srcFile.getPath(), SrcFlowStage.ORIGINAL, srcFile.getSrcCode());
        ParsingResult parsingResult;
        try {
            parsingResult = SrcAstTranslator.parse(srcFile, getPrinterConfig());
        } catch (SpoonModelBuildException modelBuildException) {
            return processSrcWithFormattingOnlyFallback(srcFile, modelBuildException.getMessage());
        }
        SpoonAstModel parsedSpoonAstModel = parsingResult.getSpoonAstModel();
        if (parsedSpoonAstModel.getOptOuts().hasFileOptOutMode(JHarmonizerOptOutMode.FULLY_OFF)) {
            return buildFullyOffFileSkippedResult(srcFile, parsingResult, "all harmonization checks");
        }
        return checkSortThenFormat(srcFile, parsedSpoonAstModel, parsingResult, true);
    }
}
