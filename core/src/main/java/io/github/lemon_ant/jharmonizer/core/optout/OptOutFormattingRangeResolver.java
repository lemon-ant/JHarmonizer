package io.github.lemon_ant.jharmonizer.core.optout;

import static java.util.stream.Collectors.toUnmodifiableList;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;

@UtilityClass
public class OptOutFormattingRangeResolver {

    /**
     * Resolves formatting exclusion ranges for fully-off types.
     *
     * @param optOuts the resolved opt-out modes for the source file
     * @param serializedSourceWithSkippedTypeRanges the serialized source snapshot with preserved type ranges
     * @return the source ranges that must stay untouched by the formatter
     */
    @NonNull
    public List<@NonNull SrcCharacterRange> resolveFormattingSkippedRanges(
            @NonNull JHarmonizerOptOuts optOuts,
            @NonNull SerializedSrcWithSkippedTypeRanges serializedSrcWithSkippedTypeRanges) {
        Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges =
                serializedSrcWithSkippedTypeRanges.getSortingSkippedTypeRanges();
        return optOuts.getTypeOptOutModes().entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .map(Map.Entry::getKey)
                .map(sortingSkippedTypeRanges::get)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableList());
    }

    /**
     * Resolves original-source ranges for fully-off types when formatting runs on the original source text.
     *
     * @param optOuts the resolved opt-out modes for the source file
     * @param originalSrcCode the original source text that serves as the formatting base
     * @return the original-source ranges keyed by fully-off types
     */
    @NonNull
    public Map<CtType<?>, SrcCharacterRange> resolveFullyOffTypeRanges(
            @NonNull JHarmonizerOptOuts optOuts, @NonNull String originalSrcCode) {
        return optOuts.getTypeOptOutModes().entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> resolveOriginalTypeRange(entry.getKey(), originalSrcCode)));
    }

    @NonNull
    private static SrcCharacterRange resolveOriginalTypeRange(CtType<?> type, String originalSrcCode) {
        // Match the preserved-fragment start used by the custom Spoon printer so original-source reuse
        // excludes the same leading indentation from formatter rewrites.
        int originalStart = SrcCodeUtils.findIndentationStart(type.getPosition().getSourceStart(), originalSrcCode);
        int originalEndExclusive = type.getPosition().getSourceEnd() + 1;
        if (originalEndExclusive > originalSrcCode.length()) {
            throw new IllegalStateException("Invalid type source range: start="
                    + originalStart
                    + ", endExclusive="
                    + originalEndExclusive
                    + ", sourceLength="
                    + originalSrcCode.length());
        }
        return new SrcCharacterRange(originalStart, originalEndExclusive);
    }
}
