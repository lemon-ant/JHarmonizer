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
    static Predicate<MemberDescriptor> createNameRegex(@NonNull String expectedPattern) {
        Pattern pattern = Pattern.compile(expectedPattern);
        return effectiveMemberDescriptor -> effectiveMemberDescriptor
                .getName()
                .map(actualName -> pattern.matcher(actualName).matches())
                .orElse(false);
    }

    @NonNull
    static Predicate<MemberDescriptor> createAnnotationExactFqnOrSimple(@NonNull String expectedName) {
        return memberDescriptor -> memberDescriptor.getAnnotationQualifiedNames().stream()
                .anyMatch(fqcn ->
                        fqcn.equals(expectedName) || extractSimpleName(fqcn).equals(expectedName));
    }

    @NonNull
    static Predicate<MemberDescriptor> createAnnotationRegexFqnOrSimple(@NonNull Pattern pattern) {
        return memberDescriptor -> memberDescriptor.getAnnotationQualifiedNames().stream()
                .anyMatch(fqcn -> pattern.matcher(fqcn).matches()
                        || pattern.matcher(extractSimpleName(fqcn)).matches());
    }

    /**
     * Extracts simple name from fully qualified name, tolerant to non-qualified names.
     */
    private static String extractSimpleName(@NonNull String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }
}
