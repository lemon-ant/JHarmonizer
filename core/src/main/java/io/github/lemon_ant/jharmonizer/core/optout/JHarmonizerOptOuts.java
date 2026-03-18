package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtType;

@Value
public class JHarmonizerOptOuts {
    private static final JHarmonizerOptOuts EMPTY_OPT_OUTS = new JHarmonizerOptOuts(null, Map.of());

    @Nullable
    JHarmonizerOptOutMode fileOptOutMode;

    @NonNull
    Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes;

    /**
     * Creates an opt-out summary for one source file.
     *
     * @param fileOptOutMode the file-level opt-out mode, or {@code null} when the file stays fully enabled
     * @param typeOptOutModes the type-level opt-out modes keyed by the affected types
     */
    public JHarmonizerOptOuts(
            @Nullable JHarmonizerOptOutMode fileOptOutMode,
            @NonNull Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        this.fileOptOutMode = fileOptOutMode;
        this.typeOptOutModes = Map.copyOf(typeOptOutModes);
    }

    @NonNull
    public static JHarmonizerOptOuts empty() {
        return EMPTY_OPT_OUTS;
    }

    public boolean hasFileOptOutMode(@NonNull JHarmonizerOptOutMode mode) {
        return mode == fileOptOutMode;
    }

    public boolean isEmpty() {
        return fileOptOutMode == null && typeOptOutModes.isEmpty();
    }

    @NonNull
    public Optional<JHarmonizerOptOutMode> findTypeOptOutMode(@NonNull CtType<?> type) {
        return Optional.ofNullable(typeOptOutModes.get(type));
    }

    @NonNull
    public Optional<JHarmonizerOptOutMode> getFileOptOutMode() {
        return Optional.ofNullable(fileOptOutMode);
    }

    @NonNull
    public Set<CtType<?>> getFormattingSkippedTypes() {
        return typeOptOutModes.entrySet().stream()
                .filter(entry -> entry.getValue().skipsFormatting())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @NonNull
    public Set<CtType<?>> getSortingSkippedTypes() {
        return typeOptOutModes.keySet();
    }
}
