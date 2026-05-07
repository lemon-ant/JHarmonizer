// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import lombok.Value;

/**
 * Unified descriptor for the header-line separator rendered between member groups.
 * Specifies the character repeated across the separator line and the left-padding width.
 */
@Value
public class UnifiedHeaderLine {
    char character;
    int leftPadding;
}
