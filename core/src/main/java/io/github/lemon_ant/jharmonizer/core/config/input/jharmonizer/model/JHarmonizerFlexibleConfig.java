// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Flexible overlay for JHarmonizerConfig. Each field is individually optional, but at least one must be set.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class JHarmonizerFlexibleConfig {

    @Nullable
    Boolean backupsEnabled;

    @Nullable
    JHarmonizerFlexibleFormatting formatting;

    @Nullable
    JHarmonizerHeaderLine headerLine;

    @Nullable
    List<JHarmonizerMemberGroup> memberGroups;

    @Nullable
    ProcessingStatisticsMode processingStatisticsMode;

    @Nullable
    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Creates a flexible JHarmonizer configuration with optional overlay values.
     *
     * @param topLevelTypesOrdering the optional top-level types ordering override
     * @param formatting the optional partial formatting override
     * @param backupsEnabled the optional backups-enabled override
     * @param processingStatisticsMode the optional processing-statistics-mode override
     * @param headerLine the optional header-line override
     * @param memberGroups the optional member group overrides
     */
    public JHarmonizerFlexibleConfig(
            @Nullable @JsonProperty("top-level-types-ordering") JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable @JsonProperty("formatting") JHarmonizerFlexibleFormatting formatting,
            @Nullable @JsonProperty("backups-enabled") Boolean backupsEnabled,
            @Nullable @JsonProperty("processing-statistics-mode") ProcessingStatisticsMode processingStatisticsMode,
            @Nullable @JsonProperty("header-line") JHarmonizerHeaderLine headerLine,
            @Nullable @JsonProperty("type-members-ordering") List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        Validate.isTrue(
                topLevelTypesOrdering != null
                        || formatting != null
                        || backupsEnabled != null
                        || processingStatisticsMode != null
                        || headerLine != null
                        || memberGroups != null,
                "At least one field must be set in JHarmonizerFlexibleConfig");
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.processingStatisticsMode = processingStatisticsMode;
        this.headerLine = headerLine;
        this.memberGroups =
                ofNullable(memberGroups).map(Collections::unmodifiableList).orElse(null);
    }

    /**
     * Returns the optional backups-enabled override.
     *
     * @return the optional backups-enabled override
     */
    @NonNull
    public Optional<Boolean> getBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    /**
     * Returns the optional partial formatting override.
     *
     * @return the optional partial formatting override
     */
    @NonNull
    public Optional<JHarmonizerFlexibleFormatting> getFormatting() {
        return ofNullable(formatting);
    }

    /**
     * Returns the optional header-line override.
     *
     * @return the optional header-line override
     */
    @NonNull
    public Optional<JHarmonizerHeaderLine> getHeaderLine() {
        return ofNullable(headerLine);
    }

    /**
     * Returns the optional member group overrides.
     *
     * @return the optional member group overrides
     */
    @NonNull
    public Optional<List<JHarmonizerMemberGroup>> getMemberGroups() {
        return ofNullable(memberGroups);
    }

    /**
     * Returns the optional processing-statistics-mode override.
     *
     * @return the optional processing-statistics-mode override
     */
    @NonNull
    public Optional<ProcessingStatisticsMode> getProcessingStatisticsMode() {
        return ofNullable(processingStatisticsMode);
    }

    /**
     * Returns the optional top-level types ordering override.
     *
     * @return the optional top-level types ordering override
     */
    @NonNull
    public Optional<JHarmonizerTopLevelTypesOrdering> getTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }
}
