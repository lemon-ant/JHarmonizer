package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.Value;

/**
 * Sorting behavior is orthogonal to matching; extend as needed.
 */
@Value
@SuppressFBWarnings
// TODO This is a draft only
public class CompiledGroupSortingBehavior {
    @NonNull
    CompiledGroupSortingBehavior.SortKey sortKey;

    boolean keepAccessorsTogether;
    String separatorDirective; // nullable allowed

    public static CompiledGroupSortingBehavior defaults() {
        return new CompiledGroupSortingBehavior(SortKey.PRESERVE, false, null);
    }

    public enum SortKey {
        PRESERVE,
        ALPHA,
        SOURCE_ORDER
    }
}
