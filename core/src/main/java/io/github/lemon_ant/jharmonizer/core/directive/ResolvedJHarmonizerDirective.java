package io.github.lemon_ant.jharmonizer.core.directive;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

@Value
public class ResolvedJHarmonizerDirective {
    @NonNull
    DirectiveSourcePosition directivePosition;

    @NonNull
    JHarmonizerDirectiveMode mode;

    @Nullable
    SourceCharacterRange preservedSourceRange;

    @NonNull
    JHarmonizerDirectiveScope scope;

    @Nullable
    ResolvedDirectiveTargetType targetType;

    public boolean skipsFormatting() {
        return mode.skipsFormatting();
    }

    public boolean skipsSorting() {
        return true;
    }

    @NonNull
    public Optional<SourceCharacterRange> getPreservedSourceRange() {
        return Optional.ofNullable(preservedSourceRange);
    }

    @NonNull
    public Optional<ResolvedDirectiveTargetType> getTargetType() {
        return Optional.ofNullable(targetType);
    }
}
