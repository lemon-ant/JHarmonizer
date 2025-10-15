// =====================================================================================
// FILE: UnifiedRuleLine.java
// =====================================================================================
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import io.github.lemon_ant.jharmonizer.core.config.compiled.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.compiled.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.compiled.MemberKind;
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
    Set<@NonNull MemberKind> kinds;

    /**
     * Allowed access levels (OR). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull MemberAccess> accessLevels;

    /**
     * Required declaration modifiers (ALL-OF). Empty set ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /**
     * Optional name constraint (exact or regex). Null ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull UnifiedNameMatcher> names;

    /**
     * Any-of annotations: OR over the list; each matcher can be exact or regex. Empty ⇒ no constraint.
     */
    @NonNull
    Set<@NonNull UnifiedAnnotationMatcher> annotations;

    @Builder
    public UnifiedRuleLine(
            @NonNull @Singular Set<@NonNull MemberKind> kinds,
            @NonNull @Singular Set<@NonNull MemberAccess> accessLevels,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull @Singular Set<@NonNull UnifiedNameMatcher> names,
            @NonNull @Singular Set<@NonNull UnifiedAnnotationMatcher> annotations) {
        this.kinds = Collections.unmodifiableSet(kinds);
        this.accessLevels = Collections.unmodifiableSet(accessLevels);
        this.declarationModifiers = Collections.unmodifiableSet(declarationModifiers);
        this.names = Collections.unmodifiableSet(names);
        this.annotations = Collections.unmodifiableSet(annotations);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnifiedRuleLine that)) {
            return false;
        }

        return kinds.equals(that.kinds)
                && accessLevels.equals(that.accessLevels)
                && declarationModifiers.equals(that.declarationModifiers)
                && names.equals(that.names)
                && annotations.equals(that.annotations);
    }

    @Override
    public int hashCode() {
        int result = kinds.hashCode();
        result = 31 * result + accessLevels.hashCode();
        result = 31 * result + declarationModifiers.hashCode();
        result = 31 * result + names.hashCode();
        result = 31 * result + annotations.hashCode();
        return result;
    }
}
