// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * A group node in the classification tree. Children are ordered. Includes/Excludes carry OR semantics across rule lines.
 */
@Value
public class UnifiedMemberGroup {

    /**
     * Stable, human-readable identifier of the group.
     * Nullable by design: overlay configs may be imported from external formats that do not support
     * naming groups, and those unnamed groups are still allowed to be applied on top of the strict baseline config.
     */
    @Nullable
    String groupName;

    /**
     * Keep getter/setter pairs together where applicable.
     */
    @Nullable
    Boolean keepAccessorsTogether;

    /**
     * Ordered list of child groups.
     */
    @NonNull
    List<UnifiedMemberGroup> memberSubGroups;

    /**
     * Whether forward references to fields declared later in source order are ignored for dependency resolution.
     * When {@code true} (default), the tool only considers references to fields above the current member.
     * When {@code false}, all same-type field references create ordering constraints regardless of source position.
     */
    @Nullable
    Boolean relaxedForwardReferences;

    /**
     * Selectors for acceptance logic inside this node.
     */
    @NonNull
    UnifiedMemberGroupSelectorBlock selectorBlock;

    /**
     * Separator directive propagated from vendor config and used at the rendering stage.
     */
    @Nullable
    UnifiedSeparator separator;

    /**
     * Explicit internal members ordering for this group.
     */
    @Nullable
    List<UnifiedOrderingRule> orderingRules;

    @Builder
    private UnifiedMemberGroup(
            @Nullable String groupName,
            @Nullable Boolean keepAccessorsTogether,
            @NonNull @Singular List<@NonNull UnifiedMemberGroup> memberSubGroups,
            @Nullable Boolean relaxedForwardReferences,
            @NonNull UnifiedMemberGroupSelectorBlock selectorBlock,
            @Nullable UnifiedSeparator separator,
            @Nullable List<@NonNull UnifiedOrderingRule> orderingRules) {
        this.groupName = groupName;
        this.keepAccessorsTogether = keepAccessorsTogether;
        this.memberSubGroups = memberSubGroups;
        this.relaxedForwardReferences = relaxedForwardReferences;
        this.selectorBlock = selectorBlock;
        this.separator = separator;
        this.orderingRules =
                ofNullable(orderingRules).map(Collections::unmodifiableList).orElse(null);
    }
}
