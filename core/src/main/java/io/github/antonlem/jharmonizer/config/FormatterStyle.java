package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum FormatterStyle {
    AOP,
    GOOGLE,
    NONE,
    PALANTIR;

    @JsonCreator
    public static FormatterStyle fromString(String value) {
        return FormatterStyle.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
}
