package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.directive.SourceCharacterRange;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
public class SerializedSourceSnapshot {
    @NonNull
    List<SourceCharacterRange> formattingExclusionRanges;

    @NonNull
    String sourceCode;

    public SerializedSourceSnapshot(
            @NonNull String sourceCode, @NonNull List<SourceCharacterRange> formattingExclusionRanges) {
        this.sourceCode = sourceCode;
        this.formattingExclusionRanges = List.copyOf(formattingExclusionRanges);
    }
}
