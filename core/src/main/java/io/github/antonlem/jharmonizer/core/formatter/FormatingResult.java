package io.github.antonlem.jharmonizer.core.formatter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class FormatingResult {
    @NonNull
    String formatedSourceCode;

    @NonNull
    FormatingStatistic formatingStatistic;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormatingResult that)) {
            return false;
        }

        return formatedSourceCode.equals(that.formatedSourceCode) && formatingStatistic.equals(that.formatingStatistic);
    }

    @Override
    public int hashCode() {
        int result = formatedSourceCode.hashCode();
        result = 31 * result + formatingStatistic.hashCode();
        return result;
    }
}
