package io.github.lemon_ant.jharmonizer.core.processing_stat;

import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Per-file processing statistics derived from a {@link FlowProcessingResult}.
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
     * @param flowProcessingResult the flow processing result
     * @return the result
     */
    @NonNull
    public static FileProcessingStatistic convert(@NonNull FlowProcessingResult flowProcessingResult) {
        long serializationTime = flowProcessingResult.getSerializationStatistic().getProcessingTimeInNanos();
        long processingTime = flowProcessingResult.getParsingStatistic().getParsingTimeInNanos()
                + flowProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                + serializationTime
                + flowProcessingResult.getFormattingStatistic().getFormattingTimeInNanos();
        return new FileProcessingStatistic(
                flowProcessingResult.getPath(),
                processingTime,
                serializationTime,
                flowProcessingResult.getParsingStatistic().getOriginalSrcCodeSizeInBytes());
    }
}
