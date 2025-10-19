package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * Compiles unified top-level ordering into a pure data model with ordered predicates.
 * Semantics:
 *  - includes-only;
 *  - each unified "type group" becomes a single OR-by-kind predicate;
 *  - optional head predicate (IS_MAIN_TYPE) is prepended when mainTypeFirst is enabled.
 */
@UtilityClass
class TopLevelTypesOrderingCompiler {
    /** TODO Will be prepended when mainTypeFirst=true; replace with a real impl. */
    private static final Predicate<MemberDescriptor> IS_MAIN_TYPE = descriptor -> false;

    /**
     * Compile top-level ordering from the unified config.
     * @param topLevelTypesOrdering unified definition with type groups and mainTypeFirst flag
     * @return compiled order with ordered predicates (types-only)
     */
    CompiledTopLevelTypesOrdering compileTopLevelTypesOrdering(UnifiedTopLevelTypesOrdering topLevelTypesOrdering) {
        List<Predicate<MemberDescriptor>> orderedPredicates = new ArrayList<>();

        // 1) Optional head rule: external "is main type" predicate
        if (topLevelTypesOrdering.isMainTypeFirst()) {
            orderedPredicates.add(IS_MAIN_TYPE);
        }

        // 2) For each type group: OR over kinds
        for (UnifiedTopLevelTypeSelector topLevelTypeSelector : topLevelTypesOrdering.getTopLevelTypeSelectors()) {
            Set<MemberKind> memberKinds = topLevelTypeSelector.getTypeKinds().stream()
                    .map(UnifiedTypeKind::getMemberKind)
                    .collect(Collectors.toUnmodifiableSet());
            orderedPredicates.add(descriptor -> memberKinds.contains(descriptor.getMemberKind()));
        }

        return new CompiledTopLevelTypesOrdering(orderedPredicates);
    }
}
