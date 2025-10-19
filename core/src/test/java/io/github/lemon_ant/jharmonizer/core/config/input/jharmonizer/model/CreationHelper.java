package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CreationHelper {
    public static JHarmonizerHeaderLine createHeaderLine(char character, int leftPadding) {
        return new JHarmonizerHeaderLine(character, leftPadding);
    }

    public static JHarmonizerTopLevelTypeSelector createTypeGroup(
            @NonNull Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        return new JHarmonizerTopLevelTypeSelector(typeKinds);
    }

    public static JHarmonizerTopLevelTypesOrdering createTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull List<@NonNull JHarmonizerTopLevelTypeSelector> typeGroups,
            @NonNull List<JHarmonizerSortKey> sortKeys) {
        return new JHarmonizerTopLevelTypesOrdering(mainTypeFirst, typeGroups, sortKeys);
    }
}
