package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum TypeKind {
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD;

    @JsonCreator
    static TypeKind fromString(String value) {
        return TypeKind.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
}
