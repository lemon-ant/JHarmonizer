package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for UnifiedMemberGroupRuleLine.Builder custom setter behavior (nameMatcher single-assignment guard).
 */
class UnifiedMemberGroupRuleLineBuilderTest {

    @Test
    void build_withoutSelectors_isNotAllowed() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();

        // When / Then
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one selector");
    }

    @Test
    void nameMatcher_assignedOnce_buildsSuccessfully() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();
        UnifiedNameMatcher expectedUnifiedNameMatcher = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "methodName");

        // When
        builder.nameMatcher(expectedUnifiedNameMatcher); // single assignment is fine
        UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine = builder.build();

        // Then
        assertThat(unifiedMemberGroupRuleLine).isNotNull();
        assertThat(unifiedMemberGroupRuleLine.getNameMatcher())
                .isNotNull()
                .isSameAs(expectedUnifiedNameMatcher); // explicit null preserved
    }

    @Test
    void nameMatcher_secondAssignment_throwsIllegalStateException() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();

        // When
        builder.nameMatcher(null); // first assignment is allowed (null means "no constraint")

        // Then
        assertThatThrownBy(() -> builder.nameMatcher(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been assigned");
    }
}
