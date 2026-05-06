// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnifiedMemberGroupTest {

    @Test
    void build_groupNameMissing_preservesNullName() {
        // When
        UnifiedMemberGroup unifiedMemberGroup = UnifiedMemberGroup.builder()
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRules(List.of(UnifiedOrderingRule.ALPHA))
                .build();

        // Then
        assertThat(unifiedMemberGroup.getGroupName()).isNull();
    }
}
