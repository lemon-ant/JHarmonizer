package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static java.util.stream.Collectors.toUnmodifiableSet;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * Compiles unified top-level ordering into a pure data model with ordered predicates.
 * Semantics:
 * - includes-only;
 * - each unified "type group" becomes a single OR-by-kind predicate;
 * - optional head predicate (IS_MAIN_TYPE) is prepended when mainTypeFirst is enabled.
 */
@UtilityClass
class TopLevelTypesOrderingCompiler {

    /**
     * Compile top-level ordering from the unified config.
     *
     * @param unifiedTopLevelTypesOrdering unified definition with type groups and mainTypeFirst flag
     * @return compiled order with ordered predicates (types-only)
     */
    CompiledTopLevelTypesOrdering compileTopLevelTypesOrdering(
            UnifiedTopLevelTypesOrdering unifiedTopLevelTypesOrdering) {
        List<Predicate<MemberDescriptor>> compiledTopLevelTypesSelectors =
                unifiedTopLevelTypesOrdering.getTopLevelTypeSelectors().stream()
                        .<Predicate<MemberDescriptor>>map(topLevelTypeSelector -> {
                            Set<MemberKind> memberKinds = topLevelTypeSelector.getTypeKinds().stream()
                                    .map(UnifiedTypeKind::getMemberKind)
                                    .collect(toUnmodifiableSet());

                            return memberDescriptor -> memberKinds.contains(memberDescriptor.getMemberKind());
                        })
                        .toList();

        return new CompiledTopLevelTypesOrdering(
                unifiedTopLevelTypesOrdering.isMainTypeFirst(),
                MemberGroupCompiler.mapOrderingRules(unifiedTopLevelTypesOrdering.getOrderingRules()),
                compiledTopLevelTypesSelectors);
    }
}
