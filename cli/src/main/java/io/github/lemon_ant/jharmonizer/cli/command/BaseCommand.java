package io.github.lemon_ant.jharmonizer.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotFormattedException;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

/**
 * Abstract base for all JHarmonizer CLI commands.
 * Handles common option parsing (base directory, include/exclude globs, verbose flag)
 * and delegates concrete flow execution to sub-classes.
 */
@Slf4j
abstract class BaseCommand implements Callable<Integer> {

    /**
     * Creates a new base CLI command.
     */
    protected BaseCommand() {
        // Protected so only concrete commands in this hierarchy can instantiate the base type.
    }

    @Option(
            names = {"-b", "--base-dir"},
            description = "Base directory containing Java source files (default: current directory).")
    private Path baseDir;

    @Option(
            names = {"-i", "--include"},
            split = ",",
            description = {
                "Glob patterns for files to include.",
                "Repeat this option or pass multiple patterns as a comma-separated list."
            })
    @SuppressWarnings("PMD.ImmutableField")
    private Set<String> includeGlobs = new HashSet<>();

    @Option(
            names = {"-e", "--exclude"},
            split = ",",
            description = {
                "Glob patterns for files to exclude.",
                "Repeat this option or pass multiple patterns as a comma-separated list."
            })
    @SuppressWarnings("PMD.ImmutableField")
    private Set<String> excludeGlobs = new HashSet<>();

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose (DEBUG level) logging.")
    private boolean verbose;

    @Option(
            names = {"-c", "--config"},
            description = "Path to custom YAML configuration file merged over the built-in defaults.")
    @Nullable
    private Path configFilePath;

    /**
     * Returns the processing flow implemented by the command.
     *
     * @return the flow type to execute
     */
    @NonNull
    protected abstract FlowType getFlowType();

    /**
     * Returns the exit code used when a check command detects violations.
     *
     * @return the exit code for check failures
     */
    protected int checkFailedExitCode() {
        return 1;
    }

    /**
     * Parses command-line options and runs the selected processing flow.
     *
     * @return the process exit code
     */
    @Override
    @SuppressWarnings({"PMD.GuardLogStatement", "PMD.AvoidCatchingGenericException"})
    public final Integer call() {
        Path effectiveBaseDir = baseDir != null ? baseDir : Path.of(".");
        Path absoluteBaseDir = toAbsoluteNormalizedPath(effectiveBaseDir);
        if (!Files.isDirectory(absoluteBaseDir)) {
            log.error("Base directory does not exist or is not a directory: {}", absoluteBaseDir);
            return 1;
        }
        Path effectiveConfigFilePath = toAbsoluteNormalizedPath(configFilePath);
        if (effectiveConfigFilePath != null && !Files.exists(effectiveConfigFilePath)) {
            log.error("Config file does not exist: {}", effectiveConfigFilePath);
            return 1;
        }
        CommandOptions commandOptions = new CommandOptions(
                absoluteBaseDir, Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs), verbose, effectiveConfigFilePath);
        if (commandOptions.isVerbose()) {
            ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
        }
        try {
            return processWithFlow(commandOptions);
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private int processWithFlow(CommandOptions commandOptions) {
        FlowType flowType = getFlowType();
        log.info(
                "Processing sources with flow {} in: {} using config: {}",
                flowType,
                commandOptions.getBaseDir(),
                describeConfigSource(commandOptions.getConfigFilePath()));
        try {
            createSourceProcessor(commandOptions.getConfigFilePath())
                    .processSources(
                            commandOptions.getBaseDir(),
                            commandOptions.getIncludeGlobs(),
                            commandOptions.getExcludeGlobs(),
                            flowType);
            return 0;
        } catch (NotFormattedException | NotOrderedException e) {
            log.warn("Flow {} stopped early: {}", flowType, e.getMessage());
            return checkFailedExitCode();
        }
    }

    @NonNull
    private static SourceProcessor createSourceProcessor(@Nullable Path configFilePath) {
        FlexibleUnifiedConfig externalConfig = configFilePath != null
                ? JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configFilePath)
                : null;
        return new SourceProcessor(externalConfig);
    }

    @Nullable
    private static Path toAbsoluteNormalizedPath(@Nullable Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    @NonNull
    private static String describeConfigSource(@Nullable Path configFilePath) {
        return configFilePath != null
                ? configFilePath.toString()
                : "embedded core default config (/default-config.yml)";
    }

    @Value
    private static class CommandOptions {
        @NonNull
        Path baseDir;

        @NonNull
        Set<@NonNull String> includeGlobs;

        @NonNull
        Set<@NonNull String> excludeGlobs;

        boolean verbose;

        @Nullable
        Path configFilePath;
    }
}
