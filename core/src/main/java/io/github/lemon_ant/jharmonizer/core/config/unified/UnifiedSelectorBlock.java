// =====================================================================================
// FILE: UnifiedSelectorBlock.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Includes and excludes are lists of rule lines with OR semantics inside each list:
 * - includes: empty ⇒ true (matches everything)
 * - excludes: empty ⇒ false (matches nothing)
 * <p>
 * Final acceptance (in this node) is: any(includeLines) AND NOT any(excludeLines).
 */
@Value
@Builder
public class UnifiedSelectorBlock {

    @NonNull
    @Singular
    Set<UnifiedRuleLine> includes;

    @NonNull
    @Singular
    Set<UnifiedRuleLine> excludes;
}
