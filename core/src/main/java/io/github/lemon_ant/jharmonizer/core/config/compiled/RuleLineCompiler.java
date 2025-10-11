package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.NonNull;

/**
 * Compiles ONE rule-line into ONE predicate: mask AND (optional name) AND (optional annotations).
 * Runtime executes a single function with zero mode switching.
 */
final class RuleLineCompiler {
    private RuleLineCompiler() {}

    public static RuleLineMatcher compile(
            int requiredDeclarationFlagsMask,
            @NonNull Optional<Predicate<MemberDescriptor>> namePredicateOpt,
            @NonNull Optional<Predicate<MemberDescriptor>> annotationPredicateOpt) {
        Predicate<MemberDescriptor> predicate = RuleAtomPredicates.createMaskContainsAll(requiredDeclarationFlagsMask);
        if (namePredicateOpt.isPresent()) predicate = predicate.and(namePredicateOpt.get());
        if (annotationPredicateOpt.isPresent()) predicate = predicate.and(annotationPredicateOpt.get());
        final Predicate<MemberDescriptor> compiled = predicate;
        return compiled::test;
    }
}
