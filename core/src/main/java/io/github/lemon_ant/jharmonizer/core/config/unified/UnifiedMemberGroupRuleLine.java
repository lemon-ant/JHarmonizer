// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * One rule line is a conjunction (AND) of atomic constraints.
 * <p>
 * During compilation, this becomes a single predicate function over CompiledMemberDescriptor:
 * maskPredicate AND [namePredicate?] AND [annotationsPredicate?]
 */
@Value
public class UnifiedMemberGroupRuleLine {

    /**
     * Any-of annotations: OR over the list; each matcher can be exact or regex. Empty ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull UnifiedAnnotationMatcher> annotationMatchers;

    /**
     * Required declaration modifiers (ALL-OF). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /**
     * Allowed access levels (OR). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull MemberAccess> memberAccesses;

    /**
     * Allowed kinds of the member (OR inside the set). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull MemberKind> memberKinds;

    /**
     * Optional name constraint (exact or regex). Null ⇒ no constraint.
     */
    @Nullable
    UnifiedNameMatcher nameMatcher;

    // TODO Remove builder
    /**
     * Creates a new UnifiedMemberGroupRuleLine.
     * @param memberKinds the member kinds
     * @param memberAccesses the member accesses
     * @param declarationModifiers the declaration modifiers
     * @param nameMatcher the name matcher
     * @param annotationMatchers the annotation matchers
     */
    @Builder
    public UnifiedMemberGroupRuleLine(
            @NonNull @Singular Set<@NonNull MemberKind> memberKinds,
            @NonNull @Singular Set<@NonNull MemberAccess> memberAccesses,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @Nullable UnifiedNameMatcher nameMatcher,
            @NonNull @Singular Set<@NonNull UnifiedAnnotationMatcher> annotationMatchers) {
        this.memberKinds = Collections.unmodifiableSet(new TreeSet<>(memberKinds));
        this.memberAccesses = Collections.unmodifiableSet(memberAccesses);
        this.declarationModifiers = Collections.unmodifiableSet(new TreeSet<>(declarationModifiers));
        this.nameMatcher = nameMatcher;
        this.annotationMatchers = Collections.unmodifiableSet(annotationMatchers);
        validateAtLeastOneSelectorIsConfigured();
    }

    private boolean hasAnySelectorConfigured() {
        boolean hasMemberKindsConfigured = !memberKinds.isEmpty();
        boolean hasMemberAccessesConfigured = !memberAccesses.isEmpty();
        boolean hasDeclarationModifiersConfigured = !declarationModifiers.isEmpty();
        boolean hasNameMatcherConfigured = nameMatcher != null;
        boolean hasAnnotationMatchersConfigured = !annotationMatchers.isEmpty();

        return hasMemberKindsConfigured
                || hasMemberAccessesConfigured
                || hasDeclarationModifiersConfigured
                || hasNameMatcherConfigured
                || hasAnnotationMatchersConfigured;
    }

    private void validateAtLeastOneSelectorIsConfigured() {
        if (!hasAnySelectorConfigured()) {
            throw new IllegalArgumentException(
                    this.getClass().getSimpleName() + " must have at least one selector configured: "
                            + "memberKinds, memberAccesses, declarationModifiers, nameMatcher or annotationMatchers");
        }
    }

    /**
     * Lombok will generate the builder class named UnifiedRuleLineBuilder by default.
     * We extend it with a custom setter for nameMatcher that throws on second assignment.
     */
    public static class UnifiedMemberGroupRuleLineBuilder {
        private boolean nameMatcherAlreadyAssigned;

        /**
         * Assigns the name matcher exactly once. Any subsequent call results in an exception.
         *
         * @param nameMatcher the matcher to set (nullable means "no constraint")
         * @return the builder
         * @throws IllegalStateException if nameMatcher has already been assigned
         */
        @NonNull
        // TODO Why we do not use this method result???
        public UnifiedMemberGroupRuleLineBuilder nameMatcher(@Nullable UnifiedNameMatcher nameMatcher) {
            if (this.nameMatcherAlreadyAssigned) {
                throw new IllegalStateException("nameMatcher has already been assigned for " + this);
            }
            // delegate to Lombok-generated field (same name)
            this.nameMatcher = nameMatcher;
            this.nameMatcherAlreadyAssigned = true;
            return this;
        }
    }
}
