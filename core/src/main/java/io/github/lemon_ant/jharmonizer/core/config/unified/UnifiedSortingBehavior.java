// =====================================================================================
// FILE: UnifiedSortingBehavior.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Sorting behavior hints for a group. These are high-level knobs that the compiler will translate
 * into concrete comparators and flags inside the effective model.
 */
@Value
@Builder
public class UnifiedSortingBehavior {

    /**
     * Optional explicit access ordering for this group (overrides global default if present).
     */
    @NonNull
    @Singular
    List<UnifiedSortKey> unifiedSortKeys;

    /**
     * Keep getter/setter pairs together where applicable.
     */
    boolean keepAccessorsTogether;
}
