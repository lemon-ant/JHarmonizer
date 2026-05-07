// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @RequiredArgsConstructor fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the field-ordering bug.
import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.diff.FormattingViolationPrinter;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.CheckAllFlow;
import io.github.lemon_ant.jharmonizer.core.flow.CheckFailFastFlow;
import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.IFlow;
import io.github.lemon_ant.jharmonizer.core.flow.ReorderFlow;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsPrintService;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.MemberRelocationPrinter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.utilities.JvmShutdownSignal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/*
 * Main class for processing source files in JHarmonizer.
 * It orchestrates the flow of processing by utilizing the Components instance.
 * The processSources method is the entry point for processing the sources.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.ExcessiveImports"})
public final class SrcProcessor {

    private static final String SINGLE_FILE_LOG_PREFIX = "JHarmonizer";
    private static final int MAX_TOTAL_PATH_LENGTH = 100;
    private static final int MAX_BULLET_LIST_PATH_LENGTH = 120;
    private static final String SUMMARY_STATUS_COMPLETED = "COMPLETED";
    private static final String SUMMARY_STATUS_COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS";

    private final CompiledConfig config;
    private final Formatter formatter;
    private final Sorter sorter;
    private final PrinterConfig printerConfig;

    /**
     * Creates a new SrcProcessor.
     */
    public SrcProcessor() {
        this((FlexibleUnifiedConfig) null);
    }

    /**
     * Primary constructor for SrcProcessor embedded into some wrapper.
     *
     * @param externalConfig optional external configuration overlay
     */
    public SrcProcessor(@Nullable FlexibleUnifiedConfig externalConfig) {
        this(ConfigurationManager.overrideDefaultConfig(externalConfig));
    }

    private SrcProcessor(CompiledConfig compiledConfig) {
        this(
                compiledConfig,
                new Formatter(
                        compiledConfig.getFormatting().getFormatterStyle(),
                        compiledConfig.getFormatting().isFixImports()),
                new Sorter(compiledConfig),
                createPrinterConfig(compiledConfig.getFormatting()));
    }

    /**
     * Extracts printer configuration flags from the unified formatting settings.
     *
     * @param formatting the unified formatting settings
     * @return the printer config
     */
    @NonNull
    private static PrinterConfig createPrinterConfig(@NonNull UnifiedFormatting formatting) {
        return new PrinterConfig(
                formatting.isBlankLineAfterTypeHeader(),
                formatting.isBlankLineBeforeComment(),
                formatting.isBlankLineBetweenFields());
    }

    /**
     * Processes source files found under the given base directory by the specified flow type.
     * The flow itself controls stream pipeline decoration (e.g. early termination for
     * fail-fast), success determination, and completion logging.
     *
     * @param baseDir the root directory to scan for source files
     * @param includeGlobs glob patterns for files to include
     * @param excludeGlobs glob patterns for files to exclude
     * @param flowType the processing flow strategy to apply
     * @return the pipeline-level processing result
     */
    @NonNull
    public SrcProcessingResult processSources(
            @NonNull Path baseDir,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs,
            @NonNull FlowType flowType) {
        logStartupBanner(flowType, baseDir, includeGlobs, excludeGlobs);
        IFlow flow = createFlow(flowType);

        ProcessingProgressReporter progressReporter = new ProcessingProgressReporter();

        AggregatedProcessingStatistic aggregatedProcessingStatistic = flow.processStream(
                        SrcFilesHandler.readJavaFiles(baseDir, includeGlobs, excludeGlobs))
                .peek(fileProcessingResult -> {
                    if (log.isDebugEnabled()) {
                        log.debug(formatSingleFileLogMessage(
                                fileProcessingResult.getPath(),
                                fileProcessingResult.getFileProcessingStatus().name()));
                    }
                })
                .peek(fileProcessingResult ->
                        progressReporter.recordProcessedFile(fileProcessingResult.getFileProcessingStatus()))
                .peek(SrcProcessor::logNonConformingFileDetails)
                .collect(FlowProcessingStats.statsCollector());

        switch (config.getProcessingStatisticsMode()) {
            case FULL -> log.info(ProcessingStatisticsPrintService.render(aggregatedProcessingStatistic));
            case MINIMAL -> {
                log.info(ProcessingStatisticsPrintService.renderMinimal(aggregatedProcessingStatistic));
                logFilesWithUnexpectedErrors(aggregatedProcessingStatistic);
            }
            case DISABLED -> {
                logDebugProcessingCompletionSummary(aggregatedProcessingStatistic, flowType);
                logFilesWithUnexpectedErrors(aggregatedProcessingStatistic);
            }
            default ->
                throw new IllegalStateException(
                        "Unhandled ProcessingStatisticsMode: " + config.getProcessingStatisticsMode());
        }

        logCompletionMessage(flowType, aggregatedProcessingStatistic, flow.isModifyingFlow());
        long nonConformingFileCount = aggregatedProcessingStatistic.computeNonConformingFileCount();
        boolean success = flow.isSuccessful(nonConformingFileCount > 0);
        return new SrcProcessingResult(aggregatedProcessingStatistic, success);
    }

    /**
     * Creates the processing flow for the requested processing strategy.
     *
     * @param flowType the flow strategy to instantiate
     * @return the matching processing flow
     */
    @NonNull
    private IFlow createFlow(FlowType flowType) {
        return switch (flowType) {
            case REORDER -> new ReorderFlow(formatter, config.isBackupsEnabled(), sorter, printerConfig);
            case CHECK_ALL -> new CheckAllFlow(formatter, sorter, printerConfig);
            case CHECK_FAIL_FAST -> new CheckFailFastFlow(formatter, sorter, printerConfig);
        };
    }

    private static void logDebugProcessingCompletionSummary(
            @NonNull AggregatedProcessingStatistic aggregatedProcessingStatistic, @NonNull FlowType flowType) {
        String processingStatus =
                aggregatedProcessingStatistic.getFilesWithUnexpectedErrors().isEmpty()
                        ? SUMMARY_STATUS_COMPLETED
                        : SUMMARY_STATUS_COMPLETED_WITH_ERRORS;
        log.debug(
                "Processing completed (statistics disabled). flowType={}, status={}, processedFiles={}, totalSizeBytes={}, wallClockTimeNanos={}, totalCpuTimeNanos={}, unexpectedErrors={}",
                flowType,
                processingStatus,
                aggregatedProcessingStatistic.getFileCount(),
                aggregatedProcessingStatistic.getTotalSizeInBytes(),
                aggregatedProcessingStatistic.getWallClockTimeNanos(),
                aggregatedProcessingStatistic.getTotalProcessingTimeNanos(),
                aggregatedProcessingStatistic.getFilesWithUnexpectedErrors().size());
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private static void logCompletionMessage(
            @NonNull FlowType flowType,
            @NonNull AggregatedProcessingStatistic aggregatedProcessingStatistic,
            boolean isModifyingFlow) {
        long modifiedFileCount = aggregatedProcessingStatistic.computeNonConformingFileCount();
        List<Path> stopTriggerPaths = aggregatedProcessingStatistic.getStopTriggerPaths();
        if (JvmShutdownSignal.isShuttingDown()) {
            log.info(
                    "{} interrupted (Ctrl+C). Processed {} file(s).",
                    flowType,
                    aggregatedProcessingStatistic.getFileCount());
        } else if (!stopTriggerPaths.isEmpty()) {
            log.warn(
                    "{} stopped early. Processed {} file(s), {} non-conforming.{}",
                    flowType,
                    aggregatedProcessingStatistic.getFileCount(),
                    modifiedFileCount,
                    formatBulletList("Stop triggered by", stopTriggerPaths));
        } else {
            String countLabel = isModifyingFlow ? "modified" : "non-conforming";
            log.info(
                    "{} completed. Processed {} file(s), {} {}.",
                    flowType,
                    aggregatedProcessingStatistic.getFileCount(),
                    modifiedFileCount,
                    countLabel);
        }
    }

    @NonNull
    private static String formatBulletList(@NonNull String header, @NonNull List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n  ").append(header).append(':');
        paths.stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> PathDisplayFormatUtil.abbreviatePathForDisplay(path, MAX_BULLET_LIST_PATH_LENGTH))
                .forEach(abbreviatedPath -> builder.append("\n    - ").append(abbreviatedPath));
        return builder.toString();
    }

    private static void logFilesWithUnexpectedErrors(
            @NonNull AggregatedProcessingStatistic aggregatedProcessingStatistic) {
        if (aggregatedProcessingStatistic.getFilesWithUnexpectedErrors().isEmpty()) {
            return;
        }
        log.warn(
                "Files encountered unexpected errors and were not processed correctly:{}",
                formatBulletList("Affected files", aggregatedProcessingStatistic.getFilesWithUnexpectedErrors()));
    }

    @NonNull
    private static String formatSingleFileLogMessage(Path path, String status) {
        String abbreviatedPath = PathDisplayFormatUtil.abbreviatePathForDisplay(path, MAX_TOTAL_PATH_LENGTH);
        return SINGLE_FILE_LOG_PREFIX + " " + status + " " + abbreviatedPath;
    }

    private static void logNonConformingFileDetails(@NonNull FileProcessingResult fileProcessingResult) {
        if (fileProcessingResult.getMemberRelocations() != null
                && !fileProcessingResult.getMemberRelocations().isEmpty()) {
            log.error(MemberRelocationPrinter.printRelocations(
                    fileProcessingResult.getPath(), fileProcessingResult.getMemberRelocations()));
        }
        String diff = fileProcessingResult.getDiff();
        if (diff != null && !diff.isEmpty()) {
            log.error(FormattingViolationPrinter.printFormattingViolation(fileProcessingResult.getPath(), diff));
        }
    }

    private void logStartupBanner(
            @NonNull FlowType flowType,
            @NonNull Path baseDir,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        if (config.getProcessingStatisticsMode() == ProcessingStatisticsMode.FULL && log.isInfoEnabled()) {
            log.info(StartupBannerRenderer.render(
                    flowType, baseDir, config.isBackupsEnabled(), includeGlobs, excludeGlobs));
        } else {
            log.debug(
                    "Starting source processing. flowType={}, baseDir={}, includeGlobs={}, excludeGlobs={}, backupsEnabled={}",
                    flowType,
                    baseDir,
                    includeGlobs,
                    excludeGlobs,
                    config.isBackupsEnabled());
        }
    }
}
