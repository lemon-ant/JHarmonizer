// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceLoggingFlowIntegrationTest {
    private final Logger flowLogger = (Logger) LoggerFactory.getLogger(IFlow.class.getPackageName());

    @TempDir
    Path temporaryDirectory;

    @Nullable
    private Level previousLogLevel;

    private SrcProcessor srcProcessor;

    @BeforeAll
    void setUpProcessor() {
        srcProcessor = new SrcProcessor();
    }

    @BeforeEach
    void enableTraceLogging() {
        previousLogLevel = flowLogger.getLevel();
        flowLogger.setLevel(Level.TRACE);
    }

    @AfterEach
    void restoreLoggingLevel() {
        flowLogger.setLevel(previousLogLevel);
    }

    @ParameterizedTest
    @EnumSource(FlowType.class)
    void processSources_traceLoggingWithoutDebugDirectoryAccess_completesProcessing(@NonNull FlowType flowType)
            throws IOException {
        // Given
        Path srcPath = temporaryDirectory.resolve("TraceLogging.java");
        String srcCode = "class TraceLogging {}\n";
        Files.writeString(srcPath, srcCode);
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, invocation -> {
            if (invocation.getMethod().getName().equals("createDirectories")
                    && Path.of("debug").equals(invocation.getArgument(0))) {
                throw new AccessDeniedException("debug");
            }
            return invocation.callRealMethod();
        })) {

            // When
            SrcProcessingResult result =
                    srcProcessor.processSources(temporaryDirectory, List.of("**/*.java"), List.of(), flowType);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatistics().getFileCount()).isEqualTo(1);
            assertThat(result.getStatistics().getFilesWithUnexpectedErrors()).isEmpty();
            assertThat(Files.readString(srcPath)).isEqualTo(srcCode);
        }
    }
}
