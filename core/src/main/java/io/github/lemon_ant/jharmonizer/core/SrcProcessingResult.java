// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
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
