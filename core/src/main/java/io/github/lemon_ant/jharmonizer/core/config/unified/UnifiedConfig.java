// =====================================================================================
// FILE: UnifiedConfig.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Unified, strongly-typed configuration model prepared for compilation into the effective model.
 * <p>
 * This model is a normalized, vendor-independent representation assembled from any vendor-specific
 * inputs (IDE exports, YAML/XML, proprietary configs). It mirrors the MemberDescriptor capabilities
 * on the configuration side (kinds, access, modifiers, names, annotations, behaviors) and is
 * deliberately strict and explicit.
 */
@Value
@SuppressFBWarnings
public class UnifiedConfig {

    /**
     * Sequence of root group nodes. DFS classification will walk these in-order.
     */
    @NonNull
    List<UnifiedMemberGroup> rootMemberGroups;

    @Builder
    public UnifiedConfig(@NonNull @Singular List<UnifiedMemberGroup> rootMemberGroups) {
        this.rootMemberGroups = Collections.unmodifiableList(rootMemberGroups);
    }
}
