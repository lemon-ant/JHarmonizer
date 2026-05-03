/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Unified Java type-kind values used when ordering top-level types.
 * Each constant maps to the corresponding {@link MemberKind} for use in selectors.
 */
@Getter
@RequiredArgsConstructor
public enum UnifiedTypeKind {
    CLASS(MemberKind.TYPE_CLASS),
    INTERFACE(MemberKind.TYPE_INTERFACE),
    ENUM(MemberKind.TYPE_ENUM),
    ANNOTATION(MemberKind.TYPE_ANNOTATION),
    RECORD(MemberKind.TYPE_RECORD),
    ;

    private final MemberKind memberKind;
}
