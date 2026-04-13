package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnifiedNameMatcherTest {

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        UnifiedNameMatcher matcher = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");

        // When / Then
        assertThat(matcher).isEqualTo(matcher);
    }

    @Test
    void equals_nonNameMatcherObject_returnsFalse() {
        // Given
        UnifiedNameMatcher matcher = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");

        // When / Then
        assertThat(matcher).isNotEqualTo("not a name matcher");
    }

    @Test
    void equals_sameMethodAndValue_returnsTrue() {
        // Given
        UnifiedNameMatcher first = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");
        UnifiedNameMatcher second = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentMatchMethod_returnsFalse() {
        // Given
        UnifiedNameMatcher first = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");
        UnifiedNameMatcher second = new UnifiedNameMatcher(UnifiedMatchMethod.REGEX, "myField");

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        // Given
        UnifiedNameMatcher first = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");
        UnifiedNameMatcher second = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "otherField");

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        UnifiedNameMatcher first = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");
        UnifiedNameMatcher second = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "myField");

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
