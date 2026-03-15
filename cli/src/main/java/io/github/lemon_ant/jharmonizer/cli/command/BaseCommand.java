package io.github.lemon_ant.jharmonizer.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
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

@Slf4j
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Lombok @NonNull guard; class is not finalizable")
abstract class BaseCommand implements Callable<Integer> {

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
    private Path configFilePath;

    protected abstract FlowType getFlowType();

    @NonNull
    protected abstract SourceProcessor createSourceProcessor(Path configFilePath);

    protected int checkFailedExitCode() {
        return 1;
    }

    @Override
    @SuppressWarnings({"PMD.GuardLogStatement", "PMD.AvoidCatchingGenericException"})
    public final Integer call() {
        Path effectiveBaseDir = baseDir != null ? baseDir.normalize() : Path.of(".");
        Path absoluteBaseDir = effectiveBaseDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(absoluteBaseDir)) {
            log.error("Base directory does not exist or is not a directory: {}", absoluteBaseDir);
            return 1;
        }
        Path effectiveConfigFilePath =
                configFilePath != null ? configFilePath.toAbsolutePath().normalize() : null;
        if (effectiveConfigFilePath != null && !Files.isRegularFile(effectiveConfigFilePath)) {
            log.error("Config file does not exist or is not a regular file: {}", effectiveConfigFilePath);
            return 1;
        }
        CommandOptions commandOptions = new CommandOptions(
                effectiveBaseDir, Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs), verbose, effectiveConfigFilePath);
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
    protected static SourceProcessor createDefaultSourceProcessor(Path configFilePath) {
        return configFilePath != null ? new SourceProcessor(configFilePath) : new SourceProcessor();
    }

    @NonNull
    private static String describeConfigSource(Path configFilePath) {
        return configFilePath != null
                ? configFilePath.toString()
                : "embedded default config from core classpath resource /default-config.yml";
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

        Path configFilePath;
    }
}
