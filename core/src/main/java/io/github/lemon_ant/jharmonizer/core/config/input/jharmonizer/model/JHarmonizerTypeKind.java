package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerTypeKind {
    CLASS(UnifiedTypeKind.CLASS),
    INTERFACE(UnifiedTypeKind.INTERFACE),
    ENUM(UnifiedTypeKind.ENUM),
    ANNOTATION(UnifiedTypeKind.ANNOTATION),
    RECORD(UnifiedTypeKind.RECORD),
    ;

    private final UnifiedTypeKind unifiedTypeKind;
}
