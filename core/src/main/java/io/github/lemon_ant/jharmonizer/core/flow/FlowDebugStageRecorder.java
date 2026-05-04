// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Optional debug helper that writes intermediate source-code snapshots to disk for each
 * processing stage when TRACE logging is enabled.
 * Files are written directly into the {@code debug/} directory with timestamp-prefixed file names.
 */
@Slf4j
final class FlowDebugStageRecorder {

    private static final Path BASE_OUTPUT_DIRECTORY = Path.of("debug");

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss").withZone(ZoneId.systemDefault());

    private final boolean enabled;
    private final FlowType flowType;
    private final String runTimestampPrefix;
    private final Path flowOutputDirectory;

    /**
     * Creates a new FlowDebugStageRecorder.
     * @param flowType the processing flow to run
     */
    FlowDebugStageRecorder(@NonNull FlowType flowType) {
        this(flowType, Clock.systemDefaultZone());
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private FlowDebugStageRecorder(FlowType flowType, Clock clock) {
        this.enabled = log.isTraceEnabled();
        this.flowType = flowType;
        this.runTimestampPrefix = TIMESTAMP_FORMATTER.format(Instant.now(clock));
        this.flowOutputDirectory = BASE_OUTPUT_DIRECTORY;

        if (enabled) {
            ensureDirectoryExists(flowOutputDirectory);
            log.trace("Debug stage recorder enabled. Output directory: {}", flowOutputDirectory.toAbsolutePath());
        }
    }

    private static void ensureDirectoryExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to create debug output directory: " + directory, ioException);
        }
    }

    /**
     * Writes one intermediate source snapshot for the given processing stage.
     *
     * @param fileName the source file path being recorded
     * @param stage the processing stage represented by the snapshot
     * @param javaSourceText the Java source text to persist
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    void recordSrcStage(
            @NonNull Path fileName, @NonNull FlowDebugStageRecorder.SrcFlowStage stage, @NonNull String javaSrcText) {
        if (!enabled) {
            return;
        }

        String outputFileName = runTimestampPrefix
                + "_" + Clock.systemDefaultZone().millis()
                + "__" + flowType
                + "__" + fileName.getFileName()
                + "__" + stage.getPhaseNumber() + "_" + stage
                + ".java";

        Path outputFile = flowOutputDirectory.resolve(outputFileName);

        try {
            Files.writeString(outputFile, javaSrcText, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            log.trace("Saved debug stage: {}", outputFile.toAbsolutePath());
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to write debug stage file: " + outputFile, ioException);
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum SrcFlowStage {
        FORMATTED(3),
        ORIGINAL(1),
        SORTED(2);

        private final int phaseNumber;
    }
}
