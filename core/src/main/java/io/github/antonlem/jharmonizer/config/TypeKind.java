package io.github.antonlem.jharmonizer.config;

import java.util.Locale;

public enum TypeKind {
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD;

    static TypeKind fromRaw(String raw) {
        return switch (raw.toLowerCase(Locale.ENGLISH)) {
            case "class" -> CLASS;
            case "interface" -> INTERFACE;
            case "enum" -> ENUM;
            case "annotation" -> ANNOTATION;
            case "record" -> RECORD;
            default -> throw new IllegalArgumentException("Unsupported type: " + raw);
        };
    }
}
