package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.NonNull;
import lombok.Value;

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
}
