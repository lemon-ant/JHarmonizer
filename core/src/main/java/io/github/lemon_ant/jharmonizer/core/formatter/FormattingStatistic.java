package io.github.lemon_ant.jharmonizer.core.formatter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Timing and size statistics collected during a single source-file formatting pass.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FormattingStatistic {
    long formattedCodeLength;
    long formattingTimeInNanos;

    public static FormattingStatistic skipped(long formattedCodeLength) {
        return new FormattingStatistic(formattedCodeLength, 0);
    }
}
