package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
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
    Map<@NonNull CtType<?>, @NonNull SourceCharacterRange> sortingSkippedTypeRanges;

    public SerializedSourceWithSkippedTypeRanges(
            @NonNull String serializedSrcCode,
            @NonNull Map<@NonNull CtType<?>, @NonNull SourceCharacterRange> sortingSkippedTypeRanges) {
        this.serializedSrcCode = serializedSrcCode;
        this.sortingSkippedTypeRanges = Collections.unmodifiableMap(sortingSkippedTypeRanges);
    }
}
