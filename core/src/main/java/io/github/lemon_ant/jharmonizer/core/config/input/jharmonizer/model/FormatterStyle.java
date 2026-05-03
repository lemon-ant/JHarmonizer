// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Formatter style choices supported in the JHarmonizer YAML config.
 * Each constant maps to a corresponding {@link UnifiedFormatterStyle}.
 */
@Getter
@RequiredArgsConstructor
public enum FormatterStyle {
    AOP(UnifiedFormatterStyle.AOSP),
    GOOGLE(UnifiedFormatterStyle.GOOGLE),
    NONE(UnifiedFormatterStyle.NONE),
    PALANTIR(UnifiedFormatterStyle.PALANTIR),
    ;

    private final UnifiedFormatterStyle unifiedFormatterStyle;

    /**
     * Performs the from string.
     * @param value the raw value to parse
     * @return the result
     */
    @NonNull
    @JsonCreator
    static FormatterStyle fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(FormatterStyle.class, value);
    }
}
