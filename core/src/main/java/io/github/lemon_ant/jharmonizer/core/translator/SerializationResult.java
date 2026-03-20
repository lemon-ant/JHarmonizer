package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of serializing a sorted Spoon AST back to Java source code.
 * Bundles the serialized source payload with the associated timing and size statistics.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SerializationResult {
    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges;

    @NonNull
    public static SerializationResult skippedWithoutSerialization(
            @NonNull SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges) {
        return new SerializationResult(
                new SerializationStatistic(
                        serializedSourceWithSkippedTypeRanges
                                .getSerializedSrcCode()
                                .length(),
                        0),
                serializedSourceWithSkippedTypeRanges);
    }
}
