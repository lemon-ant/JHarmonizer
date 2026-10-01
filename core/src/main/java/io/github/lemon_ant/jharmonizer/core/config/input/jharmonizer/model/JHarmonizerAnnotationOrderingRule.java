// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationOrderingRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/** Annotation ordering criteria accepted in YAML. */
@Getter
@RequiredArgsConstructor
public enum JHarmonizerAnnotationOrderingRule {
    ALPHA(UnifiedAnnotationOrderingRule.ALPHA),
    NAME_LENGTH_ASC(UnifiedAnnotationOrderingRule.NAME_LENGTH_ASC),
    DECLARATION_LENGTH_ASC(UnifiedAnnotationOrderingRule.DECLARATION_LENGTH_ASC),
    ARGUMENTS_ALPHA(UnifiedAnnotationOrderingRule.ARGUMENTS_ALPHA);

    @NonNull
    private final UnifiedAnnotationOrderingRule unifiedOrderingRule;

    /**
     * Parses a case-insensitive, hyphenated ordering criterion.
     * @param value configured criterion
     * @return matching criterion
     */
    @JsonCreator
    @NonNull
    public static JHarmonizerAnnotationOrderingRule parse(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(JHarmonizerAnnotationOrderingRule.class, value);
    }
}
