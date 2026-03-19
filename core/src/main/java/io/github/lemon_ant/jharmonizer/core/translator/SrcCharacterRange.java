package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class SrcCharacterRange {
    int startInclusive;
    int endExclusive;

    @NonNull
    public static SrcCharacterRange of(int startInclusive, int endExclusive) {
        return new SrcCharacterRange(startInclusive, endExclusive);
    }

    SrcCharacterRange(int startInclusive, int endExclusive) {
        Validate.isTrue(startInclusive >= 0, "Range start must be non-negative: %s", startInclusive);
        Validate.isTrue(
                endExclusive >= startInclusive, "Range end must be >= start: %s < %s", endExclusive, startInclusive);
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
    }
}
