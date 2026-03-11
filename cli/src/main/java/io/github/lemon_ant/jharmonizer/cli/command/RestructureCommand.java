package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.cli.config.RestructureCommandConfiguration;
import io.github.lemon_ant.jharmonizer.cli.logging.LoggingConfigurator;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
public class RestructureCommand implements Callable<Integer> {

    private final SourceProcessor sourceProcessor;

    @Option(
            names = "--base-dir",
            description = "Base directory containing Java source files to restructure.",
            required = true)
    private File baseDir;

    @SuppressWarnings("PMD.ImmutableField")
    @Option(names = "--include", description = "Glob patterns for files to include (can be repeated).")
    private Set<String> includeGlobs = new HashSet<>();

    @SuppressWarnings("PMD.ImmutableField")
    @Option(names = "--exclude", description = "Glob patterns for files to exclude (can be repeated).")
    private Set<String> excludeGlobs = new HashSet<>();

    @Option(names = "--dry-run", description = "Check files without applying changes.")
    private boolean dryRun;

    @Option(names = "--verbose", description = "Enable verbose (DEBUG level) logging.")
    private boolean verbose;

    public RestructureCommand() {
        this(new SourceProcessor());
    }

    RestructureCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Integer call() {
        if (verbose) {
            LoggingConfigurator.setDebugLevel();
        }

        RestructureCommandConfiguration config = new RestructureCommandConfiguration(
                baseDir.toPath(), Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs), dryRun, verbose);

        FlowType flowType = config.isDryRun() ? FlowType.CHECK_ALL : FlowType.RESTRUCTURE;

        log.info("Processing sources in: {}", config.getBaseDir());

        try {
            sourceProcessor.processSources(
                    config.getBaseDir(), config.getIncludeGlobs(), config.getExcludeGlobs(), flowType);
            return 0;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}
