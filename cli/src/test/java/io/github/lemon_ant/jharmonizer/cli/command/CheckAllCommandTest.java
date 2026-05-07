// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

class CheckAllCommandTest {

    private CommandLine commandLine;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new CheckAllCommand());
    }

    @Test
    void checkCommand_invoked_usesCheckAllFlow() {
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
                .processSources(eq(Path.of("src").toAbsolutePath().normalize()), any(), any(), eq(FlowType.CHECK_ALL));
    }

    @Test
    void checkCommand_nonConformingFilesDetected_returnsCheckFailedExitCode() {
        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(CommandTestUtils.buildFailedResult());
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(ExitCodes.CHECK_FAILED);
    }

    @Test
    void checkCommand_processorThrowsRuntimeException_returnsProcessingErrorExitCode() throws Exception {
        // When
        int exitCode;
        try (AutoCloseable ignoredLogs = CommandTestUtils.suppressBaseCommandLogs();
                MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
                    when(mock.processSources(any(Path.class), any(), any(), any()))
                            .thenThrow(new RuntimeException("Unexpected error"));
                })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(ExitCodes.PROCESSING_ERROR);
    }

    @Test
    void checkCommand_nonConformingFiles_logsReorderFixHint() throws Exception {
        // Given
        List<ILoggingEvent> capturedLogs = new ArrayList<>();

        // When
        try (AutoCloseable logCapture = CommandTestUtils.captureBaseCommandLogEvents(capturedLogs);
                MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
                    when(mock.processSources(any(Path.class), any(), any(), any()))
                            .thenReturn(CommandTestUtils.buildFailedResult());
                })) {
            commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(capturedLogs)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("jharmonizer reorder"));
    }

    @Test
    void checkCommand_nonConformingFilesAndInfoDisabled_returnsCheckFailedExitCode() {
        // Given
        Logger baseCommandLogger = (Logger) LoggerFactory.getLogger(BaseCommand.class);
        Level previousLevel = baseCommandLogger.getLevel();
        baseCommandLogger.setLevel(Level.WARN);

        // When
        int exitCode;
        try (MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(CommandTestUtils.buildFailedResult());
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        } finally {
            baseCommandLogger.setLevel(previousLevel);
        }

        // Then
        assertThat(exitCode).isEqualTo(ExitCodes.CHECK_FAILED);
    }
}
