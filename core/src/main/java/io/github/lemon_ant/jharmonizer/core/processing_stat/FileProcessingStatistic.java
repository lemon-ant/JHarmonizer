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
    long size;

    /**
     * Performs the convert.
     * @param fileProcessingResult the flow processing result
     * @return the result
     */
    @NonNull
    public static FileProcessingStatistic convert(@NonNull FileProcessingResult fileProcessingResult) {
        long serializationTime =
                fileProcessingResult.getSerializationStatistic().getProcessingTimeInNanos();
        long processingTime = fileProcessingResult.getParsingStatistic().getParsingTimeInNanos()
                + fileProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                + serializationTime
                + fileProcessingResult.getFormattingStatistic().getFormattingTimeInNanos();
        return new FileProcessingStatistic(
                fileProcessingResult.getPath(),
                processingTime,
                serializationTime,
                fileProcessingResult.getParsingStatistic().getOriginalSrcCodeSizeInBytes());
    }
}
