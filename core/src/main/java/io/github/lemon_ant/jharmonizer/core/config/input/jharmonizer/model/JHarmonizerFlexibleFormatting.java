// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Partial formatting overlay for a JHarmonizer flexible YAML config.
 * All fields are optional; at least one must be set.
 * Deserialized from the {@code formatting} section of a flexible config file.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class JHarmonizerFlexibleFormatting {

    @Nullable
    Boolean blankLineAfterTypeHeader;

    @Nullable
    Boolean blankLineBeforeComment;

    @Nullable
    Boolean blankLineBetweenFields;

    @Nullable
    Boolean fixImports;

    @Nullable
    FormatterStyle formatterStyle;

    /**
     * Creates a partial formatting overlay. At least one parameter must be non-null.
     *
     * @param fixImports optional override for fix-imports
     * @param formatterStyle optional override for formatter style
     * @param blankLineAfterTypeHeader optional override for blank line after type header
     * @param blankLineBeforeComment optional override for blank line before comment
     * @param blankLineBetweenFields optional override for blank line between fields
     */
    // CPD-OFF: This vendor input model intentionally mirrors FlexibleUnifiedFormatting's structure.
    // The two classes exist at different pipeline layers (Jackson deserialization vs. unified internal
    // representation) and cannot share a common base without breaking @Value / @Builder constraints.
    // Remove this suppression once the two types are consolidated or a shared abstraction is introduced.
    public JHarmonizerFlexibleFormatting(
            @Nullable @JsonProperty("fix-imports") Boolean fixImports,
            @Nullable @JsonProperty("formatter-style") FormatterStyle formatterStyle,
            @Nullable @JsonProperty("blank-line-after-type-header") Boolean blankLineAfterTypeHeader,
            @Nullable @JsonProperty("blank-line-before-comment") Boolean blankLineBeforeComment,
            @Nullable @JsonProperty("blank-line-between-fields") Boolean blankLineBetweenFields) {
        Validate.isTrue(
                fixImports != null
                        || formatterStyle != null
                        || blankLineAfterTypeHeader != null
                        || blankLineBeforeComment != null
                        || blankLineBetweenFields != null,
                "At least one formatting parameter must be specified in JHarmonizerFlexibleFormatting");
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.blankLineAfterTypeHeader = blankLineAfterTypeHeader;
        this.blankLineBeforeComment = blankLineBeforeComment;
        this.blankLineBetweenFields = blankLineBetweenFields;
    }

    /**
     * Returns the optional blank-line-after-type-header override.
     *
     * @return the optional blank-line-after-type-header override
     */
    @NonNull
    public Optional<Boolean> getBlankLineAfterTypeHeader() {
        return ofNullable(blankLineAfterTypeHeader);
    }

    /**
     * Returns the optional blank-line-before-comment override.
     *
     * @return the optional blank-line-before-comment override
     */
    @NonNull
    public Optional<Boolean> getBlankLineBeforeComment() {
        return ofNullable(blankLineBeforeComment);
    }

    /**
     * Returns the optional blank-line-between-fields override.
     *
     * @return the optional blank-line-between-fields override
     */
    @NonNull
    public Optional<Boolean> getBlankLineBetweenFields() {
        return ofNullable(blankLineBetweenFields);
    }

    /**
     * Returns the optional fix-imports override.
     *
     * @return the optional fix-imports override
     */
    @NonNull
    public Optional<Boolean> getFixImports() {
        return ofNullable(fixImports);
    }

    // CPD-ON

    /**
     * Returns the optional formatter style override.
     *
     * @return the optional formatter style override
     */
    @NonNull
    public Optional<FormatterStyle> getFormatterStyle() {
        return ofNullable(formatterStyle);
    }
}
