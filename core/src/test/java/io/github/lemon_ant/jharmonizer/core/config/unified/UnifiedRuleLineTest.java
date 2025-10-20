package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for UnifiedMemberGroupRuleLine.Builder custom setter behavior (nameMatcher single-assignment guard).
 */
class UnifiedMemberGroupRuleLineBuilderTest {

    @Test
    void build_withoutNameMatcher_isAllowed() {
        // given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();

        // when
        UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine = builder.build();

        // then
        assertThat(unifiedMemberGroupRuleLine).isNotNull();
        assertThat(unifiedMemberGroupRuleLine.getNameMatcher()).isNull(); // absent -> null by contract
    }

    @Test
    void nameMatcher_assignedOnce_buildsSuccessfully() {
        // given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();

        // when
        builder.nameMatcher(null); // single assignment is fine
        UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine = builder.build();

        // then
        assertThat(unifiedMemberGroupRuleLine).isNotNull();
        assertThat(unifiedMemberGroupRuleLine.getNameMatcher()).isNull(); // explicit null preserved
    }

    @Test
    void nameMatcher_secondAssignment_throwsIllegalStateException() {
        // given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder builder = UnifiedMemberGroupRuleLine.builder();

        // when
        builder.nameMatcher(null); // first assignment is allowed (null means "no constraint")

        // then
        assertThatThrownBy(() -> builder.nameMatcher(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been assigned");
    }
}
