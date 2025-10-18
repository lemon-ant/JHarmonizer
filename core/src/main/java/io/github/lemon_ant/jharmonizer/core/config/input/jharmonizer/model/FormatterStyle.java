package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormatterStyle {
    AOP(UnifiedFormatterStyle.AOSP),
    GOOGLE(UnifiedFormatterStyle.GOOGLE),
    NONE(UnifiedFormatterStyle.NONE),
    PALANTIR(UnifiedFormatterStyle.PALANTIR),
    ;

    private final UnifiedFormatterStyle unifiedFormatterStyle;

    @NonNull
    @JsonCreator
    static FormatterStyle fromString(@NonNull String value) {
        return EnumDeserializerUtil.deserialize(FormatterStyle.class, value);
    }
}
