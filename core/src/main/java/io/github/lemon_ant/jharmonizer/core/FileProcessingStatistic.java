package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileProcessingStatistic {
    @NonNull
    Path path;

    long processingTimeNanos;
    long size;

    public static FileProcessingStatistic convert(FlowProcessingResult flowProcessingResult) {
        long processingTime = TimeUnit.MILLISECONDS.toNanos(
                flowProcessingResult.getParsingStatistic().getParsingTimeInNanos()
                        + flowProcessingResult.getSortingStatistic().getSortingTimeInNanos()
                        + flowProcessingResult.getSerializationStatistic().getProcessingTimeInNanos()
                        + flowProcessingResult.getFormatingStatistic().getFormattingTimeInNanos());
        return new FileProcessingStatistic(
                flowProcessingResult.getPath(),
                flowProcessingResult.getParsingStatistic().getOriginalSourceCodeLength(),
                processingTime);
    }
}
