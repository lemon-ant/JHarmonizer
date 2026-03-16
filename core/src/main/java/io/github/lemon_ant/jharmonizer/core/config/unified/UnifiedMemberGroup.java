package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * A group node in the classification tree. Children are ordered. Includes/Excludes carry OR semantics across rule lines.
 */
@Value
public class UnifiedMemberGroup {

    /**
     * Stable, human-readable identifier of the group.
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
    @NonNull
    UnifiedSeparator separator;

    /**
     * Explicit internal members ordering for this group.
     */
    @NonNull
    List<UnifiedOrderingRule> orderingRules;

    @Builder
    private UnifiedMemberGroup(
            @Nullable String groupName,
            @Nullable Boolean keepAccessorsTogether,
            @NonNull @Singular List<@NonNull UnifiedMemberGroup> memberSubGroups,
            @NonNull UnifiedMemberGroupSelectorBlock selectorBlock,
            @NonNull UnifiedSeparator separator,
            @NonNull @Singular List<@NonNull UnifiedOrderingRule> orderingRules) {
        this.groupName = groupName;
        this.keepAccessorsTogether = keepAccessorsTogether;
        this.memberSubGroups = memberSubGroups;
        this.selectorBlock = selectorBlock;
        this.separator = separator;
        Validate.notEmpty(orderingRules, "Ordering rules collection cannot be empty");
        this.orderingRules = Collections.unmodifiableList(orderingRules);
    }

    /**
     * Checks whether this unified member group matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedMemberGroup that)) {
            return false;
        }

        return Objects.equals(groupName, that.groupName)
                && Objects.equals(keepAccessorsTogether, that.keepAccessorsTogether)
                && memberSubGroups.equals(that.memberSubGroups)
                && selectorBlock.equals(that.selectorBlock)
                && separator == that.separator
                && orderingRules.equals(that.orderingRules);
    }

    /**
     * Returns the hash code of this unified member group.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = Objects.hashCode(groupName);
        result = 31 * result + Objects.hashCode(keepAccessorsTogether);
        result = 31 * result + memberSubGroups.hashCode();
        result = 31 * result + selectorBlock.hashCode();
        result = 31 * result + separator.hashCode();
        result = 31 * result + orderingRules.hashCode();
        return result;
    }
}
