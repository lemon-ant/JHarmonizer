package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;

public enum Separator {
    NEW_LINE,
    HEADER,
    NONE;

    @NonNull
    @JsonCreator
    static Separator fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(Separator.class, value);
    }
}
