package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtType;

/**
 * Result of serializing a sorted Spoon AST back to Java source code.
 * Bundles the serialized source payload with the associated timing and size statistics.
 */
@Value
public class SerializationResult {
    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges;

    public SerializationResult(
            @NonNull SerializationStatistic serializationStatistic,
            @NonNull SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges) {
        this.serializationStatistic = serializationStatistic;
        this.serializedSourceWithSkippedTypeRanges = serializedSourceWithSkippedTypeRanges;
    }

    /**
     * Resolves formatter exclusion ranges for the specified fully skipped types.
     *
     * @param formattingSkippedTypes the types whose preserved source fragments must not be reformatted
     * @return the source ranges to exclude from formatting
     */
    @NonNull
    public List<@NonNull SourceCharacterRange> getFormattingSkippedRanges(
            @NonNull Set<CtType<?>> formattingSkippedTypes) {
        return serializedSourceWithSkippedTypeRanges.getSortingSkippedTypeRanges().entrySet().stream()
                .filter(entry -> formattingSkippedTypes.contains(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .sorted(Comparator.comparingInt(SourceCharacterRange::getStartInclusive))
                .toList();
    }
}
