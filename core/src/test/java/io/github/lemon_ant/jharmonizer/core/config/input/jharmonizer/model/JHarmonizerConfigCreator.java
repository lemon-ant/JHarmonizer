package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
// TODO Rename
public class JHarmonizerConfigCreator {
    public static JHarmonizerHeaderLine createHeaderLine(char character, int leftPadding) {
        return new JHarmonizerHeaderLine(character, leftPadding);
    }

    public static JHarmonizerTopLevelTypesOrdering createTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull List<@NonNull JHarmonizerTopLevelTypeSelector> typeGroups,
            @NonNull List<@NonNull JHarmonizerSortKey> sortKeys) {
        return new JHarmonizerTopLevelTypesOrdering(mainTypeFirst, typeGroups, sortKeys);
    }

    public static JHarmonizerTopLevelTypeSelector createTypeGroup(
            @NonNull Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        return new JHarmonizerTopLevelTypeSelector(typeKinds);
    }

    public static JHarmonizerFormatting createFormatting(boolean fixImports, @NonNull FormatterStyle formatterStyle) {
        return new JHarmonizerFormatting(fixImports, formatterStyle);
    }
}
