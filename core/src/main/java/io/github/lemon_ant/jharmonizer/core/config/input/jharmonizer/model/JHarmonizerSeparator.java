package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;

public enum JHarmonizerSeparator {
    NEW_LINE,
    HEADER,
    NONE;

    @NonNull
    @JsonCreator
    static JHarmonizerSeparator fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(JHarmonizerSeparator.class, value);
    }
}
