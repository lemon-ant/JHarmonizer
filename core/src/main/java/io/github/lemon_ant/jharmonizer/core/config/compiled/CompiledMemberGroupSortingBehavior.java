package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Sorting behavior is orthogonal to matching; extend as needed.
 */
@Value
@SuppressFBWarnings
// TODO This is a draft only.
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CompiledMemberGroupSortingBehavior {

    // TODO We do not need it at the end
    @NonNull
    CompiledMemberGroupSortingBehavior.SortKey sortKey;

    // TODO We do not need it at the end
    boolean keepAccessorsTogether;

    // TODO private static final Comparator<MemberDescriptor> sortingComparator = ;

    // TODO We do not need it at the end
    public enum SortKey {
        PRESERVE,
        ALPHA,
        SOURCE_ORDER,
        VISIBILITY_ASC,
        VISIBILITY_DESC,
        SIGNATURE,
    }
}
