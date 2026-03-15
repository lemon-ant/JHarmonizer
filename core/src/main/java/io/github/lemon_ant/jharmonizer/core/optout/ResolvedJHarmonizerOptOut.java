package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;

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
    ResolvedOptOutTargetType targetType;

    public boolean skipsFormatting() {
        return mode.skipsFormatting();
    }

    public boolean skipsSorting() {
        return switch (mode) {
            case OFF, SORT_OFF -> true;
        };
    }

    @NonNull
    public Optional<SourceCharacterRange> getPreservedSourceRange() {
        return Optional.ofNullable(preservedSourceRange);
    }

    @NonNull
    public Optional<ResolvedOptOutTargetType> getTargetType() {
        return Optional.ofNullable(targetType);
    }
}
