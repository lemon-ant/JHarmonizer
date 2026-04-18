package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Pipeline-level result of processing all source files through a flow.
 * Carries the aggregated statistics and overall success flag.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SrcProcessingResult {

    @NonNull
    AggregatedProcessingStatistic statistics;

    boolean success;
}
