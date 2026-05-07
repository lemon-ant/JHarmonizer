// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.formatter;

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
