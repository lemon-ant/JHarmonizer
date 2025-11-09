package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Sorting behavior hints for a group. These are high-level knobs that the compiler will translate
 * into concrete comparators and flags inside the Compiled model.
 */
@Value
public class UnifiedSortingBehavior {

    /**
     * Keep getter/setter pairs together where applicable.
     */
    boolean keepAccessorsTogether;
    /**
     * Optional explicit access ordering for this group (overrides global default if present).
     */
    @NonNull
    List<UnifiedSortKey> unifiedSortKeys;

    @Builder
    public UnifiedSortingBehavior(
            @NonNull @Singular List<UnifiedSortKey> unifiedSortKeys, boolean keepAccessorsTogether) {
        Validate.notEmpty(unifiedSortKeys, "Sort keys collection cannot be empty");
        this.unifiedSortKeys = Collections.unmodifiableList(unifiedSortKeys);
        this.keepAccessorsTogether = keepAccessorsTogether;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedSortingBehavior that)) {
            return false;
        }

        return keepAccessorsTogether == that.keepAccessorsTogether && unifiedSortKeys.equals(that.unifiedSortKeys);
    }

    @Override
    public int hashCode() {
        int result = unifiedSortKeys.hashCode();
        result = 31 * result + Boolean.hashCode(keepAccessorsTogether);
        return result;
    }

    public enum UnifiedSortKey {
        ALPHA,
        PRESERVE,
        SIGNATURE,
        VISIBILITY_ASC,
        VISIBILITY_DESC,
    }
}
