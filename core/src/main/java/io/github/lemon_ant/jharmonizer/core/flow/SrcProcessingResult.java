package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Pipeline-level result of processing all source files through a flow.
 * Carries the aggregated statistics, overall success flag, and the list of
 * files that triggered pipeline stop (relevant for fail-fast check flow only,
 * where parallel threads may detect multiple stop triggers concurrently).
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SrcProcessingResult {

    @NonNull
    AggregatedProcessingStatistic statistics;

    boolean success;

    @NonNull
    List<@NonNull FlowProcessingResult> stopTriggers;

    /**
     * Creates a successful processing result with no stop triggers.
     *
     * @param statistics the aggregated processing statistics
     * @return a successful result
     */
    @NonNull
    static SrcProcessingResult successful(@NonNull AggregatedProcessingStatistic statistics) {
        return new SrcProcessingResult(statistics, true, List.of());
    }

    /**
     * Creates a failed processing result with the given stop triggers.
     *
     * @param statistics the aggregated processing statistics
     * @param stopTriggers the files that caused the pipeline to stop
     * @return a failed result
     */
    @NonNull
    static SrcProcessingResult failed(
            @NonNull AggregatedProcessingStatistic statistics,
            @NonNull List<@NonNull FlowProcessingResult> stopTriggers) {
        return new SrcProcessingResult(statistics, false, Collections.unmodifiableList(stopTriggers));
    }
}
