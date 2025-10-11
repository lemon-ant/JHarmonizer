// =====================================================================================
// FILE: AnnotationMatcher.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.NonNull;
import lombok.Value;

/**
 * Annotation constraint. Matches by simple name or fully-qualified name, using EXACT or REGEX.
 * All-of semantics are intentionally omitted for v1 (kept for future extension).
 */
@Value
public class AnnotationMatcher {

    @NonNull
    NameMatchKind nameMatchKind; // EXACT or REGEX

    @NonNull
    String value; // exact value or regex pattern

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnnotationMatcher that)) {
            return false;
        }

        return nameMatchKind == that.nameMatchKind && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = nameMatchKind.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
