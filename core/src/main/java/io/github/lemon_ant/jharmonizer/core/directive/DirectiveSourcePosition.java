package io.github.lemon_ant.jharmonizer.core.directive;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;

@Value
public class DirectiveSourcePosition {
    int line;
    int column;
    int sourceEnd;
    int sourceStart;

    @NonNull
    public static DirectiveSourcePosition from(@NonNull SourcePosition sourcePosition) {
        return new DirectiveSourcePosition(
                sourcePosition.getLine(),
                sourcePosition.getColumn(),
                sourcePosition.getSourceEnd(),
                sourcePosition.getSourceStart());
    }
}
