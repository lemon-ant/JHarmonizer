package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class CheckAllCommandTest {

    @Test
    void checkCommand_baseDirOption_invokesProcessorWithCheckAllFlow() {
        // Given
        CommandLine cmd = new CommandLine(new CheckAllCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute("--base-dir", "src");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor).processSources(eq(Path.of("src")), any(), any(), eq(FlowType.CHECK_ALL));
    }

    @Test
    void checkCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        CommandLine cmd = new CommandLine(new CheckAllCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor).processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), any());
    }

    @Test
    void checkCommand_shortCollectionOptions_parseRepeatedPatternsCorrectly() {
        // Given
        CommandLine cmd = new CommandLine(new CheckAllCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute(
                    "-b",
                    "src",
                    "-i",
                    "src/main/java/**/*.java",
                    "-i",
                    "src/test/java/**/*.java",
                    "-e",
                    "**/internal/**",
                    "-e",
                    "**/*Test.java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Path.of("src")),
                        eq(Set.of("src/main/java/**/*.java", "src/test/java/**/*.java")),
                        eq(Set.of("**/internal/**", "**/*Test.java")),
                        eq(FlowType.CHECK_ALL));
    }

    @Test
    void checkCommand_allFilesChecked_returnsExitCode0() {
        // Given
        CommandLine cmd = new CommandLine(new CheckAllCommand());

        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isZero();
    }

    @Test
    void checkCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        CommandLine cmd = new CommandLine(new CheckAllCommand());

        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));
        })) {
            exitCode = cmd.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    @NonNull
    private static MockedConstruction<SourceProcessor> mockSuccessfulProcessorConstruction() {
        return mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(new AggregatedProcessingStatistic(0, 0, 0, null, null));
        });
    }
}
