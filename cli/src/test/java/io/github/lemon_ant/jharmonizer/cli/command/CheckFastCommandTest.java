package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotFormattedException;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class CheckFastCommandTest {

    private CommandLine commandLine;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new CheckFastCommand());
    }

    @Test
    void checkFastCommand_invoked_usesCheckFailFastFlow() {
        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Path.of("src").toAbsolutePath().normalize()), any(), any(), eq(FlowType.CHECK_FAIL_FAST));
    }

    @Test
    void checkFastCommand_formattingChangesDetected_returnsExitCode3() {
        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new NotFormattedException(Path.of("SomeFile.java"), "--- diff ---"));
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(3);
    }

    @Test
    void checkFastCommand_orderingChangesDetected_returnsExitCode3() {
        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new NotOrderedException(Path.of("SomeFile.java"), List.of()));
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(3);
    }

    @Test
    void checkFastCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }
}
