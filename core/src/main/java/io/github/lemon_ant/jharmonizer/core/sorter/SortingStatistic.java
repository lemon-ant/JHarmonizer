package io.github.lemon_ant.jharmonizer.core.sorter;

import lombok.Value;

/**
 * Timing statistics collected during a single member-sorting pass.
 */
@Value
public class SortingStatistic {
    long sortingTimeInNanos;
}
