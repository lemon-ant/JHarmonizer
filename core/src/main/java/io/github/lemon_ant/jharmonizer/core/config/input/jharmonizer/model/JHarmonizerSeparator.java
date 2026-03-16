package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Separator style values supported in the JHarmonizer YAML config for member groups.
 * Each constant maps to a corresponding {@link UnifiedSeparator}.
 */
@Getter
@RequiredArgsConstructor
public enum JHarmonizerSeparator {
    NEW_LINE(UnifiedSeparator.NEW_LINE),
    HEADER(UnifiedSeparator.HEADER),
    NONE(UnifiedSeparator.NONE);

    private final UnifiedSeparator unifiedSeparator;

    @NonNull
    @JsonCreator
    static JHarmonizerSeparator fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(JHarmonizerSeparator.class, value);
    }
}
