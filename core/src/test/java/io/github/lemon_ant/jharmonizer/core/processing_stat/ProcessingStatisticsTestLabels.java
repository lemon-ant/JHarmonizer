// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import lombok.experimental.UtilityClass;

/**
 * Test-only string constants for expected metric labels and section headers produced by
 * {@link ProcessingStatisticsPrintService}.
 */
@UtilityClass
public class ProcessingStatisticsTestLabels {

    /** Label for the "files with unexpected errors" table row. */
    public static final String FILES_WITH_UNEXPECTED_ERRORS = "Files with unexpected errors";

    /** Label for the "wall-clock time" table row. */
    static final String WALL_CLOCK_TIME = "Wall-clock time";

    /** Label for the "total CPU time" table row. */
    static final String TOTAL_CPU_TIME = "Total CPU time";

    /** Label for the "serialization time (share)" table row. */
    static final String SERIALIZATION_TIME_SHARE = "Serialization time (share)";

    /** Label for the "formatting time (share)" table row. */
    static final String FORMATTING_TIME_SHARE = "Formatting time (share)";

    /** Section header printed before the list of unexpected error file paths. */
    static final String UNEXPECTED_ERROR_FILES_HEADER = "Unexpected internal error files:";

    /** Prefix used for the minimum-size file in the size boundary section. */
    static final String MIN_SIZE_FILE_PREFIX = "Min size file:";

    /** Prefix used for the maximum-size file in the size boundary section. */
    static final String MAX_SIZE_FILE_PREFIX = "Max size file:";
}
