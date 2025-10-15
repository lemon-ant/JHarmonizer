package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortKey {
    ALPHA(UnifiedSortKey.ALPHA),
    PRESERVE(UnifiedSortKey.PRESERVE),
    SIGNATURE(UnifiedSortKey.SIGNATURE),
    VISIBILITY_ASC(UnifiedSortKey.VISIBILITY_ASC),
    VISIBILITY_DESC(UnifiedSortKey.VISIBILITY_DESC),
    ;

    private final UnifiedSortKey unifiedSortKey;
}
