package io.github.lemon_ant.jharmonizer.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

    private static final String STDOUT_APPENDER_NAME = "STDOUT";
    private static final String VERBOSE_LOG_PATTERN = "%-5level [%-8.8thread] [%logger{36}] %msg%n";
    private static final int DEFAULT_CHECK_FAILED_EXIT_CODE = 1;

    private final int checkFailedExitCode;

    /**
     * Creates a new base CLI command with the default check-failed exit code.
     */
    protected BaseCommand() {
        this(DEFAULT_CHECK_FAILED_EXIT_CODE);
    }

    /**
     * Creates a new base CLI command with a custom check-failed exit code.
     *
     * @param checkFailedExitCode exit code returned when a check flow detects violations
     */
    protected BaseCommand(int checkFailedExitCode) {
        this.checkFailedExitCode = checkFailedExitCode;
    }

    @Option(
            names = {"-b", "--base-dir"},
            description = {
                "Base directory containing Java source files.",
                "Defaults to the current directory when not specified."
            })
    @Nullable
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

    @Option(
            names = {"-B", "--no-backup"},
            description = "Disable backup (.bak) file creation even when config enables backups.")
    private boolean noBackup;

    @Option(
            names = {"-S", "--no-statistics"},
            description = "Disable final processing statistics report output.")
    private boolean noStatistics;

    /**
     * Returns the processing flow implemented by the command.
     *
     * @return the flow type to execute
     */
    @NonNull
    protected abstract FlowType getFlowType();

    /**
     * Parses command-line options and runs the selected processing flow.
     *
     * @return the process exit code
     */
    @Override
    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public final Integer call() {
        Path normalizedBaseDir = baseDir != null
                ? baseDir.toAbsolutePath().normalize()
                : Path.of(".").toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedBaseDir)) {
            log.error("Base directory does not exist or is not a directory: {}", normalizedBaseDir);
            return 1;
        }
        Path effectiveConfigFilePath = toAbsoluteNormalizedPath(configFilePath);
        if (effectiveConfigFilePath != null && !Files.isRegularFile(effectiveConfigFilePath)) {
            log.error("Config file does not exist or is not a regular file: {}", effectiveConfigFilePath);
            return 1;
        }
        CommandOptions commandOptions = CommandOptions.builder()
                .baseDir(normalizedBaseDir)
                .includeGlobs(Set.copyOf(includeGlobs))
                .excludeGlobs(Set.copyOf(excludeGlobs))
                .verbose(verbose)
                .configFilePath(effectiveConfigFilePath)
                .noBackup(noBackup)
                .noStatistics(noStatistics)
                .build();
        if (commandOptions.isVerbose()) {
            ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
            switchToVerboseLogPattern();
        }
        try {
            return processWithFlow(commandOptions);
        } catch (RuntimeException e) {
            logRuntimeFailure(commandOptions.isVerbose(), e);
            return 1;
        }
    }

    private int processWithFlow(CommandOptions commandOptions) {
        FlowType flowType = getFlowType();
        FlexibleUnifiedConfig effectiveConfig = resolveEffectiveConfig(
                commandOptions.getConfigFilePath(), commandOptions.isNoBackup(), commandOptions.isNoStatistics());
        SrcProcessingResult srcProcessingResult = new SrcProcessor(effectiveConfig)
                .processSources(
                        commandOptions.getBaseDir(),
                        commandOptions.getIncludeGlobs(),
                        commandOptions.getExcludeGlobs(),
                        flowType);
        int exitCode = srcProcessingResult.isSuccess() ? 0 : checkFailedExitCode;
        log.info("Exit code: {}", exitCode);
        return exitCode;
    }

    @Nullable
    private static FlexibleUnifiedConfig resolveEffectiveConfig(
            @Nullable Path configFilePath, boolean disableBackups, boolean disableStatisticsOutput) {
        FlexibleUnifiedConfig externalConfig = configFilePath != null
                ? JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configFilePath)
                : null;
        FlexibleUnifiedConfig cliOverrideConfig = (disableBackups || disableStatisticsOutput)
                ? FlexibleUnifiedConfig.builder()
                        .backupsEnabled(disableBackups ? false : null)
                        .printProcessingStatistics(disableStatisticsOutput ? false : null)
                        .build()
                : null;
        return mergeFlexibleConfigs(externalConfig, cliOverrideConfig);
    }

    @Nullable
    private static FlexibleUnifiedConfig mergeFlexibleConfigs(
            @Nullable FlexibleUnifiedConfig baselineConfig, @Nullable FlexibleUnifiedConfig overlayConfig) {
        if (baselineConfig == null) {
            return overlayConfig;
        }
        if (overlayConfig == null) {
            return baselineConfig;
        }
        return UnifiedConfigMerger.merge(baselineConfig, overlayConfig);
    }

    @Nullable
    private static Path toAbsoluteNormalizedPath(@Nullable Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private static void logRuntimeFailure(boolean verbose, RuntimeException exception) {
        if (verbose) {
            log.error("Processing failed with detailed stack trace.", exception);
            return;
        }
        String errorDetails = describeRuntimeFailure(exception);
        log.error("Processing failed: {}. Re-run with -v/--verbose for detailed diagnostics.", errorDetails);
    }

    @NonNull
    private static String describeRuntimeFailure(RuntimeException exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionType;
        }
        return exceptionType + ": " + exceptionMessage;
    }

    @SuppressWarnings("unchecked")
    private static void switchToVerboseLogPattern() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ConsoleAppender<ILoggingEvent> consoleAppender =
                (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender(STDOUT_APPENDER_NAME);
        if (consoleAppender == null) {
            return;
        }
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) consoleAppender.getEncoder();
        encoder.stop();
        encoder.setPattern(VERBOSE_LOG_PATTERN);
        encoder.start();
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder(access = AccessLevel.PRIVATE)
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

        boolean noBackup;

        boolean noStatistics;
    }
}
