// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Partial formatting overlay. All fields are optional; at least one must be set.
 * Use {@link UnifiedConfigMerger} to apply this overlay over a strict {@link UnifiedFormatting} baseline.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class FlexibleUnifiedFormatting {

    /**
     * Optional override for whether to fix/reorder imports in the final formatting pass.
     */
    @Nullable
    Boolean fixImports;

    /**
     * Optional override for the formatter style hint.
     */
    @Nullable
    UnifiedFormatterStyle formatterStyle;

    /**
     * Optional override for whether to insert a blank line after the type declaration header.
     */
    @Nullable
    Boolean blankLineAfterTypeHeader;

    /**
     * Optional override for whether to insert a blank line before members with leading comments.
     */
    @Nullable
    Boolean blankLineBeforeComment;

    /**
     * Optional override for whether to insert a blank line between consecutive field declarations.
     */
    @Nullable
    Boolean blankLineBetweenFields;

    /**
     * Creates a partial formatting overlay. At least one parameter must be non-null.
     *
     * @param fixImports optional override for fix-imports
     * @param formatterStyle optional override for formatter style
     * @param blankLineAfterTypeHeader optional override for blank line after type header
     * @param blankLineBeforeComment optional override for blank line before comment
     * @param blankLineBetweenFields optional override for blank line between fields
     */
    @Builder
    private FlexibleUnifiedFormatting(
            @Nullable Boolean fixImports,
            @Nullable UnifiedFormatterStyle formatterStyle,
            @Nullable Boolean blankLineAfterTypeHeader,
            @Nullable Boolean blankLineBeforeComment,
            @Nullable Boolean blankLineBetweenFields) {
        Validate.isTrue(
                fixImports != null
                        || formatterStyle != null
                        || blankLineAfterTypeHeader != null
                        || blankLineBeforeComment != null
                        || blankLineBetweenFields != null,
                "At least one formatting parameter must be specified in FlexibleUnifiedFormatting");
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.blankLineAfterTypeHeader = blankLineAfterTypeHeader;
        this.blankLineBeforeComment = blankLineBeforeComment;
        this.blankLineBetweenFields = blankLineBetweenFields;
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

    /**
     * Returns the optional formatter style override.
     *
     * @return the optional formatter style override
     */
    @NonNull
    public Optional<UnifiedFormatterStyle> getFormatterStyle() {
        return ofNullable(formatterStyle);
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
}
