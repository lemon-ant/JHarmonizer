/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

/**
 * Unified representation of Java member visibility levels,
 * used when evaluating member-group selectors.
 */
public enum MemberAccess {
    PUBLIC,
    PROTECTED,
    PACKAGE,
    PRIVATE,
}
