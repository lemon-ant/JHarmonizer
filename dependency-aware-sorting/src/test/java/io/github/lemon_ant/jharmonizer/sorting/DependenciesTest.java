package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Dependencies} factory methods.
 */
class DependenciesTest {

    @Test
    void of_nullPairs_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> Dependencies.of((String[]) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_oddNumberOfArguments_throwsIllegalArgumentException() {
        // When / Then
        assertThatThrownBy(() -> Dependencies.of("a", "b", "c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("even");
    }
}
