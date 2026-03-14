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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Set;
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
        verify(mockProcessor).processSources(eq(Path.of("src/main/java")), any(), any(), eq(FlowType.RESTRUCTURE));
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
    void restructureCommand_mixedCollectionOptions_combineAllValuesCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute(
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

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor)
                .processSources(
                        eq(Path.of("src")),
                        eq(Set.of(
                                "src/main/java/**/*.java",
                                "src/test/java/**/*.java",
                                "src/integrationTest/java/**/*.java")),
                        eq(Set.of("**/internal/**", "**/excluded/**", "**/*Test.java")),
                        eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_helpUsage_describeOptionAliasesAndCollectionFormats() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand(mock(SourceProcessor.class)));
        StringWriter usage = new StringWriter();

        // When
        cmd.usage(new PrintWriter(usage), CommandLine.Help.Ansi.OFF);

        // Then
        assertThat(usage.toString())
                .contains("-b, --base-dir")
                .contains("-i, --include")
                .contains("-e, --exclude")
                .contains("Repeat this option or pass multiple patterns as a")
                .contains("comma-separated list.");
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
