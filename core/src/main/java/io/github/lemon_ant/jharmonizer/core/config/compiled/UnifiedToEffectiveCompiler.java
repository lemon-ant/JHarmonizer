package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatchKind.*;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatchKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Translates strict Unified configuration into the Effective runtime model.
 * The Effective model is branch-free at runtime: each rule-line is compiled into a single predicate.
 *
 * This compiler performs the following steps:
 *  - Compiles selector blocks (includes/excludes) into {@link CompiledSelectorBlock}
 *  - Maps sorting behavior knobs to {@link CompiledGroupSortingBehavior}
 *  - Builds a DFS-ordered tree of {@link CompiledGroup} nodes and assigns post-order indexes
 */
@UtilityClass
@SuppressWarnings("PMD")
public class UnifiedToEffectiveCompiler {

    /**
     * Compiles a full Unified configuration into an Effective configuration.
     */
    @NonNull
    public static CompiledConfig compile(@NonNull UnifiedConfig unifiedConfig) {
        // TODO Today we only compile the member groups; top-level type ordering may feed into future phases.
        List<CompiledGroup> typeRoots = compileTopLevelGroups(unifiedConfig.getRootMemberGroups());

        return new CompiledConfig(typeRoots);
    }

    @NonNull
    private static List<CompiledGroup> compileTopLevelGroups(@NonNull List<UnifiedMemberGroup> unifiedGroups) {
        return unifiedGroups.stream()
                .map(UnifiedToEffectiveCompiler::compileGroupRecursively)
                .toList();
    }

    @NonNull
    private static CompiledGroup compileGroupRecursively(@NonNull UnifiedMemberGroup unifiedGroup) {
        String groupName = unifiedGroup.getGroupName();

        CompiledSelectorBlock compiledSelectorBlock = compileSelectorBlock(unifiedGroup.getSelectorBlock());
        CompiledGroupSortingBehavior sortingBehavior = mapSortingBehavior(unifiedGroup.getSortingBehavior());

        // Recursively compile children first (DFS order preserved)
        List<UnifiedMemberGroup> childGroups = unifiedGroup.getMemberSubGroups();
        List<CompiledGroup> compiledChildren = new ArrayList<>(childGroups.size());
        for (UnifiedMemberGroup child : childGroups) {
            compiledChildren.add(compileGroupRecursively(child));
        }

        // orderIndex will be assigned by CompiledConfig.assignPostOrderIndexes(...)
        return new CompiledGroup(groupName, /*orderIndex*/ 0, compiledSelectorBlock, sortingBehavior, compiledChildren);
    }

    @NonNull
    private static CompiledSelectorBlock compileSelectorBlock(@NonNull UnifiedSelectorBlock selectorBlock) {
        List<Predicate<MemberDescriptor>> includeMatchers = new ArrayList<>();
        for (UnifiedRuleLine includeLine : selectorBlock.getIncludes()) {
            includeMatchers.add(compileRuleLineToMatcher(includeLine));
        }

        List<Predicate<MemberDescriptor>> excludeMatchers = new ArrayList<>();
        for (UnifiedRuleLine excludeLine : selectorBlock.getExcludes()) {
            excludeMatchers.add(compileRuleLineToMatcher(excludeLine));
        }

        return new CompiledSelectorBlock(includeMatchers, excludeMatchers);
    }

    /**
     * Compiles one rule line into one or more matchers.
     * If a rule-line contains multiple kinds, we compile a matcher per kind (OR semantics across the list).
     */
    private static Predicate<MemberDescriptor> compileRuleLineToMatcher(UnifiedRuleLine unifiedRuleLine) {

        Optional<Predicate<MemberDescriptor>> namePredicateOpt = compileNamePredicate(unifiedRuleLine.getNameMatchers());

        // Annotation predicate is optional
        Optional<Predicate<MemberDescriptor>> annotationPredicateOpt =
                compileAnnotationPredicate(unifiedRuleLine.getAnnotationMatchers());

        // Compute mask
        int mask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                unifiedRuleLine.getMemberKinds(),
                unifiedRuleLine.getMemberAccesses(),
                unifiedRuleLine.getDeclarationModifiers());

