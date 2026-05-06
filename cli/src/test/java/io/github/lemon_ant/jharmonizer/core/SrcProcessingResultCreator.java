// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Creator that accesses the package-private constructor of {@link SrcProcessingResult}.
 * Lives in the same package as {@code SrcProcessingResult} so no reflection is needed.
 */
@UtilityClass
public class SrcProcessingResultCreator {

    /**
     * Creates a {@link SrcProcessingResult} via the package-private constructor.
     *
     * @param statistics aggregated processing statistics
     * @param success whether the processing was successful
     * @return a new result instance
     */
    @NonNull
    public static SrcProcessingResult create(@NonNull AggregatedProcessingStatistic statistics, boolean success) {
        return new SrcProcessingResult(statistics, success);
    }
}
