package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.Value;

/**
 * Sorting behavior is orthogonal to matching; extend as needed.
 */
@Value
@SuppressFBWarnings
public class GroupSortingBehavior {
    @NonNull
    SortKeys sortKeys;

    boolean keepAccessorsTogether;
    String separatorDirective; // nullable allowed

    public static GroupSortingBehavior defaults() {
        return new GroupSortingBehavior(SortKeys.PRESERVE, false, null);
    }

    public enum SortKeys {
        PRESERVE,
        ALPHA,
        SOURCE_ORDER
    }
}