        return RuleLineAssembler.assembleLineRule(
                mask, namePredicateOpt.orElse(null), annotationPredicateOpt.orElse(null));
    }

    @NonNull
    private static Optional<Predicate<MemberDescriptor>> compileNamePredicate(
            @NonNull Set<UnifiedNameMatcher> nameMatchers) {
        if (nameMatchers.isEmpty()) return Optional.empty();

        List<Predicate<MemberDescriptor>> compiledPredicates = nameMatchers.stream()
                .map(UnifiedToEffectiveCompiler::predicateForNameMatcher)
                .toList();

        Predicate<MemberDescriptor> disjunction = descriptor -> {
            for (Predicate<MemberDescriptor> predicate : compiledPredicates) {
                if (predicate.test(descriptor)) return true;
            }
            return false;
        };
        return Optional.of(disjunction);
    }

    private static Predicate<MemberDescriptor> predicateForNameMatcher(@NonNull UnifiedNameMatcher matcher) {
        if (matcher.getKind() == EXACT) {
            return RuleAtomPredicates.createNameExact(matcher.getValue());
        }
        if (matcher.getKind() == REGEX) {
            return RuleAtomPredicates.createNameRegex(matcher.getValue());
        }
        throw new IllegalStateException(
                "Unexpected " + UnifiedNameMatchKind.class.getSimpleName() + " value: " + matcher.getKind());
    }

    @NonNull
    private static Optional<Predicate<MemberDescriptor>> compileAnnotationPredicate(
            @NonNull Set<UnifiedAnnotationMatcher> annotationMatchers) {
        if (annotationMatchers.isEmpty()) return Optional.empty();

        List<Predicate<MemberDescriptor>> compiledPredicates = annotationMatchers.stream()
                .map(UnifiedToEffectiveCompiler::predicateForAnnotationMatcher)
                .toList();

        Predicate<MemberDescriptor> disjunction = descriptor -> {
            for (Predicate<MemberDescriptor> predicate : compiledPredicates) {
                if (predicate.test(descriptor)) return true;
            }
            return false;
        };
        return Optional.of(disjunction);
    }

    private static Predicate<MemberDescriptor> predicateForAnnotationMatcher(
            @NonNull UnifiedAnnotationMatcher matcher) {
        boolean isExact = matcher.getNameMatchKind() == EXACT;
        final String value = matcher.getValue();
        final Pattern regex = isExact ? null : Pattern.compile(value);

        return descriptor -> {
            for (String qualifiedAnnotationName : descriptor.getAnnotationQualifiedNames()) {
                String simpleName = extractSimpleName(qualifiedAnnotationName);
                if (isExact) {
                    if (qualifiedAnnotationName.equals(value) || simpleName.equals(value)) return true;
                } else {
                    if (regex.matcher(qualifiedAnnotationName).matches()
                            || regex.matcher(simpleName).matches()) return true;
                }
            }
            return false;
        };
    }

    private static String extractSimpleName(@NonNull String qualifiedName) {
        int idx = qualifiedName.lastIndexOf('.');
        return (idx < 0) ? qualifiedName : qualifiedName.substring(idx + 1);
    }

    @NonNull
    private static CompiledGroupSortingBehavior mapSortingBehavior(
            @NonNull UnifiedSortingBehavior unifiedSortingBehavior) {
        CompiledGroupSortingBehavior.SortKeys sortKeys = mapSortKeys(unifiedSortingBehavior.getUnifiedSortKeys());
        boolean keepAccessorsTogether = unifiedSortingBehavior.isKeepAccessorsTogether();
        // Separator handling can be added to Effective model later; keep a placeholder string for now (null ==
        // unspecified).
        return new CompiledGroupSortingBehavior(sortKeys, keepAccessorsTogether, null);
    }

    private static CompiledGroupSortingBehavior.SortKeys mapSortKeys(@NonNull List<UnifiedSortKey> unifiedSortKeys) {
        // We currently support a single effective sort key knob; pick the first meaningful item.
        for (UnifiedSortKey key : unifiedSortKeys) {
            switch (key) {
                case ALPHA:
                    return CompiledGroupSortingBehavior.SortKeys.ALPHA;
                case PRESERVE:
                    return CompiledGroupSortingBehavior.SortKeys.PRESERVE;
                default:
                    // VISIBILITY_ASC / VISIBILITY_DESC / SIGNATURE fall back to a stable source order for now.
                    return CompiledGroupSortingBehavior.SortKeys.SOURCE_ORDER;
            }
        }
        // Safety: default to PRESERVE if the list is somehow empty (should be validated earlier)
        return CompiledGroupSortingBehavior.SortKeys.PRESERVE;
    }
}
