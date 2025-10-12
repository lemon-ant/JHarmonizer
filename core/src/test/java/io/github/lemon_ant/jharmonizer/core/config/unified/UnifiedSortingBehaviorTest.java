package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnifiedSortingBehaviorTest {

    @Test
    void sortingBehavior_shouldHoldKeysAndAccessorFlag() {
        UnifiedSortingBehavior behavior = UnifiedSortingBehavior.builder()
                .unifiedSortKeys(List.of(UnifiedSortKey.BY_VISIBILITY, UnifiedSortKey.BY_NAME))
                .keepAccessorsTogether(true)
                .build();

        assertThat(behavior.getUnifiedSortKeys()).containsExactly(UnifiedSortKey.BY_VISIBILITY, UnifiedSortKey.BY_NAME);
        assertThat(behavior.isKeepAccessorsTogether()).isTrue();
    }
}
