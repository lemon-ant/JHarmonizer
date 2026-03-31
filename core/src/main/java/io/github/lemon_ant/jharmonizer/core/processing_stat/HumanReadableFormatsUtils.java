package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Internal utility methods for formatting source-length and durations into human-readable strings.
 */
@UtilityClass
final class HumanReadableFormatsUtils {

    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

    private static final DecimalFormat DECIMAL_0 = new DecimalFormat("#,##0", DECIMAL_FORMAT_SYMBOLS);

    /**
     * Formats the source-length measured in UTF-16 char units.
     * @param sourceLengthChars the source-length measured in chars
     * @return the result
     */
    @SuppressWarnings("PMD.UnsynchronizedStaticFormatter")
    @NonNull
    static String formatSourceLengthChars(long sourceLengthChars) {
        if (sourceLengthChars < 0) {
            throw new IllegalArgumentException("Source-length must be non-negative, but was: " + sourceLengthChars);
        }
        return DECIMAL_0.format(sourceLengthChars) + " chars";
    }

    /**
     * Formats the seconds microseconds from nanos.
     * @param durationNanos the duration nanos
     * @return the result
     */
    @NonNull
    static String formatSecondsMicrosecondsFromNanos(long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("Duration must be non-negative, but was: " + durationNanos + " ns");
        }

        Duration duration = Duration.ofNanos(durationNanos);
        long totalSeconds = duration.getSeconds();
        int millisecondsPart = duration.getNano() / 1_000_000; // 0..999

        return String.format(Locale.ROOT, "%d.%03d", totalSeconds, millisecondsPart);
    }

    /**
     * Formats the hms millis from nanos.
     * @param durationNanos the duration nanos
     * @return the result
     */
    @NonNull
    static String formatHmsMillisFromNanos(long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("Duration must be non-negative, but was: " + durationNanos + " ns");
        }

        // Truncate to milliseconds (standard behavior in most tooling).
        long totalMillis = NANOSECONDS.toMillis(durationNanos);

        long totalSeconds = totalMillis / 1_000L;
        long millisecondsPart = totalMillis % 1_000L;

        long totalHours = totalSeconds / 3_600L;
        long minutesPart = (totalSeconds % 3_600L) / 60L;
        long secondsPart = totalSeconds % 60L;

        return String.format(Locale.ROOT, "%d:%02d:%02d.%03d", totalHours, minutesPart, secondsPart, millisecondsPart);
    }
}
