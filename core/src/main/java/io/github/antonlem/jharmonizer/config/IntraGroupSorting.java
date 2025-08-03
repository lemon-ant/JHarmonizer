package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import lombok.NonNull;

public enum IntraGroupSorting {
    ALPHA,
    PRESERVE,
    SIGNATURE,
    VISIBILITY_ASC,
    VISIBILITY_DESC,
    ;

    @NonNull
    @JsonCreator
    static IntraGroupSorting fromString(@NonNull String value) {
        return IntraGroupSorting.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
}
