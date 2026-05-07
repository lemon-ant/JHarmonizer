// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.formatter;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Timing and size statistics collected during a single source-file formatting pass.
 */
@Value
@AllArgsConstructor
public class FormattingStatistic {
    long formattedCodeLength;
    long formattingTimeInNanos;
}
