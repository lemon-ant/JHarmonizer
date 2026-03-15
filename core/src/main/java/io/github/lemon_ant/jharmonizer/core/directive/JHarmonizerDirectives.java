package io.github.lemon_ant.jharmonizer.core.directive;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JHarmonizerDirectives {
    private static final JHarmonizerDirectives EMPTY = new JHarmonizerDirectives(null, Map.of());

    @Nullable
    ResolvedJHarmonizerDirective fileDirective;

    @NonNull
    Map<SourcePosition, ResolvedJHarmonizerDirective> typeDirectivesByTargetPosition;

    @NonNull
    public static JHarmonizerDirectives empty() {
        return EMPTY;
    }

    @NonNull
    public static JHarmonizerDirectives of(
            @Nullable ResolvedJHarmonizerDirective fileDirective,
            @NonNull Map<SourcePosition, ResolvedJHarmonizerDirective> typeDirectivesByTargetPosition) {
        return new JHarmonizerDirectives(fileDirective, Map.copyOf(typeDirectivesByTargetPosition));
    }

    public boolean hasFileDirectiveMode(@NonNull JHarmonizerDirectiveMode mode) {
        return getFileDirective()
                .map(ResolvedJHarmonizerDirective::getMode)
                .filter(mode::equals)
                .isPresent();
    }

    public boolean isEmpty() {
        return fileDirective == null && typeDirectivesByTargetPosition.isEmpty();
    }

    public boolean isFormattingSkippedForType(@NonNull CtType<?> type) {
        return findTypeDirective(type)
                .map(ResolvedJHarmonizerDirective::skipsFormatting)
                .orElse(false);
    }

    public boolean isSortingSkippedForType(@NonNull CtType<?> type) {
        return findTypeDirective(type).isPresent();
    }

    @NonNull
    public Optional<ResolvedJHarmonizerDirective> findTypeDirective(@NonNull CtType<?> type) {
        return findTypeDirective(type.getPosition());
    }

    @NonNull
    public Optional<ResolvedJHarmonizerDirective> findTypeDirective(@NonNull SourcePosition sourcePosition) {
        return Optional.ofNullable(typeDirectivesByTargetPosition.get(sourcePosition));
    }

    @NonNull
    public Optional<ResolvedJHarmonizerDirective> getFileDirective() {
        return Optional.ofNullable(fileDirective);
    }

    @NonNull
    public List<ResolvedJHarmonizerDirective> getFormattingSkippedTypeDirectives() {
        return typeDirectivesByTargetPosition.values().stream()
                .filter(ResolvedJHarmonizerDirective::skipsFormatting)
                .toList();
    }
}
