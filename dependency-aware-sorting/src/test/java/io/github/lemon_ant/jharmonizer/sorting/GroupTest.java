package io.github.lemon_ant.jharmonizer.sorting;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Group} and {@link Groups} factory methods.
 */
class GroupTest {

    @Test
    void of_nullItems_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> Group.of((String[]) null)).isInstanceOf(NullPointerException.class);
    }

    @Nested
    class GroupsOfTests {

        @Test
        void of_nullGroups_throwsNullPointerException() {
            // Given
            Group<String>[] nullGroups = null;

            // When / Then
            assertThatThrownBy(() -> Groups.of(nullGroups)).isInstanceOf(NullPointerException.class);
        }
    }
}
