package io.github.lemon_ant.jharmonizer.core.translator;

import io.github.lemon_ant.jharmonizer.core.directive.SourceCharacterRange;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

@Value
public class SerializationResult {
    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    String serializedSrcCode;

    @NonNull
    List<SourceCharacterRange> formattingExclusionRanges;

    public SerializationResult(
            @NonNull SerializationStatistic serializationStatistic,
            @NonNull String serializedSrcCode,
            @NonNull List<SourceCharacterRange> formattingExclusionRanges) {
        this.serializationStatistic = serializationStatistic;
        this.serializedSrcCode = serializedSrcCode;
        this.formattingExclusionRanges = List.copyOf(formattingExclusionRanges);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SerializationResult) obj;
        return Objects.equals(this.serializedSrcCode, that.serializedSrcCode)
                && Objects.equals(this.serializationStatistic, that.serializationStatistic)
                && Objects.equals(this.formattingExclusionRanges, that.formattingExclusionRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serializedSrcCode, serializationStatistic, formattingExclusionRanges);
    }
}
