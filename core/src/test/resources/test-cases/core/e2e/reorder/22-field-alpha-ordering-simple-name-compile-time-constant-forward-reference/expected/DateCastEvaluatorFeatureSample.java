package io.github.lemon_ant.jharmonizer.core.e2e.reorder;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class DateCastEvaluatorFeatureSample {
    private static final Pattern AAA_NUMERIC_PATTERN = Pattern.compile("\\d+");
    private static final String ZZZ_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final DateTimeFormatter BBB_FORMATTER_FROM_ZZZ = DateTimeFormatter.ofPattern(ZZZ_FORMAT_PATTERN);
}
