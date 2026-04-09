package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

class BaseCommandTest {

    private static final String SPOON_TYPE_FACTORY_LOGGER_NAME = "spoon.reflect.factory.TypeFactory";
    private static final String DEFAULT_LOG_PATTERN = "%-5level %msg%n";

    private CommandLine commandLine;
    private Level initialRootLevel;
    private Level initialSpoonLevel;
    private String initialPattern;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new TestCommand());
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        initialRootLevel = rootLogger.getLevel();
        initialSpoonLevel = ((Logger) LoggerFactory.getLogger(SPOON_TYPE_FACTORY_LOGGER_NAME)).getLevel();
        initialPattern = resolveCurrentLogPattern(rootLogger);
    }

    @AfterEach
    void tearDown() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(initialRootLevel);
        ((Logger) LoggerFactory.getLogger(SPOON_TYPE_FACTORY_LOGGER_NAME)).setLevel(initialSpoonLevel);
        restoreLogPattern(rootLogger, initialPattern);
    }

    @Test
    void call_baseDirOptionInvoked_passesNormalizedAbsoluteBaseDir() {
        // When
        int exitCode;
        SrcProcessor constructedProcessor;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src");
            constructedProcessor = srcProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(eq(Path.of("src").toAbsolutePath().normalize()), any(), any(), eq(FlowType.REORDER));
    }

    @Test
    void call_includeOptionInvoked_parsesIncludePatternCorrectly() {
        // When
        int exitCode;
        SrcProcessor constructedProcessor;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src", "--include", "**/*.java");
            constructedProcessor = srcProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(any(Path.class), eq(Set.of("**/*.java")), any(), eq(FlowType.REORDER));
    }

    @Test
    void call_mixedCollectionOptionsInvoked_combinesAllValuesCorrectly() {
        // When
        int exitCode;
        SrcProcessor constructedProcessor;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute(
                    "-b",
                    "src",
                    "-i",
                    "src/main/java/**/*.java,src/test/java/**/*.java",
                    "--include",
                    "src/integrationTest/java/**/*.java",
                    "-e",
                    "**/internal/**",
                    "--exclude",
                    "**/excluded/**,**/*Test.java");
            constructedProcessor = srcProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Path.of("src").toAbsolutePath().normalize()),
                        eq(Set.of(
                                "src/main/java/**/*.java",
                                "src/test/java/**/*.java",
                                "src/integrationTest/java/**/*.java")),
                        eq(Set.of("**/internal/**", "**/excluded/**", "**/*Test.java")),
                        eq(FlowType.REORDER));
    }

    @Test
    void call_processingSucceeds_returnsExitCode0() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isZero();
    }

    @Test
    void call_noBackupOptionInvoked_disablesBackupsInSrcProcessor() {
        // Given
        CommandLine cmd = new CommandLine(new TestCommand());
        AtomicReference<List<?>> constructorArguments = new AtomicReference<>();

        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                mockConstruction(SrcProcessor.class, (mock, context) -> {
                    constructorArguments.set(context.arguments());
                    when(mock.processSources(any(Path.class), any(), any(), any()))
                            .thenReturn(mock(AggregatedProcessingStatistic.class));
                })) {
            exitCode = cmd.execute("--base-dir", "src", "--no-backup");
        }

        // Then
        assertThat(exitCode).isZero();
        assertThat(constructorArguments.get()).hasSize(1);
        Object constructorConfig = constructorArguments.get().getFirst();
        assertThat(constructorConfig).isInstanceOf(FlexibleUnifiedConfig.class);
        FlexibleUnifiedConfig flexibleConfig = (FlexibleUnifiedConfig) constructorConfig;
        assertThat(flexibleConfig.getBackupsEnabled()).contains(false);
    }

    @Test
    void call_noStatisticsOptionInvoked_disablesStatisticsReportInSrcProcessor() {
        // Given
        CommandLine cmd = new CommandLine(new TestCommand());
        AtomicReference<List<?>> constructorArguments = new AtomicReference<>();

        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                mockConstruction(SrcProcessor.class, (mock, context) -> {
                    constructorArguments.set(context.arguments());
                    when(mock.processSources(any(Path.class), any(), any(), any()))
                            .thenReturn(mock(AggregatedProcessingStatistic.class));
                })) {
            exitCode = cmd.execute("--base-dir", "src", "--no-statistics");
        }

        // Then
        assertThat(exitCode).isZero();
        assertThat(constructorArguments.get()).hasSize(1);
        Object constructorConfig = constructorArguments.get().getFirst();
        assertThat(constructorConfig).isInstanceOf(FlexibleUnifiedConfig.class);
        FlexibleUnifiedConfig flexibleConfig = (FlexibleUnifiedConfig) constructorConfig;
        assertThat(flexibleConfig.getPrintProcessingStatistics()).contains(false);
    }

    @Test
    void call_processorThrowsRuntimeException_returnsExitCode1() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void call_baseDirMissing_returnsExitCode1() {
        // Given
        Path missingDirectoryPath = temporaryDirectory.resolve("missing-base-dir");

        // When
        int exitCode = commandLine.execute("--base-dir", missingDirectoryPath.toString());

        // Then
        assertThat(Files.exists(missingDirectoryPath)).isFalse();
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void call_configFileMissing_returnsExitCode1() {
        // Given
        Path missingConfigPath = temporaryDirectory.resolve("missing-config.yml");

        // When
        int exitCode = commandLine.execute("--base-dir", "src", "--config", missingConfigPath.toString());

        // Then
        assertThat(Files.exists(missingConfigPath)).isFalse();
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void call_processorThrowsRuntimeWithoutMessage_returnsExitCode1() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any())).thenThrow(new RuntimeException());
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void call_verboseOptionInvoked_setsDebugLevelAndVerbosePattern() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src", "--verbose");
        }

        // Then
        assertThat(exitCode).isZero();
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertThat(rootLogger.getLevel()).isEqualTo(Level.DEBUG);
        Logger spoonTypeFactoryLogger = (Logger) LoggerFactory.getLogger(SPOON_TYPE_FACTORY_LOGGER_NAME);
        assertThat(spoonTypeFactoryLogger.getLevel()).isEqualTo(Level.WARN);
        assertThat(resolveCurrentLogPattern(rootLogger)).contains("%logger");
    }

    @Test
    void call_verboseWithRuntimeException_logsDetailedStackTrace() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Verbose error"));
        })) {
            exitCode = commandLine.execute("--base-dir", "src", "--verbose");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    private static final class TestCommand extends BaseCommand {

        @Override
        @NonNull
        protected FlowType getFlowType() {
            return FlowType.REORDER;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static String resolveCurrentLogPattern(Logger rootLogger) {
        ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender("STDOUT");
        if (appender == null) {
            return null;
        }
        return ((PatternLayoutEncoder) appender.getEncoder()).getPattern();
    }

    private static void restoreLogPattern(Logger rootLogger, @Nullable String pattern) {
        if (pattern == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender("STDOUT");
        if (appender == null) {
            return;
        }
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();
        encoder.stop();
        encoder.setPattern(pattern);
        encoder.start();
    }
}
