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
     * Builds a pipeline-level result from aggregated statistics, flow type, and any stop triggers.
     *
     * <p>Success is determined by the flow type:
     * <ul>
     *   <li>{@code REORDER} — always successful</li>
     *   <li>{@code CHECK_ALL} — successful when no non-conforming files are found</li>
     *   <li>{@code CHECK_FAIL_FAST} — successful when no stop triggers were recorded</li>
     * </ul>
     *
     * @param flowType the processing flow strategy
     * @param statistics the aggregated processing statistics
     * @param stopTriggers the files that caused the pipeline to stop (empty for non-fail-fast flows)
     * @return a pipeline-level result with the correct success flag
     */
    @NonNull
    public static SrcProcessingResult buildResult(
            @NonNull FlowType flowType,
            @NonNull AggregatedProcessingStatistic statistics,
            @NonNull List<@NonNull FlowProcessingResult> stopTriggers) {
        boolean success =
                switch (flowType) {
                    case REORDER -> true;
                    case CHECK_ALL -> statistics.computeNonConformingFileCount() == 0;
                    case CHECK_FAIL_FAST -> stopTriggers.isEmpty();
                };
        return new SrcProcessingResult(statistics, success, Collections.unmodifiableList(stopTriggers));
    }
}
