package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.cli.logging.LoggingConfigurator;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseCommand implements Callable<Integer> {

    @NonNull
    protected final SourceProcessor sourceProcessor;

    @Option(
            names = {"-b", "--base-dir"},
            description = "Base directory containing Java source files.",
            required = true)
    private Path baseDir;

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

    protected BaseCommand() {
        this(new SourceProcessor());
    }

    @Override
    public final Integer call() {
        CommandOptions opts = new CommandOptions(baseDir, Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs), verbose);
        if (opts.isVerbose()) {
            LoggingConfigurator.setDebugLevel();
        }
        return execute(opts);
    }

    protected abstract int execute(CommandOptions opts);
}
