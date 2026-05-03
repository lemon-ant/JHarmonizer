// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

/**
 * String-matching strategy used when evaluating member-selector patterns.
 * {@code EXACT} requires an exact string match; {@code REGEX} treats the pattern as a regular expression.
 */
public enum UnifiedMatchMethod {
    EXACT,
    REGEX
}
