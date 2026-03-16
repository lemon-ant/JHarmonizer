package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@Value
public class ResolvedJHarmonizerOptOut {
    @NonNull
    SourcePosition commentPosition;

    @NonNull
    JHarmonizerOptOutMode mode;

    @Nullable
    SourceCharacterRange preservedSourceRange;

    @NonNull
    JHarmonizerOptOutScope scope;

    @Nullable
    CtType<?> targetType;

    public boolean skipsFormatting() {
        return mode.skipsFormatting();
    }

    @NonNull
    public Optional<SourceCharacterRange> getPreservedSourceRange() {
        return Optional.ofNullable(preservedSourceRange);
    }

    @NonNull
    public Optional<CtType<?>> getTargetType() {
        return Optional.ofNullable(targetType);
    }
}
