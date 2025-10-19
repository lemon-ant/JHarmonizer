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
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CompiledTopLevelTypesOrdering {

    /**
     * Ordered predicates applied to top-level candidates (MemberDescriptor).
     * The first matching predicate determines the candidate's bucket.
     *
     * Semantics:
     * - includes-only;
     * - within a single "rule line" the semantics is OR over type kinds;
     * - name/annotations are NOT used here (types-only focus).
     */
    @NonNull
    List<Predicate<MemberDescriptor>> orderedPredicates;
}
