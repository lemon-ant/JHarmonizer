package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static java.util.stream.Collectors.toUnmodifiableSet;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerSortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTypeKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior.UnifiedSortKey;
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

    static UnifiedTopLevelTypesOrdering map(@NonNull JHarmonizerTopLevelTypesOrdering srcTopLevelTypesOrdering) {
        List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors =
                srcTopLevelTypesOrdering.getTopLevelTypeSelectors().stream()
                        .map(typeGroup -> new UnifiedTopLevelTypeSelector(typeGroup.getTypeKinds().stream()
                                .map(JHarmonizerTypeKind::getUnifiedTypeKind)
                                .collect(toUnmodifiableSet())))
                        .toList();

        List<UnifiedSortKey> sortKeys = srcTopLevelTypesOrdering.getSortKeys().stream()
                .map(JHarmonizerSortKey::getUnifiedSortKey)
                .toList();

        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(srcTopLevelTypesOrdering.isMainTypeFirst())
                .topLevelTypeSelectors(topLevelTypeSelectors)
                .sortKeys(sortKeys)
                .build();
    }
}
