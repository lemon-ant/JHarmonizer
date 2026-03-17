package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Internal utility methods for formatting byte sizes and durations into human-readable strings.
 */
@UtilityClass
final class HumanReadableFormatsUtils {

    private static final long KIB = 1024L;
    private static final long MIB = KIB * 1024L;
    private static final long GIB = MIB * 1024L;

    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

    private static final DecimalFormat DECIMAL_0 = new DecimalFormat("#,##0", DECIMAL_FORMAT_SYMBOLS);

    private static final DecimalFormat DECIMAL_1 = new DecimalFormat("#,##0.0", DECIMAL_FORMAT_SYMBOLS);

    @SuppressWarnings("PMD.UnsynchronizedStaticFormatter")
    static String formatBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Byte size must be non-negative, but was: " + bytes);
        }

        if (bytes < KIB) {
            return DECIMAL_0.format(bytes) + " B";
        }
        if (bytes < MIB) {
            return formatBinary(bytes, KIB, " KiB");
        }
        if (bytes < GIB) {
            return formatBinary(bytes, MIB, " MiB");
        }
        return formatBinary(bytes, GIB, " GiB");
    }

    static String formatSecondsMicrosecondsFromNanos(long durationNanos) {
        if (durationNanos < 0) {
            throw new IllegalArgumentException("Duration must be non-negative, but was: " + durationNanos + " ns");
        }

        Duration duration = Duration.ofNanos(durationNanos);
        long totalSeconds = duration.getSeconds();
        int millisecondsPart = duration.getNano() / 1_000_000; // 0..999

        return String.format(Locale.ROOT, "%d.%03d", totalSeconds, millisecondsPart);
    }

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

    @NonNull
    private static String formatBinary(long bytes, long unit, String suffix) {
        double value = (double) bytes / (double) unit;
        DecimalFormat decimalFormat = value < 10.0 ? DECIMAL_1 : DECIMAL_0;
        return decimalFormat.format(value) + suffix;
    }
}
