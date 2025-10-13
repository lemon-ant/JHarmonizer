package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnifiedSortingBehaviorTest {

    @Test
    void buildSortingBehavior_validProperties_containsExpectedSortKeysAndAccessorsFlag() {
        UnifiedSortingBehavior behavior = UnifiedSortingBehavior.builder()
                .unifiedSortKeys(List.of(UnifiedSortKey.VISIBILITY_ASC, UnifiedSortKey.ALPHA))
                .keepAccessorsTogether(true)
                .build();

        assertThat(behavior.getUnifiedSortKeys()).containsExactly(UnifiedSortKey.VISIBILITY_ASC, UnifiedSortKey.ALPHA);
        assertThat(behavior.isKeepAccessorsTogether()).isTrue();
    }
}
