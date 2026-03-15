package io.github.lemon_ant.jharmonizer.core.directive;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@Value
public class ResolvedDirectiveTargetType {
    @NonNull
    String qualifiedName;

    @NonNull
    String simpleName;

    @NonNull
    SourcePosition sourcePosition;

    @NonNull
    public static ResolvedDirectiveTargetType from(@NonNull CtType<?> targetType) {
        return new ResolvedDirectiveTargetType(
                targetType.getQualifiedName(), targetType.getSimpleName(), targetType.getPosition());
    }
}
