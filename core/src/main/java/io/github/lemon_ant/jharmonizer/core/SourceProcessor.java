package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.flow.CheckAllFlow;
import io.github.lemon_ant.jharmonizer.core.flow.CheckFailFastFlow;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.IFlow;
import io.github.lemon_ant.jharmonizer.core.flow.RestructureFlow;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FileProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import java.nio.file.Path;
import java.util.Collection;
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
public final class SourceProcessor {

    private static final String SINGLE_FILE_LOG_PREFIX = "JHarmonizer";
    private static final int MAX_TOTAL_PATH_LENGTH = 100;

    private final CompiledConfig config;
    private final Formatter formatter;
    private final Sorter sorter;

    public SourceProcessor() {
        this((FlexibleUnifiedConfig) null);
    }

    /**
     * Primary constructor for SourceProcessor embedded into some wrapper.
     */
    public SourceProcessor(FlexibleUnifiedConfig externalConfig) {
        this(ConfigurationManager.overrideDefaultConfig(externalConfig));
    }

    private SourceProcessor(CompiledConfig compiledConfig) {
        this(
                compiledConfig,
                new Formatter(
                        compiledConfig.getFormatting().getFormatterStyle(),
                        compiledConfig.getFormatting().isFixImports()),
                new Sorter(compiledConfig));
    }

    /**
     * Processes a specific list of source file paths.
     * It processes each source file in parallel and collects the results.
     * The outcome of the processing is logged at the info level.
     *
     * @param paths List of paths to source files to be processed.
     */
    public AggregatedProcessingStatistic processSources(
            Path baseDir, Collection<String> includeGlobs, Collection<String> excludeGlobs, FlowType flowType) {
        log.info(
                "Starting source processing. flowType={}, baseDir={}, includeGlobs={}, excludeGlobs={}, backupsEnabled={}",
                flowType,
                baseDir.toAbsolutePath(),
                includeGlobs,
                excludeGlobs,
                config.isBackupsEnabled());

        IFlow flow =
                // TODO Move it into the flow factory
                switch (flowType) {
                    case RESTRUCTURE -> new RestructureFlow(formatter, config.isBackupsEnabled(), sorter);
                    case CHECK_ALL -> new CheckAllFlow(formatter, sorter);
                    case CHECK_FAIL_FAST -> new CheckFailFastFlow(formatter, sorter);
                };

        AggregatedProcessingStatistic aggregatedProcessingStatistic = SourceFilesHandler.findJavaFiles(
                        baseDir, includeGlobs, excludeGlobs)
                // TODO Possibly include into the one method inside SourceFilesHandler
                .map(SourceFilesHandler::readFile)
                .map(flow::processSource)
                .peek(flowProcessingResult -> log.info(formatSingleFileLogMessage(
                        flowProcessingResult.getPath(),
                        flowProcessingResult.getFlowProcessingStatus().name())))
                .map(FileProcessingStatistic::convert)
                .collect(SourceProcessingStats.statsCollector());

        log.info(aggregatedProcessingStatistic.toString());
        return aggregatedProcessingStatistic;
    }

    @NonNull
    private static String formatSingleFileLogMessage(Path path, String status) {
        String abbreviatedPath = PathDisplayFormatUtil.abbreviatePathForDisplay(path, MAX_TOTAL_PATH_LENGTH);
        return SINGLE_FILE_LOG_PREFIX + " " + status + " " + abbreviatedPath;
    }
}
