package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HumanReadableFormatsUtilsTest {

    @Test
    void formatBytes_zeroBytesValue_returnsZeroB() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(0L);

        // Then
        assertThat(result).isEqualTo("0 B");
    }

    @Test
    void formatBytes_lessThanOneKib_returnsB() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(500L);

        // Then
        assertThat(result).endsWith(" B");
    }

    @Test
    void formatBytes_exactlyOneKib_returnsKib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(1024L);

        // Then
        assertThat(result).endsWith(" KiB");
    }

    @Test
    void formatBytes_lessThanOneMib_returnsKib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(512L * 1024L);

        // Then
        assertThat(result).endsWith(" KiB");
    }

    @Test
    void formatBytes_exactlyOneMib_returnsMib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(1024L * 1024L);

        // Then
        assertThat(result).endsWith(" MiB");
    }

    @Test
    void formatBytes_lessThanOneGib_returnsMib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(512L * 1024L * 1024L);

        // Then
        assertThat(result).endsWith(" MiB");
    }

    @Test
    void formatBytes_oneGibOrMore_returnsGib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(1024L * 1024L * 1024L);

        // Then
        assertThat(result).endsWith(" GiB");
    }

    @Test
    void formatBytes_negativeValue_throwsIllegalArgumentException() {
        // When / Then
        assertThatThrownBy(() -> HumanReadableFormatsUtils.formatBytes(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void formatBytes_largeGibValue_returnsGib() {
        // When
        String result = HumanReadableFormatsUtils.formatBytes(5L * 1024L * 1024L * 1024L);

        // Then
        assertThat(result).endsWith(" GiB").contains("5");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_zeroNanos_returnsZero() {
        // When
        String result = HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(0L);

        // Then
        assertThat(result).isEqualTo("0.000");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_oneSecond_returnsOneSecond() {
        // When
        String result = HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(1_000_000_000L);

        // Then
        assertThat(result).isEqualTo("1.000");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_oneSecondAndHalfMillisecond_returnsCorrectFormat() {
        // When
        String result = HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(1_500_000_000L);

        // Then
        assertThat(result).isEqualTo("1.500");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_negativeValue_throwsIllegalArgumentException() {
        // When / Then
        assertThatThrownBy(() -> HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void formatHmsMillisFromNanos_zeroNanos_returnsZeroTime() {
        // When
        String result = HumanReadableFormatsUtils.formatHmsMillisFromNanos(0L);

        // Then
        assertThat(result).isEqualTo("0:00:00.000");
    }

    @Test
    void formatHmsMillisFromNanos_oneHour_returnsOneHour() {
        // When
        String result = HumanReadableFormatsUtils.formatHmsMillisFromNanos(3_600_000_000_000L);

        // Then
        assertThat(result).isEqualTo("1:00:00.000");
    }

    @Test
    void formatHmsMillisFromNanos_oneMinute_returnsOneMinute() {
        // When
        String result = HumanReadableFormatsUtils.formatHmsMillisFromNanos(60_000_000_000L);

        // Then
        assertThat(result).isEqualTo("0:01:00.000");
    }

    @Test
    void formatHmsMillisFromNanos_oneMillisecond_returnsCorrectMilliseconds() {
        // When
        String result = HumanReadableFormatsUtils.formatHmsMillisFromNanos(1_000_000L);

        // Then
        assertThat(result).isEqualTo("0:00:00.001");
    }

    @Test
    void formatHmsMillisFromNanos_negativeValue_throwsIllegalArgumentException() {
        // When / Then
        assertThatThrownBy(() -> HumanReadableFormatsUtils.formatHmsMillisFromNanos(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }
}
