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
import io.github.lemon_ant.jharmonizer.core.flow.SafeFlow;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsPrintService;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
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

    @NonNull
    private static PrinterConfig createPrinterConfig(@NonNull UnifiedFormatting formatting) {
        return new PrinterConfig(
                formatting.isBlankLineAfterTypeHeader(),
                formatting.isBlankLineBeforeAnnotation(),
                formatting.isBlankLineBeforeComment());
    }

    /**
     * Processes a specific list of source file paths.
     * It processes each source file in parallel and collects the results.
     * The outcome of the processing is logged at the info level.
     *
     * @param paths List of paths to source files to be processed.
     */
    @NonNull
    public AggregatedProcessingStatistic processSources(
            @NonNull Path baseDir,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs,
            @NonNull FlowType flowType) {
        log.info(
                "Starting source processing. flowType={}, baseDir={}, includeGlobs={}, excludeGlobs={}, backupsEnabled={}",
                flowType,
                baseDir.toAbsolutePath(),
                includeGlobs,
                excludeGlobs,
                config.isBackupsEnabled());

        IFlow baseFlow =
                // TODO Move it into the flow factory
                switch (flowType) {
                    case REORDER -> new ReorderFlow(formatter, config.isBackupsEnabled(), sorter, printerConfig);
                    case CHECK_ALL -> new CheckAllFlow(formatter, sorter, printerConfig);
                    case CHECK_FAIL_FAST -> new CheckFailFastFlow(formatter, sorter, printerConfig);
                };
        IFlow flow = SafeFlow.wrap(baseFlow);

        ProcessingProgressReporter progressReporter = new ProcessingProgressReporter();
        AggregatedProcessingStatistic aggregatedProcessingStatistic = SrcFilesHandler.readJavaFiles(
                        baseDir, includeGlobs, excludeGlobs)
                .map(flow::processSrc)
                .peek(flowProcessingResult -> {
                    if (log.isDebugEnabled()) {
                        log.debug(formatSingleFileLogMessage(
                                flowProcessingResult.getPath(),
                                flowProcessingResult.getFlowProcessingStatus().name()));
                    }
                    progressReporter.recordProcessedFile(flowProcessingResult.getFlowProcessingStatus());
                })
                .collect(SrcProcessingStats.statsCollector());

        if (config.isPrintProcessingStatistics()) {
            log.info(ProcessingStatisticsPrintService.render(aggregatedProcessingStatistic));
        } else {
            logDebugProcessingCompletionSummary(aggregatedProcessingStatistic, flowType);
            logFilesWithUnexpectedErrors(aggregatedProcessingStatistic);
        }
        return aggregatedProcessingStatistic;
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
}
