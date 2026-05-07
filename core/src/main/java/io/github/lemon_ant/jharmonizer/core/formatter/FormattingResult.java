// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.formatter;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of formatting one source file.
 * Bundles the formatted source code string with the associated timing and size statistics.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FormattingResult {
    @NonNull
    String formattedSrcCode;

    @NonNull
    FormattingStatistic formattingStatistic;
}
