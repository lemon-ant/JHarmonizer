// =====================================================================================
// FILE: UnifiedRuleLine.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import io.github.lemon_ant.jharmonizer.core.config.compiled.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.compiled.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.compiled.MemberKind;
import java.util.Collections;
import java.util.Set;
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
public class UnifiedRuleLine {

    /**
     * Allowed kinds of the member (OR inside the set). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull MemberKind> memberKinds;

    /**
     * Allowed access levels (OR). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull MemberAccess> memberAccesses;

    /**
     * Required declaration modifiers (ALL-OF). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /**
     * Optional name constraint (exact or regex). Null ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull UnifiedNameMatcher> nameMatchers;

    /**
     * Any-of annotations: OR over the list; each matcher can be exact or regex. Empty ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull UnifiedAnnotationMatcher> annotationMatchers;

    @Builder
    public UnifiedRuleLine(
            @NonNull @Singular Set<@NonNull MemberKind> memberKinds,
            @NonNull @Singular Set<@NonNull MemberAccess> memberAccesses,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull @Singular Set<@NonNull UnifiedNameMatcher> nameMatchers,
            @NonNull @Singular Set<@NonNull UnifiedAnnotationMatcher> annotationMatchers) {
        this.memberKinds = Collections.unmodifiableSet(memberKinds);
        this.memberAccesses = Collections.unmodifiableSet(memberAccesses);
        this.declarationModifiers = Collections.unmodifiableSet(declarationModifiers);
        this.nameMatchers = Collections.unmodifiableSet(nameMatchers);
        this.annotationMatchers = Collections.unmodifiableSet(annotationMatchers);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedRuleLine that)) {
            return false;
        }

        return memberKinds.equals(that.memberKinds)
                && memberAccesses.equals(that.memberAccesses)
                && declarationModifiers.equals(that.declarationModifiers)
                && nameMatchers.equals(that.nameMatchers)
                && annotationMatchers.equals(that.annotationMatchers);
    }

    @Override
    public int hashCode() {
        int result = memberKinds.hashCode();
        result = 31 * result + memberAccesses.hashCode();
        result = 31 * result + declarationModifiers.hashCode();
        result = 31 * result + nameMatchers.hashCode();
        result = 31 * result + annotationMatchers.hashCode();
        return result;
    }
}
