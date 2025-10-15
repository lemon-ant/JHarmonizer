package io.github.lemon_ant.jharmonizer.core.config.compiled;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Atomic predicate builders — compiled once, used as-is at runtime.
 * No mode checks in runtime; all branching is resolved by the converter at compile time.
 */
@UtilityClass
class RuleAtomPredicates {

    /**
     * Mask check via existing MemberDeclarationFlagsUtil (single int mask).
     */
    @NonNull
    static Predicate<MemberDescriptor> createMaskContainsAll(int requiredDeclarationFlagsMask) {
        if (requiredDeclarationFlagsMask == 0) {
            throw new IllegalArgumentException(/*TODO*/ );
        }
        return effectiveMemberDescriptor -> MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(
                effectiveMemberDescriptor.getFeatureMask(), requiredDeclarationFlagsMask);
    }

    /**
     * Name: "=exact" compiled once.
     */
    @NonNull
    static Predicate<MemberDescriptor> createNameExact(@NonNull String expectedName) {
        return effectiveMemberDescriptor -> effectiveMemberDescriptor
                .getName()
                .map(actualName -> actualName.equals(expectedName))
                .orElse(false);
    }

    /**
     * Name: "~regex" compiled once.
     */
    @NonNull
    static Predicate<MemberDescriptor> createNameRegex(@NonNull Pattern pattern) {
        return effectiveMemberDescriptor -> effectiveMemberDescriptor
                .getName()
                .map(actualName -> pattern.matcher(actualName).matches())
                .orElse(false);
    }

    @NonNull
    static Predicate<MemberDescriptor> createAnnotationExact(@NonNull String expectedFqn) {
        return effectiveMemberDescriptor ->
                effectiveMemberDescriptor.getAnnotationQualifiedNames().stream().anyMatch(expectedFqn::equals);
    }

    @NonNull
    static Predicate<MemberDescriptor> createAnnotationRegex(@NonNull Pattern pattern) {
        return effectiveMemberDescriptor -> effectiveMemberDescriptor.getAnnotationQualifiedNames().stream()
                .anyMatch(annotation -> pattern.matcher(annotation).matches());
    }
}
