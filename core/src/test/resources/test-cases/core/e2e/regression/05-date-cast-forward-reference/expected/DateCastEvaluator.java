package org.apache.nifi.attribute.expression.language.evaluation.cast;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.util.FormatUtils;

public class DateCastEvaluator extends DateEvaluator {
    public static final String ALTERNATE_FORMAT_WITHOUT_MILLIS = "yyyy/MM/dd HH:mm:ss";
    public static final String ALTERNATE_FORMAT_WITH_MILLIS = "yyyy/MM/dd HH:mm:ss.SSS";
    public static final DateTimeFormatter ALTERNATE_FORMATTER_WITHOUT_MILLIS =
            FormatUtils.prepareLenientCaseInsensitiveDateTimeFormatter(ALTERNATE_FORMAT_WITHOUT_MILLIS);
    public static final DateTimeFormatter ALTERNATE_FORMATTER_WITH_MILLIS =
            FormatUtils.prepareLenientCaseInsensitiveDateTimeFormatter(ALTERNATE_FORMAT_WITH_MILLIS);
    public static final Pattern ALTERNATE_PATTERN =
            Pattern.compile("\\d{4}/\\d{2}/\\d{2} \\d{2}\\:\\d{2}\\:\\d{2}(\\.\\d{3})?");
    public static final String DATE_TO_STRING_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    public static final DateTimeFormatter DATE_TO_STRING_FORMATTER =
            FormatUtils.prepareLenientCaseInsensitiveDateTimeFormatter(DATE_TO_STRING_FORMAT);
    public static final Pattern DATE_TO_STRING_PATTERN =
            Pattern.compile("(?:[a-zA-Z]{3} ){2}\\d{2} \\d{2}\\:\\d{2}\\:\\d{2} (?:.*?) \\d{4}");
    public static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final Evaluator<?> subjectEvaluator;
}
