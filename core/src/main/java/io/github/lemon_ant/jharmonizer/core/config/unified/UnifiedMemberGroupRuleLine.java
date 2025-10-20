package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * One rule line is a conjunction (AND) of atomic constraints.
 * <p>
 * During compilation, this becomes a single predicate function over EffectiveMemberDescriptor:
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
    @Builder
    public UnifiedMemberGroupRuleLine(
            @NonNull @Singular Set<@NonNull MemberKind> memberKinds,
            @NonNull @Singular Set<@NonNull MemberAccess> memberAccesses,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @Nullable UnifiedNameMatcher nameMatcher,
            @NonNull @Singular Set<@NonNull UnifiedAnnotationMatcher> annotationMatchers) {
        this.memberKinds = Collections.unmodifiableSet(memberKinds);
        this.memberAccesses = Collections.unmodifiableSet(memberAccesses);
        this.declarationModifiers = Collections.unmodifiableSet(new TreeSet<>(declarationModifiers));
        this.nameMatcher = nameMatcher;
        this.annotationMatchers = Collections.unmodifiableSet(annotationMatchers);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedMemberGroupRuleLine that)) {
            return false;
        }

        return memberKinds.equals(that.memberKinds)
                && memberAccesses.equals(that.memberAccesses)
                && declarationModifiers.equals(that.declarationModifiers)
                && Objects.equals(nameMatcher, that.nameMatcher)
                && annotationMatchers.equals(that.annotationMatchers);
    }

    @Override
    public int hashCode() {
        int result = memberKinds.hashCode();
        result = 31 * result + memberAccesses.hashCode();
        result = 31 * result + declarationModifiers.hashCode();
        result = 31 * result + Objects.hash(nameMatcher);
        result = 31 * result + annotationMatchers.hashCode();
        return result;
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
