package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
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
// TODO Remove builder
@Builder
public class UnifiedMemberGroupSelectorBlock {

    @NonNull
    @Singular
    Set<UnifiedMemberGroupRuleLine> includes;

    @NonNull
    @Singular
    Set<UnifiedMemberGroupRuleLine> excludes;

    public UnifiedMemberGroupSelectorBlock(
            @NonNull Set<UnifiedMemberGroupRuleLine> includes, @NonNull Set<UnifiedMemberGroupRuleLine> excludes) {
        this.includes = Collections.unmodifiableSet(includes);
        this.excludes = Collections.unmodifiableSet(excludes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedMemberGroupSelectorBlock that)) {
            return false;
        }

        return includes.equals(that.includes) && excludes.equals(that.excludes);
    }

    @Override
    public int hashCode() {
        int result = includes.hashCode();
        result = 31 * result + excludes.hashCode();
        return result;
    }
}
