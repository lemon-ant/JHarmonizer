package io.github.lemon_ant.jharmonizer.core.optout;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
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
            @NonNull SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges) {
        Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges =
                serializedSourceWithSkippedTypeRanges.getSortingSkippedTypeRanges();
        return optOuts.getTypeOptOutModes().entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .map(Map.Entry::getKey)
                .map(sortingSkippedTypeRanges::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Resolves original-source ranges for fully-off types when formatting runs on the original source text.
     *
     * @param optOuts the resolved opt-out modes for the source file
     * @param srcCode the original source text that serves as the formatting base
     * @return the original-source ranges keyed by fully-off types
     */
    @NonNull
    public Map<CtType<?>, SrcCharacterRange> resolveFullyOffTypeRanges(
            @NonNull JHarmonizerOptOuts optOuts, @NonNull String srcCode) {
        return optOuts.getTypeOptOutModes().entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF)
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> resolveOriginalTypeRange(entry.getKey(), srcCode)));
    }

    @NonNull
    private static SrcCharacterRange resolveOriginalTypeRange(CtType<?> type, String srcCode) {
        // Match the preserved-fragment start used by the custom Spoon printer so original-source reuse
        // excludes the same leading indentation from formatter rewrites.
        int originalStart = findIndentationStart(type.getPosition().getSourceStart(), srcCode);
        int originalEndExclusive = type.getPosition().getSourceEnd() + 1;
        if (originalEndExclusive > srcCode.length()) {
            throw new IllegalStateException("Invalid type source range: start="
                    + originalStart
                    + ", endExclusive="
                    + originalEndExclusive
                    + ", sourceLength="
                    + srcCode.length());
        }
        return new SrcCharacterRange(originalStart, originalEndExclusive);
    }

    private static int findIndentationStart(int start, String srcCode) {
        // Walk backward over spaces/tabs to include the full line indentation in the excluded range.
        int position = start - 1;
        while (position >= 0) {
            char character = srcCode.charAt(position);
            if (character != '\t' && character != ' ') {
                break;
            }
            position--;
        }
        return position + 1;
    }
}
