package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDeclarationFlagsUtil;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
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
    private static final String ZERO_REQUIRED_DECLARATION_FLAGS_MASK_MESSAGE =
            "Required declaration flags mask must not be zero";

    /**
     * Creates the annotation exact fqn or simple.
     * @param expectedName the expected name
     * @return the created annotation exact fqn or simple
     */
    @NonNull
    static Predicate<MemberDescriptor> createAnnotationExactFqnOrSimple(@NonNull String expectedName) {
        return memberDescriptor -> memberDescriptor.getAnnotationQualifiedNames().stream()
                .anyMatch(fqcn ->
                        fqcn.equals(expectedName) || extractSimpleName(fqcn).equals(expectedName));
    }

    /**
     * Creates the annotation regex fqn or simple.
     * @param pattern the pattern
     * @return the created annotation regex fqn or simple
     */
    @NonNull
    static Predicate<MemberDescriptor> createAnnotationRegexFqnOrSimple(@NonNull Pattern pattern) {
        return memberDescriptor -> memberDescriptor.getAnnotationQualifiedNames().stream()
                .anyMatch(fqcn -> pattern.matcher(fqcn).matches()
                        || pattern.matcher(extractSimpleName(fqcn)).matches());
    }

    /**
     * Mask check via existing MemberDeclarationFlagsUtil (single int mask).
     */
    @NonNull
    static Predicate<MemberDescriptor> createMaskContainsAll(int requiredDeclarationFlagsMask) {
        if (requiredDeclarationFlagsMask == 0) {
            throw new IllegalArgumentException(ZERO_REQUIRED_DECLARATION_FLAGS_MASK_MESSAGE);
        }
        return memberDescriptor -> MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(
                memberDescriptor.getFeatureMask(), requiredDeclarationFlagsMask);
    }

    /**
     * Creates the mask contains any.
     * @param requiredDeclarationFlagsMask the required declaration flags mask
     * @return the created mask contains any
     */
    @NonNull
    static Predicate<MemberDescriptor> createMaskContainsAny(int requiredDeclarationFlagsMask) {
        if (requiredDeclarationFlagsMask == 0) {
            throw new IllegalArgumentException(ZERO_REQUIRED_DECLARATION_FLAGS_MASK_MESSAGE);
        }
        return memberDescriptor -> MemberDeclarationFlagsUtil.containsAnyRequiredDeclarationFlags(
                memberDescriptor.getFeatureMask(), requiredDeclarationFlagsMask);
    }

    /**
     * Name: "=exact" compiled once.
     */
    @NonNull
    static Predicate<MemberDescriptor> createNameExact(@NonNull String expectedName) {
        return memberDescriptor -> memberDescriptor
                .getName()
                .map(actualName -> actualName.equals(expectedName))
                .orElse(false);
    }

    /**
     * Name: "~regex" compiled once.
     */
    @NonNull
    static Predicate<MemberDescriptor> createNameRegex(@NonNull String expectedPattern) {
        Pattern pattern = Pattern.compile(expectedPattern);
        return memberDescriptor -> memberDescriptor
                .getName()
                .map(actualName -> pattern.matcher(actualName).matches())
                .orElse(false);
    }

    /**
     * Extracts simple name from fully qualified name, tolerant to non-qualified names.
     */
    @NonNull
    private static String extractSimpleName(@NonNull String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }
}
