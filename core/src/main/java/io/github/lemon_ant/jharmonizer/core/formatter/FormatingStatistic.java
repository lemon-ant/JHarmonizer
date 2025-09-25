package io.github.lemon_ant.jharmonizer.core.formatter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FormatingStatistic {
    long formattedCodeLength;
    long formattingTimeInNanos;
}
