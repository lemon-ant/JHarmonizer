// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

/** Criteria applied to annotations in priority order; an empty rule list preserves source order. */
public enum UnifiedAnnotationOrderingRule {
    ALPHA,
    ARGUMENTS_ALPHA,
    DECLARATION_LENGTH_ASC,
    NAME_LENGTH_ASC,
}
