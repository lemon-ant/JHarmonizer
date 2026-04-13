package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Test-only utility for building {@link FileProcessingResult} instances
 * from outside the {@code flow} package (where the builder is package-private).
 */
@UtilityClass
public class FileProcessingResultTestCreator {

    /**
     * Builds a minimal {@link FileProcessingResult} for testing.
     *
     * @param path the file path
     * @param status the processing status
     * @param stopRequested whether stop was requested
     * @param srcSizeInBytes the source size in bytes
     * @param parsingTimeNanos the parsing time in nanoseconds
     * @param sortingTimeNanos the sorting time in nanoseconds
     * @param formattingTimeNanos the formatting time in nanoseconds
     * @param serializationTimeNanos the serialization time in nanoseconds
     * @return a minimal file processing result
     */
    @NonNull
    public static FileProcessingResult create(
            @NonNull Path path,
            @NonNull FileProcessingStatus status,
            boolean stopRequested,
            long srcSizeInBytes,
            long parsingTimeNanos,
            long sortingTimeNanos,
            long formattingTimeNanos,
            long serializationTimeNanos) {
        return FileProcessingResult.builder()
                .path(path)
                .fileProcessingStatus(status)
                .stopRequested(stopRequested)
                .parsingStatistic(new ParsingStatistic(srcSizeInBytes, srcSizeInBytes, 0, 0, 0, parsingTimeNanos))
                .sortingStatistic(new SortingStatistic(sortingTimeNanos))
                .serializationStatistic(new SerializationStatistic(srcSizeInBytes, serializationTimeNanos))
                .formattingStatistic(new FormattingStatistic(srcSizeInBytes, formattingTimeNanos))
                .relocations(List.of())
                .diff("")
                .build();
    }
}
