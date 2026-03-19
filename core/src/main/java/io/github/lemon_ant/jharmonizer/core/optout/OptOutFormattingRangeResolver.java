package io.github.lemon_ant.jharmonizer.core.optout;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
        Set<CtType<?>> formattingSkippedTypes = findFormattingSkippedTypes(optOuts.getTypeOptOutModes());
        return serializedSourceWithSkippedTypeRanges.getSortingSkippedTypeRanges().entrySet().stream()
                .filter(entry -> formattingSkippedTypes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(SrcCharacterRange::getStartInclusive))
                .toList();
    }

    @NonNull
    private Set<CtType<?>> findFormattingSkippedTypes(Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        return typeOptOutModes.entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
