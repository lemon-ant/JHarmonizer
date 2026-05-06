// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.NonNull;
import lombok.Value;

/**
 * Name constraint: either EXACT or REGEX. The value stores the original string.
 * Regex compilation is deferred to the compilation phase.
 */
@Value
public class UnifiedNameMatcher {
    @NonNull
    UnifiedMatchMethod matchMethod;
    /**
     * Raw exact string or raw regex pattern (as provided in config).
     */
    @NonNull
    String value;
}
