package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.cli.logging.LoggingConfigurator;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Option;

public abstract class BaseCommand implements Callable<Integer> {

    protected final SourceProcessor sourceProcessor;

    @Option(
            names = {"-b", "--base-dir"},
            description = "Base directory containing Java source files.",
            required = true)
    protected File baseDir;

    @Option(
            names = {"-i", "--include"},
            description = "Glob patterns for files to include (can be repeated).")
    protected Set<String> includeGlobs = new HashSet<>();

    @Option(
            names = {"-e", "--exclude"},
            description = "Glob patterns for files to exclude (can be repeated).")
    protected Set<String> excludeGlobs = new HashSet<>();

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose (DEBUG level) logging.")
    protected boolean verbose;

    protected BaseCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    public final Integer call() {
        if (verbose) {
            LoggingConfigurator.setDebugLevel();
        }
        return execute();
    }

    protected abstract Integer execute();
}
