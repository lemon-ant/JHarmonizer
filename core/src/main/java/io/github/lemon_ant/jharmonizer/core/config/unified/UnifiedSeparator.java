/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

/**
 * Unified separator style for member groups.
 * {@code NEW_LINE} inserts a blank line, {@code HEADER} inserts a header-line comment,
 * and {@code NONE} inserts no separator.
 */
public enum UnifiedSeparator {
    NEW_LINE,
    HEADER,
    NONE,
}
