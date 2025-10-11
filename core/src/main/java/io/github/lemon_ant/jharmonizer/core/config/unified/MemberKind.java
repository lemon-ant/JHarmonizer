package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Unifies members and nested types; each constant binds to a TargetCategory.
 */
@Getter
@RequiredArgsConstructor
public enum MemberKind {
    FIELD(TargetCategory.FIELD),
    METHOD(TargetCategory.METHOD),
    CONSTRUCTOR(TargetCategory.CONSTRUCTOR),

    // Initializer blocks:
    STATIC_INIT_BLOCK(TargetCategory.INIT_BLOCK),
    INSTANCE_INIT_BLOCK(TargetCategory.INIT_BLOCK),

    // Non-block entries that must have names (distinct from init blocks):
    ENUM_CONSTANT(TargetCategory.ENUM_CONSTANT),
    RECORD_COMPONENT(TargetCategory.RECORD_COMPONENT),

    // Types:
    TYPE_CLASS(TargetCategory.TYPE),
    TYPE_INTERFACE(TargetCategory.TYPE),
    TYPE_ENUM(TargetCategory.TYPE),
    TYPE_RECORD(TargetCategory.TYPE),
    TYPE_ANNOTATION(TargetCategory.TYPE),
    ;

    private final TargetCategory targetCategory;

    public boolean isType() {
        return this.getTargetCategory().isType();
    }

    public boolean isInitializer() {
        return this.getTargetCategory().isInitializer();
    }
}
