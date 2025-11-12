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
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import java.nio.file.Path;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Main class for processing source files in JReStructor.
 * It orchestrates the flow of processing by utilizing the Components instance.
 * The processSources method is the entry point for processing the sources.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SourceProcessor {

    private final CompiledConfig config;
    private final Formatter formatter;
    // TODO This is bull shit
    private ProcessingResultCollector processingResultCollector;
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
                //  new ProcessingResultCollector(),
                new Sorter(compiledConfig));
    }

    /**
     * Processes a specific list of source file paths.
     * It processes each source file in parallel and collects the results.
     * The outcome of the processing is logged at the info level.
     *
     * @param paths List of paths to source files to be processed.
     */
    public ProcessingResultCollector processSources(
            Path baseDir, Collection<String> includeGlobs, Collection<String> excludeGlobs, FlowType flowType) {
        IFlow flow =
                // TODO Move it into the flow factory
                switch (flowType) {
                    case RESTRUCTURE ->
                        new RestructureFlow(formatter, /* TODO Should be taken from the config*/ true, sorter);
                    case CHECK_ALL -> new CheckAllFlow(formatter, sorter);
                    case CHECK_FAIL_FAST -> new CheckFailFastFlow(formatter, sorter);
                };

        processingResultCollector = SourceFilesHandler.findJavaFiles(baseDir, includeGlobs, excludeGlobs)
                // TODO Possibly include into the one method inside SourceFilesHandler
                .map(SourceFilesHandler::readFile)
                .map(flow::processSource)
                .reduce(
                        // TODO Some shit to understand and redesign
                        processingResultCollector,
                        ProcessingResultCollector::collectResult,
                        (currentResult, newResult) -> newResult);

        processingResultCollector.printAggregatedStatistics();

        return processingResultCollector;
    }
}
