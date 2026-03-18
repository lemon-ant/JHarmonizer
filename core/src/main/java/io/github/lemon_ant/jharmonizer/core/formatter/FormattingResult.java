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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormattingResult that)) {
            return false;
        }

        return formattedSrcCode.equals(that.formattedSrcCode) && formattingStatistic.equals(that.formattingStatistic);
    }

    @Override
    public int hashCode() {
        int result = formattedSrcCode.hashCode();
        result = 31 * result + formattingStatistic.hashCode();
        return result;
    }
}
