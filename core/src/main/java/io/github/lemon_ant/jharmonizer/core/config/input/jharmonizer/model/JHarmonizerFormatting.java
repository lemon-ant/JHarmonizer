// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

/**
 * Formatting section of a JHarmonizer YAML config, controlling import fixing, formatter style,
 * and printer options.
 */
@Value
public class JHarmonizerFormatting {

    boolean fixImports;

    @NonNull
    FormatterStyle formatterStyle;

    boolean blankLineAfterTypeHeader;

    boolean blankLineBeforeComment;

    boolean blankLineBetweenFields;

    /**
     * Creates a new JHarmonizerFormatting.
     *
     * @param fixImports whether to fix/reorder imports
     * @param formatterStyle the formatter style
     * @param blankLineAfterTypeHeader whether to add a blank line after type header before the first member
     * @param blankLineBeforeComment whether to add a blank line before members with leading comments
     * @param blankLineBetweenFields whether to add a blank line between consecutive field declarations
     */
    public JHarmonizerFormatting(
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle,
            @JsonProperty(value = "blank-line-after-type-header", required = true) boolean blankLineAfterTypeHeader,
            @JsonProperty(value = "blank-line-before-comment", required = true) boolean blankLineBeforeComment,
            @JsonProperty(value = "blank-line-between-fields", required = true) boolean blankLineBetweenFields) {
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.blankLineAfterTypeHeader = blankLineAfterTypeHeader;
        this.blankLineBeforeComment = blankLineBeforeComment;
        this.blankLineBetweenFields = blankLineBetweenFields;
    }
}
