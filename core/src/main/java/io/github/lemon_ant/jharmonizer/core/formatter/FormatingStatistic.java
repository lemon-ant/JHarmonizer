package io.github.lemon_ant.jharmonizer.core.formatter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FormatingStatistic {
    long formattedCodeLength;
    long formattingTimeInNanos;
}
