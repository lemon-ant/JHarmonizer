package io.github.lemon_ant.jharmonizer.core.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormattingResultTest {

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        FormattingResult formattingResult = buildFormattingResult("class A {}\n", 1000L, 5L);

        // When / Then
        assertThat(formattingResult).isEqualTo(formattingResult);
    }

    @Test
    void equals_nonFormattingResultObject_returnsFalse() {
        // Given
        FormattingResult formattingResult = buildFormattingResult("class A {}\n", 1000L, 5L);

        // When / Then
        assertThat(formattingResult).isNotEqualTo("not a formatting result");
    }

    @Test
    void equals_sameFieldValues_returnsTrue() {
        // Given
        FormattingResult first = buildFormattingResult("class A {}\n", 1000L, 5L);
        FormattingResult second = buildFormattingResult("class A {}\n", 1000L, 5L);

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentSrcCode_returnsFalse() {
        // Given
        FormattingResult first = buildFormattingResult("class A {}\n", 1000L, 5L);
        FormattingResult second = buildFormattingResult("class B {}\n", 1000L, 5L);

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentStatistic_returnsFalse() {
        // Given
        FormattingResult first = buildFormattingResult("class A {}\n", 1000L, 5L);
        FormattingResult second = buildFormattingResult("class A {}\n", 9999L, 5L);

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        FormattingResult first = buildFormattingResult("class A {}\n", 1000L, 5L);
        FormattingResult second = buildFormattingResult("class A {}\n", 1000L, 5L);

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private static FormattingResult buildFormattingResult(
            String formattedSrcCode, long formattedCodeLength, long formattingTimeInNanos) {
        return new FormattingResult(
                formattedSrcCode, new FormattingStatistic(formattedCodeLength, formattingTimeInNanos));
    }
}
