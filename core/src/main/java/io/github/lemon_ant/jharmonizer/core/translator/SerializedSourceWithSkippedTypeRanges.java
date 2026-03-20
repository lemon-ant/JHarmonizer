package io.github.lemon_ant.jharmonizer.core.translator;

import java.util.Collections;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtType;

@Value
public class SerializedSourceWithSkippedTypeRanges {
    @NonNull
    String serializedSrcCode;

    @NonNull
    Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges;

    @NonNull
    public static SerializedSourceWithSkippedTypeRanges of(
            @NonNull String serializedSrcCode,
            @NonNull Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges) {
        return new SerializedSourceWithSkippedTypeRanges(
                serializedSrcCode, Collections.unmodifiableMap(sortingSkippedTypeRanges));
    }

    private SerializedSourceWithSkippedTypeRanges(
            @NonNull String serializedSrcCode,
            @NonNull Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges) {
        this.serializedSrcCode = serializedSrcCode;
        this.sortingSkippedTypeRanges = sortingSkippedTypeRanges;
    }
}
