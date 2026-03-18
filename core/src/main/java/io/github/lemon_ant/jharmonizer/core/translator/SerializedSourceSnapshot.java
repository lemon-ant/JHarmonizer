package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
public class SerializedSourceSnapshot {
    @NonNull
    List<@NonNull SourceCharacterRange> formattingSkippedRanges;

    @NonNull
    String serializedSrcCode;

    public SerializedSourceSnapshot(
            @NonNull String serializedSrcCode, @NonNull List<@NonNull SourceCharacterRange> formattingSkippedRanges) {
        this.serializedSrcCode = serializedSrcCode;
        this.formattingSkippedRanges = Collections.unmodifiableList(formattingSkippedRanges);
    }
}
