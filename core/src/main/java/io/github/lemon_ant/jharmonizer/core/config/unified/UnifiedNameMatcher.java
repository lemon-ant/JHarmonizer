// =====================================================================================
// FILE: UnifiedNameMatcher.java
// =====================================================================================
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
    UnifiedNameMatchKind kind;
    /**
     * Raw exact string or raw regex pattern (as provided in config).
     */
    @NonNull
    String value;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedNameMatcher that)) {
            return false;
        }

        return kind == that.kind && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
