// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.EXACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.REGEX;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDeclarationFlagsUtil;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * Compiles ONE rule-line into ONE predicate: mask AND (optional name) AND (optional annotations).
 * Runtime executes a single function with zero mode switching.
 */
@UtilityClass
class MemberGroupRuleLineCompiler {

    /**
     * Compiles the rule line.
     * @param unifiedMemberGroupRuleLine the unified member group rule line
     * @return the compiled rule line
     */
    @NonNull
    static Predicate<MemberDescriptor> compileRuleLine(@NonNull UnifiedMemberGroupRuleLine unifiedMemberGroupRuleLine) {

        Predicate<MemberDescriptor> namePredicateOpt =
                compileNamePredicate(unifiedMemberGroupRuleLine.getNameMatcher());

        Predicate<MemberDescriptor> annotationPredicateOpt =
                compileAnnotationPredicate(unifiedMemberGroupRuleLine.getAnnotationMatchers());

        Predicate<MemberDescriptor> memberDescriptorPredicate = compileMaskPredicate(
                unifiedMemberGroupRuleLine.getMemberKinds(),
                unifiedMemberGroupRuleLine.getMemberAccesses(),
                unifiedMemberGroupRuleLine.getDeclarationModifiers());

        return assembleRuleLine(memberDescriptorPredicate, namePredicateOpt, annotationPredicateOpt);
    }

    @NonNull
    @SafeVarargs
    private static Predicate<MemberDescriptor> assembleRuleLine(Predicate<MemberDescriptor>... predicates) {
        // AND of all non-null predicates; require at least one.
        return Arrays.stream(predicates)
                .filter(Objects::nonNull)
                .reduce(Predicate::and)
                .orElseThrow(() -> new IllegalStateException("Rule line doesn't contain any rules"));
    }

    @Nullable
    private static Predicate<MemberDescriptor> compileAnnotationPredicate(
            Set<UnifiedAnnotationMatcher> annotationMatchers) {
        if (annotationMatchers.isEmpty()) {
            return null;
        }

        List<Predicate<MemberDescriptor>> compiledPredicates = annotationMatchers.stream()
                .map(MemberGroupRuleLineCompiler::createPredicateForAnnotationMatcher)
                .toList();

        return descriptor -> compiledPredicates.stream().allMatch(predicate -> predicate.test(descriptor));
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

    @Nullable
    private static Predicate<MemberDescriptor> compileNamePredicate(@Nullable UnifiedNameMatcher nameMatcher) {
        if (null == nameMatcher) {
            return null;
        }
        return createPredicateForNameMatcher(nameMatcher);
    }

    @NonNull
    private static Predicate<MemberDescriptor> createPredicateForAnnotationMatcher(UnifiedAnnotationMatcher matcher) {

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
    private static Predicate<MemberDescriptor> createPredicateForNameMatcher(UnifiedNameMatcher matcher) {
        if (matcher.getMatchMethod() == EXACT) {
            return RuleAtomPredicates.createNameExact(matcher.getValue());
        }
        if (matcher.getMatchMethod() == REGEX) {
            return RuleAtomPredicates.createNameRegex(matcher.getValue());
        }
        throw new IllegalStateException(
                "Unexpected " + UnifiedMatchMethod.class.getSimpleName() + " value: " + matcher.getMatchMethod());
    }
}
