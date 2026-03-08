package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerOrderingRule {
    ALPHA(UnifiedOrderingRule.ALPHA),
    PRESERVE(UnifiedOrderingRule.PRESERVE),
    VISIBILITY_ASC(UnifiedOrderingRule.VISIBILITY_ASC),
    VISIBILITY_DESC(UnifiedOrderingRule.VISIBILITY_DESC),
    ;

    private final UnifiedOrderingRule unifiedOrderingRule;
}
