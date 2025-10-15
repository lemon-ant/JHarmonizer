package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CreationHelper {
    public static HeaderLine createHeaderLine(char character, int leftPadding) {
        return new HeaderLine(character, leftPadding);
    }

    public static TypeGroup createTypeGroup(@NonNull Set<@NonNull TypeKind> typeKinds) {
        return new TypeGroup(typeKinds);
    }

    public static TopLevelTypesOrdering createTopLevelTypesOrdering(
            boolean mainTypeFirst, @NonNull List<@NonNull TypeGroup> typeGroups, @NonNull List<SortKey> sortKeys) {
        return new TopLevelTypesOrdering(mainTypeFirst, typeGroups, sortKeys);
    }
}
