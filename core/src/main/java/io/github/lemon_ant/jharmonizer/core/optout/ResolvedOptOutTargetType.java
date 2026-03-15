package io.github.lemon_ant.jharmonizer.core.optout;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@Value
public class ResolvedOptOutTargetType {
    @NonNull
    String qualifiedName;

    @NonNull
    String simpleName;

    @NonNull
    SourcePosition sourcePosition;

    @NonNull
    public static ResolvedOptOutTargetType from(@NonNull CtType<?> targetType) {
        return new ResolvedOptOutTargetType(
                targetType.getQualifiedName(), targetType.getSimpleName(), targetType.getPosition());
    }
}
