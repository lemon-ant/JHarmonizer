package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * Compiles ONE rule-line into ONE predicate: mask AND (optional name) AND (optional annotations).
 * Runtime executes a single function with zero mode switching.
 */
@UtilityClass
class RuleLineAssembler {

    public static Predicate<MemberDescriptor> assembleLineRule(
            int requiredDeclarationFlagsMask,
            @Nullable Predicate<MemberDescriptor> namePredicateOpt,
            @Nullable Predicate<MemberDescriptor> annotationPredicateOpt) {

        Predicate<MemberDescriptor> compiled = null;
        if (requiredDeclarationFlagsMask != 0) {
            compiled = RuleAtomPredicates.createMaskContainsAll(requiredDeclarationFlagsMask);
        }
        if (null != namePredicateOpt) {
            compiled = null == compiled ? namePredicateOpt : compiled.and(namePredicateOpt);
        }
        if (null != annotationPredicateOpt) {
            compiled = null == compiled ? annotationPredicateOpt : compiled.and(annotationPredicateOpt);
        }
        if (null != compiled) {
            return compiled;
        }
        throw new IllegalStateException("Rule line doesn't contain any rules");
    }
}
