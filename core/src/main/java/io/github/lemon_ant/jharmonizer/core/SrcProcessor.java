package io.github.lemon_ant.jharmonizer.core;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.CheckAllFlow;
import io.github.lemon_ant.jharmonizer.core.flow.CheckFailFastFlow;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.IFlow;
import io.github.lemon_ant.jharmonizer.core.flow.ReorderFlow;
import io.github.lemon_ant.jharmonizer.core.flow.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsPrintService;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Main class for processing source files in JHarmonizer.
 * It orchestrates the flow of processing by utilizing the Components instance.
 * The processSources method is the entry point for processing the sources.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.GuardLogStatement")
public final class SrcProcessor {

    private static final String SINGLE_FILE_LOG_PREFIX = "JHarmonizer";
    private static final int MAX_TOTAL_PATH_LENGTH = 100;
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
     * Processes source files found by the given globs through the specified flow type.
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
        IFlow flow =
                // TODO Move it into the flow factory
                switch (flowType) {
                    case REORDER -> new ReorderFlow(formatter, config.isBackupsEnabled(), sorter, printerConfig);
                    case CHECK_ALL -> new CheckAllFlow(formatter, sorter, printerConfig);
                    case CHECK_FAIL_FAST -> new CheckFailFastFlow(formatter, sorter, printerConfig);
                };

        ProcessingProgressReporter progressReporter = new ProcessingProgressReporter();

        AggregatedProcessingStatistic stats = flow.processStream(
                        SrcFilesHandler.readJavaFiles(baseDir, includeGlobs, excludeGlobs))
                .peek(result -> {
                    if (log.isDebugEnabled()) {
                        log.debug(formatSingleFileLogMessage(
                                result.getPath(),
                                result.getFileProcessingStatus().name()));
                    }
                })
                .peek(result -> progressReporter.recordProcessedFile(result.getFileProcessingStatus()))
                .collect(FlowProcessingStats.statsCollector());

        if (config.isPrintProcessingStatistics()) {
            log.info(ProcessingStatisticsPrintService.render(stats));
        } else {
            logDebugProcessingCompletionSummary(stats, flowType);
            logFilesWithUnexpectedErrors(stats);
        }

        flow.logCompletion(stats);
        boolean success = flow.isSuccessful(stats);
        return SrcProcessingResult.of(stats, success);
    }

    private static void logDebugProcessingCompletionSummary(
            @NonNull AggregatedProcessingStatistic aggregatedStatistic, @NonNull FlowType flowType) {
        String processingStatus =
                aggregatedStatistic.getFilesWithUnexpectedErrors().isEmpty()
                        ? SUMMARY_STATUS_COMPLETED
                        : SUMMARY_STATUS_COMPLETED_WITH_ERRORS;
        log.debug(
                "Processing completed (full statistics report disabled). flowType={}, status={}, processedFiles={}, totalSizeBytes={}, wallClockTimeNanos={}, totalCpuTimeNanos={}, unexpectedErrors={}",
                flowType,
                processingStatus,
                aggregatedStatistic.getFileCount(),
                aggregatedStatistic.getTotalSize(),
                aggregatedStatistic.getWallClockTimeNanos(),
                aggregatedStatistic.getTotalProcessingTimeNanos(),
                aggregatedStatistic.getFilesWithUnexpectedErrors().size());
    }

    private static void logFilesWithUnexpectedErrors(@NonNull AggregatedProcessingStatistic aggregatedStatistic) {
        if (aggregatedStatistic.getFilesWithUnexpectedErrors().isEmpty()) {
            return;
        }
        String failedFilesLog = aggregatedStatistic.getFilesWithUnexpectedErrors().stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> PathDisplayFormatUtil.abbreviatePathForDisplay(path, MAX_TOTAL_PATH_LENGTH))
                .collect(Collectors.joining(", "));
        log.warn("Files encountered unexpected errors and were not processed correctly: {}", failedFilesLog);
    }

    @NonNull
    private static String formatSingleFileLogMessage(Path path, String status) {
        String abbreviatedPath = PathDisplayFormatUtil.abbreviatePathForDisplay(path, MAX_TOTAL_PATH_LENGTH);
        return SINGLE_FILE_LOG_PREFIX + " " + status + " " + abbreviatedPath;
    }

    private void logStartupBanner(
            @NonNull FlowType flowType,
            @NonNull Path baseDir,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        if (config.isPrintProcessingStatistics() && log.isInfoEnabled()) {
            log.info(StartupBannerRenderer.render(
                    flowType, baseDir, config.isBackupsEnabled(), includeGlobs, excludeGlobs));
        } else {
            log.debug(
                    "Starting source processing. flowType={}, baseDir={}, includeGlobs={}, excludeGlobs={}, backupsEnabled={}",
                    flowType,
                    baseDir.toAbsolutePath(),
                    includeGlobs,
                    excludeGlobs,
                    config.isBackupsEnabled());
        }
    }
}
