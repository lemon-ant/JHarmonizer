package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static java.util.stream.Collectors.toUnmodifiableSet;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTypeKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Maps vendor JHarmonizerTopLevelTypesOrdering to unified.
 */
@UtilityClass
class TopLevelTypesOrderingMapper {

    @NonNull
    static UnifiedTopLevelTypesOrdering map(@NonNull JHarmonizerTopLevelTypesOrdering srcTopLevelTypesOrdering) {
        List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors =
                srcTopLevelTypesOrdering.getTopLevelTypeSelectors().stream()
                        .map(typeGroup -> new UnifiedTopLevelTypeSelector(typeGroup.getTypeKinds().stream()
                                .map(JHarmonizerTypeKind::getUnifiedTypeKind)
                                .collect(toUnmodifiableSet())))
                        .toList();

        List<UnifiedOrderingRule> orderingRules = srcTopLevelTypesOrdering.getOrderingRules().stream()
                .map(JHarmonizerOrderingRule::getUnifiedOrderingRule)
                .toList();

        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(srcTopLevelTypesOrdering.isMainTypeFirst())
                .topLevelTypeSelectors(topLevelTypeSelectors)
                .orderingRules(orderingRules)
                .build();
    }
}
