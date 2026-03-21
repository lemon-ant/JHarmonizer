package io.github.lemon_ant.jharmonizer.core.formatter;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Timing and size statistics collected during a single source-file formatting pass.
 */
@Value
@AllArgsConstructor
public class FormattingStatistic {
    long formattedCodeLength;
    long formattingTimeInNanos;
}
