package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnifiedAnnotationMatcherTest {

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        UnifiedAnnotationMatcher matcher = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");

        // When / Then
        assertThat(matcher).isEqualTo(matcher);
    }

    @Test
    void equals_nonAnnotationMatcherObject_returnsFalse() {
        // Given
        UnifiedAnnotationMatcher matcher = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");

        // When / Then
        assertThat(matcher).isNotEqualTo("not a matcher");
    }

    @Test
    void equals_sameMethodAndValue_returnsTrue() {
        // Given
        UnifiedAnnotationMatcher first = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");
        UnifiedAnnotationMatcher second = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentMatchMethod_returnsFalse() {
        // Given
        UnifiedAnnotationMatcher first = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");
        UnifiedAnnotationMatcher second = new UnifiedAnnotationMatcher(UnifiedMatchMethod.REGEX, "Override");

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        // Given
        UnifiedAnnotationMatcher first = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");
        UnifiedAnnotationMatcher second = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Deprecated");

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        UnifiedAnnotationMatcher first = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");
        UnifiedAnnotationMatcher second = new UnifiedAnnotationMatcher(UnifiedMatchMethod.EXACT, "Override");

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
