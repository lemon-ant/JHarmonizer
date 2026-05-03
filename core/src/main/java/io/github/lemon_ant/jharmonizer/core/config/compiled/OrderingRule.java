// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

/**
 * Compiled ordering rule applied when sorting members inside a group.
 * Maps directly from {@link io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule}.
 */
public enum OrderingRule {
    PRESERVE,
    ALPHA,
    VISIBILITY_ASC,
    VISIBILITY_DESC,
}
