package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;

public enum FormatterStyle {
    AOP,
    GOOGLE,
    NONE,
    PALANTIR,
    ;

    @NonNull
    @JsonCreator
    static FormatterStyle fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(FormatterStyle.class, value);
    }
}
