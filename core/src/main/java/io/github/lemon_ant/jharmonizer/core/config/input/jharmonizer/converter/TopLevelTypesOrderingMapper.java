package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static java.util.stream.Collectors.toUnmodifiableSet;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.TopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.TypeKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeGroup;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Maps vendor TopLevelTypesOrdering to unified. */
@UtilityClass
class TopLevelTypesOrderingMapper {

    static UnifiedTopLevelTypesOrdering map(@NonNull TopLevelTypesOrdering srcTopLevelTypesOrdering) {
        List<UnifiedTypeGroup> typeGroups = srcTopLevelTypesOrdering.getTypeGroups().stream()
                .map(typeGroup -> new UnifiedTypeGroup(typeGroup.getTypeKinds().stream()
                        .map(TypeKind::getUnifiedTypeKind)
                        .collect(toUnmodifiableSet())))
                .toList();

        List<UnifiedSortKey> sortKeys = srcTopLevelTypesOrdering.getSortKeys().stream()
                .map(SortKey::getUnifiedSortKey)
                .toList();

        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(srcTopLevelTypesOrdering.isMainTypeFirst())
                .typeGroups(typeGroups)
                .sortKeys(sortKeys)
                .build();
    }
}
