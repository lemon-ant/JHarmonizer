// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;
import static java.lang.System.lineSeparator;

import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Formats formatting violation diffs into human-readable log messages.
 *
 * <p>Produces a single multi-line string that can be passed directly to a logger.
 * The file path is placed on its own indented line after the header (abbreviated when long),
 * followed by the diff produced by {@link DiffReporter}.
 *
 * <p>Example output:
 * <pre>
 * Formatting violations in:
 *   Sample.java
 * {@literal @}@ -1,1 +1,3 @@
 * -|class·CFormatted{int·a;}¶
 * +|class·CFormatted·{¶
 * +|····int·a;¶
 * +|}¶
 * </pre>
 */
@UtilityClass
public class FormattingViolationPrinter {

    private static final int MAX_PATH_DISPLAY_LENGTH = 120;

    /**
     * Formats the formatting violation into a human-readable string.
     *
     * <p>The path is placed on its own indented line after the header, abbreviated to
     * at most {@value MAX_PATH_DISPLAY_LENGTH} characters.
     *
     * @param path the path of the file where formatting violations were detected
     * @param diff the formatted diff string produced by {@link DiffReporter}
     * @return a formatted string representing the violations
     */
    @NonNull
    public static String printFormattingViolation(@NonNull Path path, @NonNull String diff) {
        return "Formatting violations in:"
                + lineSeparator()
                + "  "
                + abbreviatePathForDisplay(path, MAX_PATH_DISPLAY_LENGTH)
                + lineSeparator()
                + diff;
    }
}
