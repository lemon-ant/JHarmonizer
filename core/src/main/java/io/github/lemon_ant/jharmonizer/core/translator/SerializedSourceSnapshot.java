package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
public class SerializedSourceSnapshot {
    @NonNull
    List<@NonNull SourceCharacterRange> formattingExclusionRanges;

    @NonNull
    String sourceCode;

    public SerializedSourceSnapshot(
            @NonNull String sourceCode, @NonNull List<@NonNull SourceCharacterRange> formattingExclusionRanges) {
        this.sourceCode = sourceCode;
        this.formattingExclusionRanges = Collections.unmodifiableList(formattingExclusionRanges);
    }
}
