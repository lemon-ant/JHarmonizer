package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Pipeline-level result of processing all source files through a flow.
 * Carries the aggregated statistics and overall success flag.
 * Each flow determines its own success criteria via {@link IFlow#isSuccessful}.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SrcProcessingResult {

    @NonNull
    AggregatedProcessingStatistic statistics;

    boolean success;

    /**
     * Creates a pipeline-level result from aggregated statistics and a success flag.
     *
     * @param statistics the aggregated processing statistics
     * @param success whether the flow considers the run successful
     * @return the pipeline-level result
     */
    @NonNull
    public static SrcProcessingResult of(@NonNull AggregatedProcessingStatistic statistics, boolean success) {
        return new SrcProcessingResult(statistics, success);
    }
}
