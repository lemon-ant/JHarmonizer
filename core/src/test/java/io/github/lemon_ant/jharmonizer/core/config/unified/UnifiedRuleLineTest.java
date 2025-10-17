package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for UnifiedRuleLine.Builder custom setter behavior (nameMatcher single-assignment guard).
 */
class UnifiedRuleLineBuilderTest {

    @Test
    void nameMatcher_secondAssignment_throwsIllegalStateException() {
        // given
        UnifiedRuleLine.UnifiedRuleLineBuilder builder = UnifiedRuleLine.builder();

        // when
        builder.nameMatcher(null); // first assignment is allowed (null means "no constraint")

        // then
        assertThatThrownBy(() -> builder.nameMatcher(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been assigned");
    }

    @Test
    void nameMatcher_assignedOnce_buildsSuccessfully() {
        // given
        UnifiedRuleLine.UnifiedRuleLineBuilder builder = UnifiedRuleLine.builder();

        // when
        builder.nameMatcher(null); // single assignment is fine
        UnifiedRuleLine unifiedRuleLine = builder.build();

        // then
        assertThat(unifiedRuleLine).isNotNull();
        assertThat(unifiedRuleLine.getNameMatcher()).isNull(); // explicit null preserved
    }

    @Test
    void build_withoutNameMatcher_isAllowed() {
        // given
        UnifiedRuleLine.UnifiedRuleLineBuilder builder = UnifiedRuleLine.builder();

        // when
        UnifiedRuleLine unifiedRuleLine = builder.build();

        // then
        assertThat(unifiedRuleLine).isNotNull();
        assertThat(unifiedRuleLine.getNameMatcher()).isNull(); // absent -> null by contract
    }
}
