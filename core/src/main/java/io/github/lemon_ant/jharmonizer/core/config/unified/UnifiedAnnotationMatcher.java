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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedAnnotationMatcher that)) {
            return false;
        }

        return matchMethod == that.matchMethod && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = matchMethod.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
