package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import java.util.List;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Pure data model for compiled top-level ordering.
 * Holds an ordered list of predicates; order defines bucket priority.
 * No logic here — compiler is responsible for constructing the sequence.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CompiledTopLevelTypesOrdering {

    /**
     * Ordered predicates applied to top-level candidates (MemberDescriptor).
     * The first matching predicate determines the candidate's bucket.
     * <p>
     * Semantics:
     * - includes-only;
     * - within a single "rule line" the semantics is OR over type kinds;
     * - name/annotations are NOT used here (types-only focus).
     */
    @NonNull
    List<Predicate<MemberDescriptor>> topLevelTypesSelectors;

    // TODO It should be implemented it this way:
    // 1) There should be some comparator factory provider, that can create/return factories of 2 types, dependent on
    //    this flag:
    //      - mainTypeFirst = true - factory can return a comparator that compare types with the java-file name
    //      - mainTypeFirst = false - factory can return a comparator that ignore java-file name at all
    // 2) For each java-file we will create a new comparator with a new java-file name. If mainTypeFirst = false, then
    //    factory will return the same singleton instance, that ignores java-file name
    boolean mainTypeFirst;

    @NonNull
    // TODO It should be compiled
    List<UnifiedSortingBehavior.UnifiedSortKey> sortKeys;
}
