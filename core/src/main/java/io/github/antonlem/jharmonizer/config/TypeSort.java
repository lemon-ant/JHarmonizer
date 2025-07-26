package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum TypeSort {
    ALPHA,
    PRESERVE;

    @JsonCreator
    static TypeSort fromRaw(String raw) {
        return switch (raw.toLowerCase(Locale.ENGLISH)) {
            case "alpha" -> ALPHA;
            case "preserve" -> PRESERVE;
            default -> throw new IllegalArgumentException("Unknown type-sort value: " + raw);
        };
    }
}
