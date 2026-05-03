/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Dependencies} factory methods.
 */
class DependenciesTest {

    @Test
    void of_nullPairs_throwsNullPointerException() {
        // When
        Throwable thrown = catchThrowable(() -> Dependencies.of((String[]) null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_oddNumberOfArguments_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> Dependencies.of("a", "b", "c"));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("even");
    }
}
