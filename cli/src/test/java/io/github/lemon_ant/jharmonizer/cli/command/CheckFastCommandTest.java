package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotFormattedException;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CheckFastCommandTest {

    @Test
    void checkFastCommand_baseDirOption_invokesProcessorWithCheckFailFastFlow() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor).processSources(eq(new File("src").toPath()), any(), any(), eq(FlowType.CHECK_FAIL_FAST));
    }

    @Test
    void checkFastCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor).processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), any());
    }

    @Test
    void checkFastCommand_formattingChangesDetected_returnsExitCode3() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new NotFormattedException(Path.of("SomeFile.java"), "--- diff ---"));
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(3);
    }

    @Test
    void checkFastCommand_orderingChangesDetected_returnsExitCode3() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new NotOrderedException(Path.of("SomeFile.java"), List.of()));
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(3);
    }

    @Test
    void checkFastCommand_noChangesDetected_returnsExitCode0() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(5, 1024, 1000000, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isZero();
    }

    @Test
    void checkFastCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        CommandLine cmd = new CommandLine(new CheckFastCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(1);
    }
}
