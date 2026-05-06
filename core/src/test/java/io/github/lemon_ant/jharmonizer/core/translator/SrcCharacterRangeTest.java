// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class SrcCharacterRangeTest {

    @Test
    void constructor_validRange_createsRangeSuccessfully() {
        // When
        SrcCharacterRange srcCharacterRange = new SrcCharacterRange(0, 10);

        // Then
        assertThat(srcCharacterRange.getStartInclusive()).isZero();
        assertThat(srcCharacterRange.getEndExclusive()).isEqualTo(10);
    }

    @Test
    void constructor_negativeStartInclusive_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> new SrcCharacterRange(-1, 5));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-negative");
    }

    @Test
    void constructor_endLessThanStart_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> new SrcCharacterRange(10, 5));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(">=");
    }
}
