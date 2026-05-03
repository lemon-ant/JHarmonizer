/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter;

import lombok.Value;

/**
 * Timing statistics collected during a single member-sorting pass.
 */
@Value
public class SortingStatistic {
    long sortingTimeInNanos;
}
