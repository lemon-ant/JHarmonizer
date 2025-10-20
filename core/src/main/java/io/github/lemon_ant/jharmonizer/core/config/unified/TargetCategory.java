package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Coarse-grained target category that drives applicability of access and declaration modifiers.
 */
@Getter
@RequiredArgsConstructor
enum TargetCategory {
    FIELD(true),
    METHOD(true),
    CONSTRUCTOR(true),
    INIT_BLOCK(false), // static / instance initializer blocks
    ENUM_CONSTANT(false), // enum constant entries
    RECORD_COMPONENT(false), // record component entries
    TYPE(true), // all TYPE_* kinds
    ;

    /**
     * Whether explicit access level applies (must be provided).
     */
    private final boolean accessLevelApplicable;

    public boolean isInitializer() {
        return this == TargetCategory.INIT_BLOCK;
    }

    /**
     * True if this category represents a (nested) type.
     */
    public boolean isType() {
        return this == TYPE;
    }
}
