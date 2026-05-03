/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class UnifiedMemberGroupRuleLineBuilderTest {

    @Test
    void build_selectorsMissing_illegalArgumentExceptionThrown() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder unifiedMemberGroupRuleLineBuilder =
                UnifiedMemberGroupRuleLine.builder();

        // When
        Throwable thrown = catchThrowable(unifiedMemberGroupRuleLineBuilder::build);

        // Then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one selector");
    }

    @Test
    void build_nameMatcherAssignedOnce_ruleLineBuilt() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder unifiedMemberGroupRuleLineBuilder =
                UnifiedMemberGroupRuleLine.builder();
        UnifiedNameMatcher expectedUnifiedNameMatcher = new UnifiedNameMatcher(UnifiedMatchMethod.EXACT, "methodName");

        // When
        unifiedMemberGroupRuleLineBuilder.nameMatcher(expectedUnifiedNameMatcher);
        UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine = unifiedMemberGroupRuleLineBuilder.build();

        // Then
        assertThat(unifiedMemberGroupRuleLine).isNotNull();
        assertThat(unifiedMemberGroupRuleLine.getNameMatcher()).isSameAs(expectedUnifiedNameMatcher);
    }

    @Test
    void nameMatcher_secondAssignment_illegalStateExceptionThrown() {
        // Given
        UnifiedMemberGroupRuleLine.UnifiedMemberGroupRuleLineBuilder unifiedMemberGroupRuleLineBuilder =
                UnifiedMemberGroupRuleLine.builder();
        unifiedMemberGroupRuleLineBuilder.nameMatcher(null);

        // When
        Throwable thrown = catchThrowable(() -> unifiedMemberGroupRuleLineBuilder.nameMatcher(null));

        // Then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("already been assigned");
    }
}
