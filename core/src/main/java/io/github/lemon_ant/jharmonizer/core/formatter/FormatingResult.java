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
public class FormatingResult {
    @NonNull
    String formatedSrcCode;

    @NonNull
    FormatingStatistic formatingStatistic;

    /**
     * Checks whether this formating result matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormatingResult that)) {
            return false;
        }

        return formatedSrcCode.equals(that.formatedSrcCode) && formatingStatistic.equals(that.formatingStatistic);
    }

    /**
     * Returns the hash code of this formating result.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = formatedSrcCode.hashCode();
        result = 31 * result + formatingStatistic.hashCode();
        return result;
    }
}
