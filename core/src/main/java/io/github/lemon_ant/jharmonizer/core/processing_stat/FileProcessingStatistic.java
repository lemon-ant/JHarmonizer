package io.github.lemon_ant.jharmonizer.core.processing_stat;

import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Per-file processing statistics derived from a {@link FlowProcessingResult}.
 * Aggregates wall-clock processing time across all phases and records the original file size.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileProcessingStatistic {
    @NonNull
    Path path;

    long processingTimeNanos;
    long size;

    public static FileProcessingStatistic convert(FlowProcessingResult flowProcessingResult) {
        long processingTime = flowProcessingResult.getParsingStatistic().getParsingTimeInNanos()
                + flowProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                + flowProcessingResult.getSerializationStatistic().getProcessingTimeInNanos()
                + flowProcessingResult.getFormatingStatistic().getFormattingTimeInNanos();
        return new FileProcessingStatistic(
                flowProcessingResult.getPath(),
                processingTime,
                flowProcessingResult.getParsingStatistic().getOriginalSourceCodeLength());
    }
}
