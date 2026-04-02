package org.apache.nifi.attribute.expression.language.evaluation.cast;

import java.time.format.DateTimeFormatter;

public class DateCastEvaluator {
    public static final String ALTERNATE_FORMAT_WITHOUT_MILLIS = "yyyy/MM/dd HH:mm:ss";
    public static final DateTimeFormatter ALTERNATE_FORMATTER_WITHOUT_MILLIS =
            DateTimeFormatter.ofPattern(ALTERNATE_FORMAT_WITHOUT_MILLIS);
}
