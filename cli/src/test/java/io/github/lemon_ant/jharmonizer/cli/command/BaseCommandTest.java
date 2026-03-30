package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class BaseCommandTest {

    private CommandLine commandLine;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new TestCommand());
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
                .processSources(
                        eq(Path.of("src").toAbsolutePath().normalize()), any(), any(), eq(FlowType.RESTRUCTURE));
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
                .processSources(any(Path.class), eq(Set.of("**/*.java")), any(), eq(FlowType.RESTRUCTURE));
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
                        eq(FlowType.RESTRUCTURE));
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

    private static final class TestCommand extends BaseCommand {

        @Override
        @NonNull
        protected FlowType getFlowType() {
            return FlowType.RESTRUCTURE;
        }
    }
}
