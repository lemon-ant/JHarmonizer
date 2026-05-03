/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.NonNull;
import lombok.Value;

/**
 * A cohesive bundle of formatting-related options, including printer flags.
 * Kept as a separate value object to make semantic grouping explicit.
 */
@Value
public class UnifiedFormatting {

    /**
     * Whether to fix/reorder imports in the final formatting pass.
     */
    boolean fixImports;

    /**
     * Formatter style hint (PALANTIR is our default contract).
     */
    @NonNull
    UnifiedFormatterStyle formatterStyle;

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
