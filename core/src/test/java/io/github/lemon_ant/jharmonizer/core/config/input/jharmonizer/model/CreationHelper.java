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

    public static JHarmonizerTypeGroup createTypeGroup(@NonNull Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        return new JHarmonizerTypeGroup(typeKinds);
    }

    public static JHarmonizerTopLevelTypesOrdering createTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull List<@NonNull JHarmonizerTypeGroup> typeGroups,
            @NonNull List<JHarmonizerSortKey> sortKeys) {
        return new JHarmonizerTopLevelTypesOrdering(mainTypeFirst, typeGroups, sortKeys);
    }
}
