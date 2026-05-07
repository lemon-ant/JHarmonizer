// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import lombok.Value;

/**
 * Immutable configuration controlling blank-line insertion by the source printer.
 * Instances are created once from the compiled configuration and passed to the printer.
 */
@Value
public class PrinterConfig {

    /**
     * Whether to insert a blank line after the type declaration header, before the first member.
     */
    boolean blankLineAfterTypeHeader;

    /**
     * Whether to insert a blank line before members with leading comments.
     */
    boolean blankLineBeforeComment;

    /**
     * Whether to insert a blank line between consecutive field declarations.
     */
    boolean blankLineBetweenFields;
}
