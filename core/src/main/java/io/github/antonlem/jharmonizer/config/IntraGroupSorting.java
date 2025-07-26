package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum IntraGroupSorting {
    ALPHA,
    PRESERVE;

    @JsonCreator
    static IntraGroupSorting fromString(String value) {
        return IntraGroupSorting.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
}
