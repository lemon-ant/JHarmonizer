package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior.UnifiedSortKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerSortKey {
    ALPHA(UnifiedSortingBehavior.UnifiedSortKey.ALPHA),
    PRESERVE(UnifiedSortingBehavior.UnifiedSortKey.PRESERVE),
    SIGNATURE(UnifiedSortingBehavior.UnifiedSortKey.SIGNATURE),
    VISIBILITY_ASC(UnifiedSortingBehavior.UnifiedSortKey.VISIBILITY_ASC),
    VISIBILITY_DESC(UnifiedSortingBehavior.UnifiedSortKey.VISIBILITY_DESC),
    ;

    private final UnifiedSortKey unifiedSortKey;
}
