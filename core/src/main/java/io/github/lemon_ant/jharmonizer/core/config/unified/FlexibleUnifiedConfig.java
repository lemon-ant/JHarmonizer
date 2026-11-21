// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Optional.ofNullable;

import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Overrides for {@link UnifiedConfig}, with at least one field set.
 * {@link UnifiedConfigMerger} inherits omitted values from the baseline.
 */
@Value
@Getter(AccessLevel.NONE)
@SuppressWarnings("PMD.DataClass")
public class FlexibleUnifiedConfig {

    @Nullable
    List<UnifiedAnnotationOrderingRule> annotationOrderingRules;

    @Nullable
    Boolean backupsEnabled;

    /**
     * Partial formatting overlay (preferred API for flex configs).
     */
    @Nullable
    FlexibleUnifiedFormatting formatting;

    /**
     * Optional override for header line.
     */
    @Nullable
    UnifiedHeaderLine headerLine;

    @Nullable
    ProcessingStatisticsMode processingStatisticsMode;

    /**
     * Optional override for root member groups.
     * Matching root-group names replace baseline groups in place, and new root groups are prepended.
     */
    @Nullable
    List<UnifiedMemberGroup> rootMemberGroups;

    /**
     * Optional override for top-level types ordering.
     */
    @Nullable
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Creates an overlay with read-only views of supplied collections.
     * @param annotationOrderingRules replacement annotation criteria, with an empty list preserving source order
     * @param topLevelTypesOrdering the top level types ordering
     * @param formatting the formatting
     * @param backupsEnabled the backups enabled
     * @param processingStatisticsMode the processing statistics mode
     * @param headerLine the header line
     * @param rootMemberGroups the root member groups
     */
    @Builder
    private FlexibleUnifiedConfig(
            @Nullable List<UnifiedAnnotationOrderingRule> annotationOrderingRules,
            @Nullable UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable FlexibleUnifiedFormatting formatting,
            @Nullable Boolean backupsEnabled,
            @Nullable ProcessingStatisticsMode processingStatisticsMode,
            @Nullable UnifiedHeaderLine headerLine,
            @Nullable List<UnifiedMemberGroup> rootMemberGroups) {
        Validate.isTrue(
                annotationOrderingRules != null
                        || topLevelTypesOrdering != null
                        || formatting != null
                        || backupsEnabled != null
                        || processingStatisticsMode != null
                        || headerLine != null
                        || rootMemberGroups != null,
                "At least one field must be set in FlexibleUnifiedConfig");
        this.annotationOrderingRules = ofNullable(annotationOrderingRules)
                .map(Collections::unmodifiableList)
                .orElse(null);
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.processingStatisticsMode = processingStatisticsMode;
        this.headerLine = headerLine;
        this.rootMemberGroups =
                ofNullable(rootMemberGroups).map(Collections::unmodifiableList).orElse(null);
    }

    /**
     * Returns the annotation ordering override.
     * @return replacement rules in priority order, or no value to inherit the baseline rules
     */
    @NonNull
    public Optional<List<UnifiedAnnotationOrderingRule>> getAnnotationOrderingRules() {
        return ofNullable(annotationOrderingRules);
    }

    /**
     * Returns the backups enabled.
     * @return the backups enabled
     */
    @NonNull
    public Optional<Boolean> getBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    /**
     * Returns the formatting overlay.
     * @return the formatting overlay
     */
    @NonNull
    public Optional<FlexibleUnifiedFormatting> getFormatting() {
        return ofNullable(formatting);
    }

    /**
     * Returns the header line.
     * @return the header line
     */
    @NonNull
    public Optional<UnifiedHeaderLine> getHeaderLine() {
        return ofNullable(headerLine);
    }

    /**
     * Returns the processing statistics mode.
     * @return the processing statistics mode
     */
    @NonNull
    public Optional<ProcessingStatisticsMode> getProcessingStatisticsMode() {
        return ofNullable(processingStatisticsMode);
    }

    /**
     * Returns the root member groups.
     * @return the root member groups
     */
    @NonNull
    public Optional<List<UnifiedMemberGroup>> getRootMemberGroups() {
        return ofNullable(rootMemberGroups);
    }

    /**
     * Returns the top level types ordering.
     * @return the top level types ordering
     */
    @NonNull
    public Optional<UnifiedTopLevelTypesOrdering> getTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }
}
