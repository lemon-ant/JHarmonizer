package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnifiedMemberGroupTest {

    @Test
    void build_groupNameMissing_preservesNullName() {
        // When
        UnifiedMemberGroup unifiedMemberGroup = UnifiedMemberGroup.builder()
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRule(UnifiedOrderingRule.ALPHA)
                .build();

        // Then
        assertThat(unifiedMemberGroup.getGroupName()).isNull();
    }
}
