package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.EXACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.REGEX;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedRuleLine;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Compiles ONE rule-line into ONE predicate: mask AND (optional name) AND (optional annotations).
 * Runtime executes a single function with zero mode switching.
 */
@UtilityClass
class RuleLineCompiler {

    static Predicate<MemberDescriptor> compileRuleLine(UnifiedRuleLine unifiedRuleLine) {

        Predicate<MemberDescriptor> namePredicateOpt = compileNamePredicate(unifiedRuleLine.getNameMatchers());

        Predicate<MemberDescriptor> annotationPredicateOpt =
                compileAnnotationPredicate(unifiedRuleLine.getAnnotationMatchers());

        Predicate<MemberDescriptor> memberDescriptorPredicate = compileMaskPredicate(
                unifiedRuleLine.getMemberKinds(),
                unifiedRuleLine.getMemberAccesses(),
                unifiedRuleLine.getDeclarationModifiers());

        return assembleRuleLine(memberDescriptorPredicate, namePredicateOpt, annotationPredicateOpt);
    }

    @Nullable
    private static Predicate<MemberDescriptor> compileNamePredicate(@NonNull Set<UnifiedNameMatcher> nameMatchers) {
        if (nameMatchers.isEmpty()) {
            return null;
        }

        List<Predicate<MemberDescriptor>> compiledPredicates = nameMatchers.stream()
                .map(RuleLineCompiler::predicateForNameMatcher)
                .toList();

        return descriptor -> {
            for (Predicate<MemberDescriptor> predicate : compiledPredicates) {
                if (predicate.test(descriptor)) return true;
            }
            return false;
        };
    }

    @Nullable
    private static Predicate<MemberDescriptor> compileAnnotationPredicate(
            Set<UnifiedAnnotationMatcher> annotationMatchers) {
        if (annotationMatchers.isEmpty()) {
            return null;
        }

        List<Predicate<MemberDescriptor>> compiledPredicates = annotationMatchers.stream()
                .map(RuleLineCompiler::predicateForAnnotationMatcher)
                .toList();

        return descriptor -> compiledPredicates.stream().allMatch(predicate -> predicate.test(descriptor));
    }

    @NonNull
    private static Predicate<MemberDescriptor> predicateForAnnotationMatcher(UnifiedAnnotationMatcher matcher) {

        final String value = matcher.getValue();

        if (matcher.getMatchMethod() == EXACT) {
            return RuleAtomPredicates.createAnnotationExactFqnOrSimple(value);
        }
        if (matcher.getMatchMethod() == REGEX) {
            final Pattern regex = Pattern.compile(value);
            return RuleAtomPredicates.createAnnotationRegexFqnOrSimple(regex);
        }
        throw new IllegalStateException(
                "Unexpected " + UnifiedMatchMethod.class.getSimpleName() + " value: " + matcher.getMatchMethod());
    }

    @NonNull
    private static Predicate<MemberDescriptor> predicateForNameMatcher(UnifiedNameMatcher matcher) {
        if (matcher.getMatchMethod() == EXACT) {
            return RuleAtomPredicates.createNameExact(matcher.getValue());
        }
        if (matcher.getMatchMethod() == REGEX) {
            return RuleAtomPredicates.createNameRegex(matcher.getValue());
        }
        throw new IllegalStateException(
                "Unexpected " + UnifiedMatchMethod.class.getSimpleName() + " value: " + matcher.getMatchMethod());
    }

    @Nullable
    private static Predicate<MemberDescriptor> compileMaskPredicate(
            Set<MemberKind> memberKinds,
            Set<MemberAccess> memberAccesses,
            Set<DeclarationModifier> declarationModifiers) {
        int requiredDeclarationFlagsMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                memberKinds, memberAccesses, declarationModifiers);
        return requiredDeclarationFlagsMask == 0
                ? null
                : RuleAtomPredicates.createMaskContainsAll(requiredDeclarationFlagsMask);
    }

    @NonNull
    @SafeVarargs
    private static Predicate<MemberDescriptor> assembleRuleLine(Predicate<MemberDescriptor>... predicates) {
        // AND of all non-null predicates; require at least one
        Predicate<MemberDescriptor> combined = null;
        for (Predicate<MemberDescriptor> predicate : predicates) {
            if (predicate == null) {
                continue;
            }
            combined = (combined == null) ? predicate : combined.and(predicate);
        }
        if (combined == null) {
            throw new IllegalStateException("Rule line doesn't contain any rules");
        }
        return combined;
    }
}
