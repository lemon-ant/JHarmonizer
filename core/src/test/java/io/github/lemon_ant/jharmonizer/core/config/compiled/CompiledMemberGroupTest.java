package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompiledMemberGroupTest {

    @Test
    void build_nameMissing_throwsNullPointerException() {
        // When / Then
        assertThatThrownBy(() -> CompiledMemberGroup.builder()
                        .compiledSubGroups(List.of())
                        .keepAccessorsTogether(false)
                        .orderIndex(0)
                        .selectorBlock(new CompiledMemberGroupSelectorBlock(List.of(), List.of()))
                        .separator(UnifiedSeparator.NONE)
                        .orderingRule(OrderingRule.PRESERVE)
                        .build())
                .isInstanceOf(NullPointerException.class);
    }
}
