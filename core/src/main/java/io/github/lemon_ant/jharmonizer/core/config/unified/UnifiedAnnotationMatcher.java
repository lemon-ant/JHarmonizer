// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
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
