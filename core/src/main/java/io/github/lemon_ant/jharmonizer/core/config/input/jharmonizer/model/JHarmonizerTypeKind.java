/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Java type-kind values supported in the JHarmonizer YAML config.
 * Each constant maps to a corresponding {@link UnifiedTypeKind}.
 */
@Getter
@RequiredArgsConstructor
public enum JHarmonizerTypeKind {
    CLASS(UnifiedTypeKind.CLASS),
    INTERFACE(UnifiedTypeKind.INTERFACE),
    ENUM(UnifiedTypeKind.ENUM),
    ANNOTATION(UnifiedTypeKind.ANNOTATION),
    RECORD(UnifiedTypeKind.RECORD),
    ;

    private final UnifiedTypeKind unifiedTypeKind;
}
