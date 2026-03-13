package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
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
 *
 * <p>TODO Replace this passive data holder with a fully compiled top-level ordering strategy that is created once
 * during configuration compilation and then reused as-is at runtime. Comparator construction and any main-type-aware
 * branching should not happen on the hot path for every processed compilation unit.</p>
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CompiledTopLevelTypesOrdering {
    boolean mainTypeFirst;

    @NonNull
    List<OrderingRule> orderingRules;

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
}
