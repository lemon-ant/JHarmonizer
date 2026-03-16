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

    /**
     * Checks whether this unified name matcher matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedNameMatcher that)) {
            return false;
        }

        return matchMethod == that.matchMethod && value.equals(that.value);
    }

    /**
     * Returns the hash code of this unified name matcher.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = matchMethod.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
