// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
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
import org.jspecify.annotations.Nullable;
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
    private static final int DEFAULT_CHECK_FAILED_EXIT_CODE = ExitCodes.PROCESSING_ERROR;

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
            names = {"-s", "--statistics-mode"},
            description = "Processing statistics output mode: ${COMPLETION-CANDIDATES}.")
    @Nullable
    private ProcessingStatisticsMode statisticsMode;

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
        Path effectiveBaseDir = baseDir != null ? baseDir : Path.of(".");
        Path absoluteBaseDir = toAbsoluteNormalizedPath(effectiveBaseDir);
        if (!Files.isDirectory(absoluteBaseDir)) {
            log.error("Base directory does not exist or is not a directory: {}", absoluteBaseDir);
            return ExitCodes.PROCESSING_ERROR;
        }
        Path effectiveConfigFilePath = toAbsoluteNormalizedPath(configFilePath);
        if (effectiveConfigFilePath != null && !Files.isRegularFile(effectiveConfigFilePath)) {
            log.error("Config file does not exist or is not a regular file: {}", effectiveConfigFilePath);
            return ExitCodes.PROCESSING_ERROR;
        }
        CommandOptions commandOptions = CommandOptions.builder()
                .baseDir(absoluteBaseDir)
                .includeGlobs(Set.copyOf(includeGlobs))
                .excludeGlobs(Set.copyOf(excludeGlobs))
                .verbose(verbose)
                .configFilePath(effectiveConfigFilePath)
                .noBackup(noBackup)
                .statisticsMode(statisticsMode)
                .build();
        if (commandOptions.isVerbose()) {
            ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
            switchToVerboseLogPattern();
        }
        try {
            return processWithFlow(commandOptions);
        } catch (RuntimeException e) {
            logRuntimeFailure(commandOptions.isVerbose(), e);
            return ExitCodes.PROCESSING_ERROR;
        }
    }

    private int processWithFlow(CommandOptions commandOptions) {
        FlowType flowType = getFlowType();
        FlexibleUnifiedConfig effectiveConfig = resolveEffectiveConfig(
                commandOptions.getConfigFilePath(), commandOptions.isNoBackup(), commandOptions.getStatisticsMode());
        SrcProcessingResult srcProcessingResult = new SrcProcessor(effectiveConfig)
                .processSources(
                        commandOptions.getBaseDir(),
                        commandOptions.getIncludeGlobs(),
                        commandOptions.getExcludeGlobs(),
                        flowType);
        if (!srcProcessingResult.isSuccess()
                && (flowType == FlowType.CHECK_ALL || flowType == FlowType.CHECK_FAIL_FAST)
                && log.isInfoEnabled()) {
            log.info(
                    "To automatically fix these violations, run:\n{}",
                    ReorderCommandRenderer.render(
                            CliLauncherDetector.detectLauncherPrefix(),
                            commandOptions.getBaseDir(),
                            commandOptions.getIncludeGlobs(),
                            commandOptions.getExcludeGlobs(),
                            commandOptions.getConfigFilePath(),
                            commandOptions.isNoBackup(),
                            commandOptions.getStatisticsMode()));
        }
        int exitCode = srcProcessingResult.isSuccess() ? ExitCodes.OK : checkFailedExitCode;
        log.info("Exit code: {}", exitCode);
        return exitCode;
    }

    @Nullable
    private static FlexibleUnifiedConfig resolveEffectiveConfig(
            @Nullable Path configFilePath, boolean disableBackups, @Nullable ProcessingStatisticsMode statisticsMode) {
        FlexibleUnifiedConfig externalConfig = configFilePath != null
                ? JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configFilePath)
                : null;
        FlexibleUnifiedConfig cliOverrideConfig = (disableBackups || statisticsMode != null)
                ? FlexibleUnifiedConfig.builder()
                        .backupsEnabled(disableBackups ? false : null)
                        .processingStatisticsMode(statisticsMode)
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
        lowerStdoutThresholdToDebug(consoleAppender);
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) consoleAppender.getEncoder();
        encoder.stop();
        encoder.setPattern(VERBOSE_LOG_PATTERN);
        encoder.start();
    }

    private static void lowerStdoutThresholdToDebug(ConsoleAppender<ILoggingEvent> consoleAppender) {
        ThresholdFilter thresholdFilter = findStdoutThresholdFilter(consoleAppender);
        if (thresholdFilter == null) {
            return;
        }
        thresholdFilter.setLevel(Level.DEBUG.toString());
        thresholdFilter.start();
    }

    @Nullable
    private static ThresholdFilter findStdoutThresholdFilter(ConsoleAppender<ILoggingEvent> consoleAppender) {
        return consoleAppender.getCopyOfAttachedFiltersList().stream()
                .filter(ThresholdFilter.class::isInstance)
                .map(ThresholdFilter.class::cast)
                .findFirst()
                .orElse(null);
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

        @Nullable
        ProcessingStatisticsMode statisticsMode;
    }
}
