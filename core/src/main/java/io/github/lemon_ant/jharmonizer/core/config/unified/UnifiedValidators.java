// =====================================================================================
// FILE: UnifiedValidators.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * Small validation helpers for the unified configuration layer.
 * Use these at parse/convert time to fail fast on broken configuration
 * before compiling into the effective model.
 */
public final class UnifiedValidators {
    private UnifiedValidators() {}

    /**
     * Validate that a regex compiles. Throws IllegalArgumentException if invalid.
     */
    public static void validateRegex(@NonNull String pattern) {
        Pattern.compile(pattern); // compile and discard to verify correctness
    }
}
