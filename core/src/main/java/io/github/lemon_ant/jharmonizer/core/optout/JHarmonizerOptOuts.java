// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtType;

@Value
public class JHarmonizerOptOuts {
    private static final JHarmonizerOptOuts EMPTY_OPT_OUTS = new JHarmonizerOptOuts(null, Map.of());

    @Nullable
    JHarmonizerOptOutMode fileOptOutMode;

    @NonNull
    Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes;

    /**
     * Returns the shared empty opt-out summary.
     *
     * @return the shared empty opt-out summary
     */
    @NonNull
    public static JHarmonizerOptOuts empty() {
        return EMPTY_OPT_OUTS;
    }

    /**
     * Creates an opt-out summary for one source file.
     *
     * @param fileOptOutMode the file-level opt-out mode, or {@code null} when the file stays fully enabled
     * @param typeOptOutModes the type-level opt-out modes keyed by the affected types
     */
    @SuppressFBWarnings("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR")
    JHarmonizerOptOuts(
            @Nullable JHarmonizerOptOutMode fileOptOutMode,
            @NonNull Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        this.fileOptOutMode = fileOptOutMode;
        this.typeOptOutModes = Collections.unmodifiableMap(typeOptOutModes);
    }

    /**
     * Checks whether neither file-level nor type-level opt-outs are configured.
     *
     * @return {@code true} when no opt-outs are configured
     */
    public boolean isEmpty() {
        return fileOptOutMode == null && typeOptOutModes.isEmpty();
    }

    /**
     * Returns the file-level opt-out mode.
     *
     * @return the file-level opt-out mode, if present
     */
    @NonNull
    public Optional<JHarmonizerOptOutMode> getFileOptOutMode() {
        return Optional.ofNullable(fileOptOutMode);
    }

    /**
     * Collects all types whose formatting and sorting should skip internal reordering.
     *
     * @return the set of types that have sorting skipped
     */
    @NonNull
    public Set<CtType<?>> getSortingSkippedTypes() {
        return typeOptOutModes.entrySet().stream()
                .filter(entry -> entry.getValue() == JHarmonizerOptOutMode.FULLY_OFF
                        || entry.getValue() == JHarmonizerOptOutMode.SORTING_OFF)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks whether the file-level opt-out mode matches the requested mode.
     *
     * @param mode the mode to compare against the file-level opt-out
     * @return {@code true} when the file-level opt-out equals the requested mode
     */
    public boolean hasFileOptOutMode(@NonNull JHarmonizerOptOutMode mode) {
        return mode == fileOptOutMode;
    }

    /**
     * Finds the opt-out mode configured directly for the given type.
     *
     * @param type the type to inspect
     * @return the configured type-level opt-out mode, if present
     */
    @NonNull
    public Optional<JHarmonizerOptOutMode> findTypeOptOutMode(@NonNull CtType<?> type) {
        return Optional.ofNullable(typeOptOutModes.get(type));
    }
}
