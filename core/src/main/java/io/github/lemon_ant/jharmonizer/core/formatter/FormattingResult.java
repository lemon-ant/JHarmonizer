package io.github.lemon_ant.jharmonizer.core.formatter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of formatting one source file.
 * Bundles the formatted source code string with the associated timing and size statistics.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FormattingResult {
    @NonNull
    String formattedSrcCode;

    @NonNull
    FormattingStatistic formattingStatistic;
}
