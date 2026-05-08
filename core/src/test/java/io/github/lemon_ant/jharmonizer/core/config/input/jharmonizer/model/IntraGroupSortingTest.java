// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class IntraGroupSortingTest {

    @Test
    void fromString_unknownValue_throwsIllegalArgumentException() {
        // When
        Throwable thrown = catchThrowable(() -> IntraGroupSorting.fromString("unsupported_mode"));

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromString_validLowercaseName_returnsMatchingEnum() {
        // When
        IntraGroupSorting intraGroupSorting = IntraGroupSorting.fromString("alpha");

        // Then
        assertThat(intraGroupSorting).isEqualTo(IntraGroupSorting.ALPHA);
    }

    @Test
    void fromString_validMixedCaseName_returnsMatchingEnum() {
        // When
        IntraGroupSorting intraGroupSorting = IntraGroupSorting.fromString("Visibility_Asc");

        // Then
        assertThat(intraGroupSorting).isEqualTo(IntraGroupSorting.VISIBILITY_ASC);
    }
}
