// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Group} and {@link Groups} factory methods.
 */
class GroupTest {

    @Test
    void of_nullItems_throwsNullPointerException() {
        // When
        Throwable thrown = catchThrowable(() -> Group.of((String[]) null));

        // Then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Nested
    class GroupsOfTests {

        @Test
        void of_nullGroups_throwsNullPointerException() {
            // Given
            Group<String>[] nullGroups = null;

            // When
            Throwable thrown = catchThrowable(() -> Groups.of(nullGroups));

            // Then
            assertThat(thrown).isInstanceOf(NullPointerException.class);
        }
    }
}
