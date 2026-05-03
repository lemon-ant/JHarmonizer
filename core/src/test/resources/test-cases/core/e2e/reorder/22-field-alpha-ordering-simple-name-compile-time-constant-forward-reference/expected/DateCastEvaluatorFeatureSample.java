// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e.reorder;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class DateCastEvaluatorFeatureSample {
    private static final Pattern AAA_NUMERIC_PATTERN = Pattern.compile("\\d+");
    private static final String ZZZ_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final DateTimeFormatter BBB_FORMATTER_FROM_ZZZ = DateTimeFormatter.ofPattern(ZZZ_FORMAT_PATTERN);

    public static void main(String[] args) {
        if (AAA_NUMERIC_PATTERN == null
                || !"yyyy/MM/dd HH:mm:ss".equals(ZZZ_FORMAT_PATTERN)
                || BBB_FORMATTER_FROM_ZZZ == null) {
            throw new IllegalStateException("Unexpected field values after initialization:"
                    + " AAA_NUMERIC_PATTERN=" + AAA_NUMERIC_PATTERN
                    + ", ZZZ_FORMAT_PATTERN=" + ZZZ_FORMAT_PATTERN
                    + ", BBB_FORMATTER_FROM_ZZZ=" + BBB_FORMATTER_FROM_ZZZ);
        }
    }
}
