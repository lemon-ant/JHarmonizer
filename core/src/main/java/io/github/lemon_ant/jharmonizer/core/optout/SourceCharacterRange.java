package io.github.lemon_ant.jharmonizer.core.optout;

import static java.util.Comparator.comparingInt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class SourceCharacterRange {
    int startInclusive;
    int endExclusive;

    public SourceCharacterRange(int startInclusive, int endExclusive) {
        Validate.isTrue(startInclusive >= 0, "Range start must be non-negative: %s", startInclusive);
        Validate.isTrue(
                endExclusive >= startInclusive, "Range end must be >= start: %s < %s", endExclusive, startInclusive);
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
    }

    @NonNull
    public static List<SourceCharacterRange> invert(
            int sourceLength, @NonNull Collection<SourceCharacterRange> excludedRanges) {
        Validate.isTrue(sourceLength >= 0, "Source length must be non-negative: %s", sourceLength);

        List<SourceCharacterRange> normalizedRanges = excludedRanges.stream()
                .sorted(comparingInt(SourceCharacterRange::getStartInclusive)
                        .thenComparingInt(SourceCharacterRange::getEndExclusive))
                .toList();
        List<SourceCharacterRange> includedRanges = new ArrayList<>();
        int nextStart = 0;

        for (SourceCharacterRange excludedRange : normalizedRanges) {
            Validate.isTrue(
                    excludedRange.getEndExclusive() <= sourceLength,
                    "Excluded range [%s, %s) exceeds source length %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    sourceLength);
            Validate.isTrue(
                    excludedRange.getStartInclusive() >= nextStart,
                    "Excluded ranges must be sorted and non-overlapping, but [%s, %s) overlaps before %s",
                    excludedRange.getStartInclusive(),
                    excludedRange.getEndExclusive(),
                    nextStart);

            if (nextStart < excludedRange.getStartInclusive()) {
                includedRanges.add(new SourceCharacterRange(nextStart, excludedRange.getStartInclusive()));
            }
            nextStart = excludedRange.getEndExclusive();
        }

        if (nextStart < sourceLength) {
            includedRanges.add(new SourceCharacterRange(nextStart, sourceLength));
        }

        return List.copyOf(includedRanges);
    }
}
