package io.github.lemon_ant.jharmonizer.core.common;

import static java.util.Comparator.comparingInt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class SrcCharacterRange {
    int startInclusive;
    int endExclusive;

    public SrcCharacterRange(int startInclusive, int endExclusive) {
        Validate.isTrue(startInclusive >= 0, "Range start must be non-negative: %s", startInclusive);
        Validate.isTrue(
                endExclusive >= startInclusive, "Range end must be >= start: %s < %s", endExclusive, startInclusive);
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
    }

    @NonNull
    public static List<SrcCharacterRange> invert(
            int sourceLength, @NonNull Collection<SrcCharacterRange> excludedRanges) {
        Validate.isTrue(sourceLength >= 0, "Source length must be non-negative: %s", sourceLength);

        List<SrcCharacterRange> normalizedRanges = excludedRanges.stream()
                .sorted(comparingInt(SrcCharacterRange::getStartInclusive)
                        .thenComparingInt(SrcCharacterRange::getEndExclusive))
                .toList();
        List<SrcCharacterRange> includedRanges = new ArrayList<>();
        int nextStart = 0;

        for (SrcCharacterRange excludedRange : normalizedRanges) {
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
                includedRanges.add(new SrcCharacterRange(nextStart, excludedRange.getStartInclusive()));
            }
            nextStart = excludedRange.getEndExclusive();
        }

        if (nextStart < sourceLength) {
            includedRanges.add(new SrcCharacterRange(nextStart, sourceLength));
        }

        return Collections.unmodifiableList(includedRanges);
    }
}
