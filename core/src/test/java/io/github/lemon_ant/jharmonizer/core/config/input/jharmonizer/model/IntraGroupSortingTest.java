package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IntraGroupSortingTest {

    @Test
    void fromString_validLowercaseValue_returnsParsedEnum() {
        // When
        IntraGroupSorting intraGroupSorting = IntraGroupSorting.fromString("alpha");

        // Then
        assertThat(intraGroupSorting).isEqualTo(IntraGroupSorting.ALPHA);
    }

    @Test
    void fromString_validMixedCaseValue_returnsParsedEnum() {
        // When
        IntraGroupSorting intraGroupSorting = IntraGroupSorting.fromString("Preserve");

        // Then
        assertThat(intraGroupSorting).isEqualTo(IntraGroupSorting.PRESERVE);
    }

    @Test
    void fromString_invalidValue_throwsIllegalArgumentException() {
        // When / Then
        assertThatThrownBy(() -> IntraGroupSorting.fromString("unknown_sort"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromString_allEnumValues_parsedSuccessfully() {
        // When / Then
        for (IntraGroupSorting intraGroupSorting : IntraGroupSorting.values()) {
            assertThat(IntraGroupSorting.fromString(intraGroupSorting.name().toLowerCase()))
                    .isEqualTo(intraGroupSorting);
        }
    }
}
