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
    private File baseDir;

    @Option(
            names = {"-i", "--include"},
            description = "Glob patterns for files to include (can be repeated).")
    private final Set<String> includeGlobs = new HashSet<>();

    @Option(
            names = {"-e", "--exclude"},
            description = "Glob patterns for files to exclude (can be repeated).")
    private final Set<String> excludeGlobs = new HashSet<>();

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose (DEBUG level) logging.")
    private boolean verbose;

    protected BaseCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    protected File getBaseDir() {
        return baseDir;
    }

    protected Set<String> getIncludeGlobs() {
        return includeGlobs;
    }

    protected Set<String> getExcludeGlobs() {
        return excludeGlobs;
    }

    protected boolean isVerbose() {
        return verbose;
    }

    @Override
    public final Integer call() {
        if (isVerbose()) {
            LoggingConfigurator.setDebugLevel();
        }
        return execute();
    }

    protected abstract int execute();
}
