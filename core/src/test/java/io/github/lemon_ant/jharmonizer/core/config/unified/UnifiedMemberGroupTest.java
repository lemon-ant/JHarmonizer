package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnifiedMemberGroupTest {

    @Test
    void build_groupNameMissing_throwsNullPointerException() {
        // Given / When / Then
        assertThatThrownBy(() -> UnifiedMemberGroup.builder()
                        .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                        .separator(UnifiedSeparator.NONE)
                        .orderingRule(UnifiedOrderingRule.ALPHA)
                        .build())
                .isInstanceOf(NullPointerException.class);
    }
}
