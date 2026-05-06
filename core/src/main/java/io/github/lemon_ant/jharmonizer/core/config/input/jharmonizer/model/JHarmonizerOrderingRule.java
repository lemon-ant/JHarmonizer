// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ordering rule values supported in the JHarmonizer YAML config.
 * Each constant maps to a corresponding {@link UnifiedOrderingRule}.
 */
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
