// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
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
            @NonNull List<@NonNull JHarmonizerOrderingRule> orderingRules) {
        return new JHarmonizerTopLevelTypesOrdering(mainTypeFirst, typeGroups, orderingRules);
    }

    public static JHarmonizerTopLevelTypeSelector createTypeGroup(
            @NonNull Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        return new JHarmonizerTopLevelTypeSelector(typeKinds);
    }

    public static JHarmonizerFormatting createFormatting(boolean fixImports, @NonNull FormatterStyle formatterStyle) {
        return new JHarmonizerFormatting(fixImports, formatterStyle, true, true, false);
    }

    /**
     * Creates a formatting section with explicit blank-line flags.
     *
     * @param fixImports whether to fix imports
     * @param formatterStyle the formatter style
     * @param blankLineAfterTypeHeader blank line after type header flag
     * @param blankLineBeforeComment blank line before comment flag
     * @param blankLineBetweenFields blank line between fields flag
     * @return the formatting section
     */
    public static JHarmonizerFormatting createFormatting(
            boolean fixImports,
            @NonNull FormatterStyle formatterStyle,
            boolean blankLineAfterTypeHeader,
            boolean blankLineBeforeComment,
            boolean blankLineBetweenFields) {
        return new JHarmonizerFormatting(
                fixImports, formatterStyle, blankLineAfterTypeHeader, blankLineBeforeComment, blankLineBetweenFields);
    }
}
