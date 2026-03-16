package io.github.lemon_ant.jharmonizer.core.formatter;

import edu.umd.cs.findbugs.annotations.Nullable;
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
public class FormatingResult {
    @NonNull
    String formatedSrcCode;

    @NonNull
    FormatingStatistic formatingStatistic;

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof FormatingResult that)) {
            return false;
        }

        return formatedSrcCode.equals(that.formatedSrcCode) && formatingStatistic.equals(that.formatingStatistic);
    }

    @Override
    public int hashCode() {
        int result = formatedSrcCode.hashCode();
        result = 31 * result + formatingStatistic.hashCode();
        return result;
    }
}
