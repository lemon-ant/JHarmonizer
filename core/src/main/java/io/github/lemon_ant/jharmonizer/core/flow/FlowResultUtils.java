/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus.defineFileProcessingStatus;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutMode;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Static helpers for building standard {@link FileProcessingResult} instances and emitting opt-out skip logs.
 *
 * <p>Extracted from {@link AbstractOptOutFlow} to keep the abstract base class focused on flow orchestration.
 * This class is package-private and intended only for use by flow implementations in this package.
 */
@Slf4j
@UtilityClass
class FlowResultUtils {

    /**
     * Builds a synthetic {@link ParsingStatistic} for files that were never fully parsed
     * (e.g., because Spoon model creation failed and only formatting was attempted).
     *
     * @param srcFile the source file whose code length feeds the statistic
     * @return a synthetic statistic with zero parsed-type counts
     */
    @NonNull
    static ParsingStatistic buildSyntheticParsingStatistic(@NonNull SrcFile srcFile) {
        String srcCode = srcFile.getSrcCode();
        return new ParsingStatistic(srcCode.length(), srcCode.getBytes(StandardCharsets.UTF_8).length, 0, 0, 0, 0);
    }

    /**
     * Builds a {@link FileProcessingResult} for the formatting-only fallback path,
     * used when Spoon model creation fails and only formatting can be applied.
     *
     * @param srcFile the source file being processed
     * @param formattingResult the result of the formatting-only pass
     * @param stopOnChanges whether to set the stop-requested flag if formatting changes are detected
     * @return the fallback processing result
     */
    @NonNull
    static FileProcessingResult buildFormattingOnlyFallbackResult(
            @NonNull SrcFile srcFile, @NonNull FormattingResult formattingResult, boolean stopOnChanges) {
        boolean hasChanges = !srcFile.getSrcCode().equals(formattingResult.getFormattedSrcCode());
        String srcDiff = hasChanges
                ? computeDiff(
                        srcFile.getPath().toString(), srcFile.getSrcCode(), formattingResult.getFormattedSrcCode())
                : "";
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
                .stopRequested(hasChanges && stopOnChanges)
                .build();
    }

    /**
     * Builds a {@link FileProcessingResult} for a file that was skipped because of a
     * {@link JHarmonizerOptOutMode#FULLY_OFF} annotation.
     *
     * @param srcFile the source file that was skipped
     * @param parsingResult the parsing result obtained before the opt-out was detected
     * @param skippedOperationDescription human-readable description of the skipped operation for debug logging
     * @return the skipped-file processing result
     */
    @NonNull
    static FileProcessingResult buildFullyOffFileSkippedResult(
            @NonNull SrcFile srcFile,
            @NonNull ParsingResult parsingResult,
            @NonNull String skippedOperationDescription) {
        logFileOptOutSkip(srcFile, skippedOperationDescription, JHarmonizerOptOutMode.FULLY_OFF);
        return FileProcessingResult.builder()
                .path(srcFile.getPath())
                .memberRelocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(
                        new SerializationStatistic(srcFile.getSrcCode().length(), 0))
                .formattingStatistic(
                        new FormattingStatistic(srcFile.getSrcCode().length(), 0))
                .fileProcessingStatus(FileProcessingStatus.SKIPPED)
                .build();
    }

    /**
     * Emits a DEBUG-level log message indicating that an operation was skipped for a file because of an opt-out mode.
     *
     * @param srcFile the source file whose operation was skipped
     * @param skippedOperationDescription human-readable description of the skipped operation
     * @param optOutMode the opt-out mode that caused the skip
     */
    static void logFileOptOutSkip(
            @NonNull SrcFile srcFile,
            @NonNull String skippedOperationDescription,
            @NonNull JHarmonizerOptOutMode optOutMode) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Skipping {} for {} because of {} ({})",
                    skippedOperationDescription,
                    srcFile.getPath(),
                    optOutMode.getDisplayName(),
                    optOutMode.getToken());
        }
    }
}
