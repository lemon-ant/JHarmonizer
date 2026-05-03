/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.NonNull;
import lombok.Value;

/**
 * Annotation constraint. Matches by simple name or fully-qualified name, using EXACT or REGEX.
 * All-of semantics are intentionally omitted for v1 (kept for future extension).
 */
@Value
public class UnifiedAnnotationMatcher {

    @NonNull
    UnifiedMatchMethod matchMethod; // EXACT or REGEX

    @NonNull
    String value; // exact value or regex pattern
}
