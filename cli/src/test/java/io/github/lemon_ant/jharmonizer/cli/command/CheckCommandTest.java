package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CheckCommandTest {

    @Test
    void checkCommand_baseDirOption_invokesProcessorWithCheckAllFlow() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor).processSources(eq(Path.of("src")), any(), any(), eq(FlowType.CHECK_ALL));
    }

    @Test
    void checkCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor).processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), any());
    }

    @Test
    void checkCommand_allFilesChecked_returnsExitCode0() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(5, 1024, 1000000, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isZero();
    }

    @Test
    void checkCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        CommandLine cmd = new CommandLine(new CheckCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(1);
    }
}
