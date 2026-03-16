package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JHarmonizerOptOuts {
    private static final JHarmonizerOptOuts EMPTY_OPT_OUTS = new JHarmonizerOptOuts(null, Map.of());

    @Nullable
    ResolvedJHarmonizerOptOut fileOptOut;

    @NonNull
    Map<SourcePosition, ResolvedJHarmonizerOptOut> typeOptOutsByTargetPosition;

    @NonNull
    public static JHarmonizerOptOuts empty() {
        return EMPTY_OPT_OUTS;
    }

    @NonNull
    public static JHarmonizerOptOuts of(
            @Nullable ResolvedJHarmonizerOptOut fileOptOut,
            @NonNull Map<SourcePosition, ResolvedJHarmonizerOptOut> typeOptOutsByTargetPosition) {
        return new JHarmonizerOptOuts(fileOptOut, Map.copyOf(typeOptOutsByTargetPosition));
    }

    public boolean hasFileOptOutMode(@NonNull JHarmonizerOptOutMode mode) {
        return getFileOptOut()
                .map(ResolvedJHarmonizerOptOut::getMode)
                .filter(mode::equals)
                .isPresent();
    }

    public boolean isEmpty() {
        return fileOptOut == null && typeOptOutsByTargetPosition.isEmpty();
    }

    @NonNull
    public Optional<ResolvedJHarmonizerOptOut> findTypeOptOut(@NonNull SourcePosition sourcePosition) {
        return Optional.ofNullable(typeOptOutsByTargetPosition.get(sourcePosition));
    }

    @NonNull
    public Optional<ResolvedJHarmonizerOptOut> findTypeOptOut(@NonNull CtType<?> type) {
        return findTypeOptOut(type.getPosition());
    }

    @NonNull
    public Optional<ResolvedJHarmonizerOptOut> getFileOptOut() {
        return Optional.ofNullable(fileOptOut);
    }

    @NonNull
    public Set<CtType<?>> getFormattingSkippedTypes() {
        return typeOptOutsByTargetPosition.values().stream()
                .filter(ResolvedJHarmonizerOptOut::skipsFormatting)
                .map(ResolvedJHarmonizerOptOut::getTargetType)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @NonNull
    public Set<CtType<?>> getSortingSkippedTypes() {
        return typeOptOutsByTargetPosition.values().stream()
                .map(ResolvedJHarmonizerOptOut::getTargetType)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
