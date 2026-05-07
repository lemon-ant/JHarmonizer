// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class HumanReadableFormatsUtilsTest {

    @Test
    void formatBytes_largeKibValue_returnsKibWithoutDecimal() {
        // Given
        long twentyKib = 20 * 1024L;

        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(twentyKib);

        // Then
        assertThat(formatted).endsWith(" KiB");
        assertThat(formatted).doesNotContain(".");
    }

    @Test
    void formatBytes_largeMibValue_returnsMibWithoutDecimal() {
        // Given
        long fiftyMib = 50 * 1024L * 1024L;

        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(fiftyMib);

        // Then
        assertThat(formatted).endsWith(" MiB");
        assertThat(formatted).doesNotContain(".");
    }

    @Test
    void formatBytes_negativeInput_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> HumanReadableFormatsUtils.formatBytes(-1));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
    }

    @Test
    void formatBytes_valueInGibRange_returnsGibSuffix() {
        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(2L * 1024L * 1024L * 1024L);

        // Then
        assertThat(formatted).endsWith(" GiB");
    }

    @Test
    void formatBytes_valueInKibRange_returnsKibSuffix() {
        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(2048);

        // Then
        assertThat(formatted).endsWith(" KiB");
    }

    @Test
    void formatBytes_valueInMibRange_returnsMibSuffix() {
        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(5 * 1024L * 1024L);

        // Then
        assertThat(formatted).endsWith(" MiB");
    }

    @Test
    void formatBytes_valueUnderKib_returnsByteSuffix() {
        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(512);

        // Then
        assertThat(formatted).endsWith(" B");
    }

    @Test
    void formatBytes_zeroBytesInput_returnsZeroWithByteSuffix() {
        // When
        String formatted = HumanReadableFormatsUtils.formatBytes(0);

        // Then
        assertThat(formatted).isEqualTo("0 B");
    }

    @Test
    void formatHmsMillisFromNanos_negativeInput_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> HumanReadableFormatsUtils.formatHmsMillisFromNanos(-1));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
    }

    @Test
    void formatHmsMillisFromNanos_oneHourInput_returnsOneHour() {
        // Given
        long oneHourNanos = 3_600_000_000_000L;

        // When
        String formatted = HumanReadableFormatsUtils.formatHmsMillisFromNanos(oneHourNanos);

        // Then
        assertThat(formatted).isEqualTo("1:00:00.000");
    }

    @Test
    void formatHmsMillisFromNanos_zeroInput_returnsZeroHms() {
        // When
        String formatted = HumanReadableFormatsUtils.formatHmsMillisFromNanos(0);

        // Then
        assertThat(formatted).isEqualTo("0:00:00.000");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_negativeInput_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(-1));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
    }

    @Test
    void formatSecondsMicrosecondsFromNanos_zeroInput_returnsZeroSeconds() {
        // When
        String formatted = HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos(0);

        // Then
        assertThat(formatted).isEqualTo("0.000");
    }
}
