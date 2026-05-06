// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

/**
 * Controls the verbosity of the final processing statistics report.
 * <ul>
 *   <li>{@link #FULL} – a detailed pseudo-table with per-phase timings and size breakdown.</li>
 *   <li>{@link #MINIMAL} – a brief single-line summary with file count, wall-clock time and error count.</li>
 *   <li>{@link #DISABLED} – no statistics output; a debug-level entry is written instead.</li>
 * </ul>
 */
public enum ProcessingStatisticsMode {

    /**
     * Print a detailed pseudo-table report with per-phase timings and size breakdown.
     */
    FULL,

    /**
     * Print a brief single-line summary: file count, wall-clock time, total size and error count.
     * This is the default mode.
     */
    MINIMAL,

    /**
     * Suppress all statistics output.
     * A debug-level completion entry is written instead.
     */
    DISABLED
}
