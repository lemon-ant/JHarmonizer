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
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class RestructureCommandTest {

    @Test
    void restructureCommand_baseDirOption_invokesProcessorWithCorrectBaseDir() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src/main/java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor)
                .processSources(eq(new File("src/main/java").toPath()), any(), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor)
                .processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(1);
    }
}
