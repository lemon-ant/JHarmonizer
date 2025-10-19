package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UnifiedTypeKind {
    CLASS(MemberKind.TYPE_CLASS),
    INTERFACE(MemberKind.TYPE_INTERFACE),
    ENUM(MemberKind.TYPE_ENUM),
    ANNOTATION(MemberKind.TYPE_ANNOTATION),
    RECORD(MemberKind.TYPE_RECORD),
    ;

    private final MemberKind memberKind;
}
