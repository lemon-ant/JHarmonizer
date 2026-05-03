/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * Test helper for creating {@link FileProcessingResult} instances in the
 * {@code io.github.lemon_ant.jharmonizer.core.flow} package, where the package-private
 * builder is accessible. Used by tests in other packages that need
 * {@link FileProcessingResult} instances.
 */
public class FileProcessingResultCreator {

    /**
     * Creates a minimal {@link FileProcessingResult} with the given status and path.
     * All statistics are initialised to zero, {@code diff} is empty, and
     * {@code relocations} is {@code null}.
     *
     * @param fileProcessingStatus the processing outcome to assign
     * @param path the source file path to record
     * @param originalSrcCodeSizeInBytes the original source size used for min/max tracking
     * @param stopRequested whether this result signals a pipeline-stop request
     * @return a fully constructed {@link FileProcessingResult}
     */
    @NonNull
    public static FileProcessingResult createResult(
            @NonNull FileProcessingStatus fileProcessingStatus,
            @NonNull Path path,
            long originalSrcCodeSizeInBytes,
            boolean stopRequested) {
        return FileProcessingResult.builder()
                .fileProcessingStatus(fileProcessingStatus)
                .path(path)
                .parsingStatistic(
                        new ParsingStatistic(originalSrcCodeSizeInBytes, originalSrcCodeSizeInBytes, 0, 0, 0, 0))
                .sortingStatistic(new SortingStatistic(0))
                .serializationStatistic(new SerializationStatistic(0, 0))
                .formattingStatistic(new FormattingStatistic(0, 0))
                .memberRelocations(null)
                .diff("")
                .stopRequested(stopRequested)
                .build();
    }

    private FileProcessingResultCreator() {}
}
