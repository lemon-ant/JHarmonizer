package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

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
            @NonNull UnifiedMemberGroupSelectorBlock selectorBlock,
            @Nullable UnifiedSeparator separator,
            @Nullable List<@NonNull UnifiedOrderingRule> orderingRules) {
        this.groupName = groupName;
        this.keepAccessorsTogether = keepAccessorsTogether;
        this.memberSubGroups = memberSubGroups;
        this.selectorBlock = selectorBlock;
        this.separator = separator;
        this.orderingRules = orderingRules == null ? null : Collections.unmodifiableList(orderingRules);
    }
}
