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

    /**
     * Checks whether this unified annotation matcher matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedAnnotationMatcher that)) {
            return false;
        }

        return matchMethod == that.matchMethod && value.equals(that.value);
    }

    /**
     * Returns the hash code of this unified annotation matcher.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = matchMethod.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
