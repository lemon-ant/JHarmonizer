package io.github.lemon_ant.jharmonizer.core.optout;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;

@UtilityClass
public class OptOutFormattingRangeResolver {

    /**
     * Resolves formatting exclusion ranges for fully skipped types.
     *
     * @param optOuts the resolved opt-out modes for the source file
     * @param serializedSourceWithSkippedTypeRanges the serialized source snapshot with preserved type ranges
     * @return the source ranges that must stay untouched by the formatter
     */
    @NonNull
    public List<@NonNull SrcCharacterRange> resolveFormattingSkippedRanges(
            @NonNull JHarmonizerOptOuts optOuts,
            @NonNull SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges) {
        Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges =
                serializedSourceWithSkippedTypeRanges.getSortingSkippedTypeRanges();
        return optOuts.getTypeOptOutModes().entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .map(Map.Entry::getKey)
                .map(sortingSkippedTypeRanges::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SrcCharacterRange::getStartInclusive))
                .toList();
    }
}
