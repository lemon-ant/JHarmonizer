// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResult;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Per-file processing statistics derived from a {@link FileProcessingResult}.
 * Aggregates wall-clock processing time across all phases and records original file size in bytes.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileProcessingStatistic {
    @NonNull
    Path path;

    long processingTimeNanos;
    long serializationTimeNanos;
    long sizeInBytes;

    /**
     * Converts a per-file processing result into a statistics record.
     *
     * @param fileProcessingResult the per-file processing result
     * @return the derived statistics
     */
    @NonNull
    public static FileProcessingStatistic convert(@NonNull FileProcessingResult fileProcessingResult) {
        long serializationTimeNanos =
                fileProcessingResult.getSerializationStatistic().getProcessingTimeInNanos();
        long processingTimeNanos = fileProcessingResult.getParsingStatistic().getParsingTimeInNanos()
                + fileProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                + serializationTimeNanos
                + fileProcessingResult.getFormattingStatistic().getFormattingTimeInNanos();
        return new FileProcessingStatistic(
                fileProcessingResult.getPath(),
                processingTimeNanos,
                serializationTimeNanos,
                fileProcessingResult.getParsingStatistic().getOriginalSrcCodeSizeInBytes());
    }
}
