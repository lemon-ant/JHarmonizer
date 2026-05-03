/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Optional.ofNullable;

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
 * Flexible overlay for UnifiedConfig. Each field is individually optional, but at least one must be set.
 * Use UnifiedConfigMerger.merge(baseline, overlay) to produce a strict UnifiedConfig.
 * This class does NOT define defaults and does NOT invent values.
 * Root member groups are merged only at the root level when provided.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class FlexibleUnifiedConfig {

    /**
     * Partial formatting overlay (preferred API for flex configs).
     */
    @Nullable
    FlexibleUnifiedFormatting formatting;

    @Nullable
    Boolean backupsEnabled;

    @Nullable
    Boolean printProcessingStatistics;

    /**
     * Optional override for header line.
     */
    @Nullable
    UnifiedHeaderLine headerLine;

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
     * Creates a new FlexibleUnifiedConfig.
     * @param topLevelTypesOrdering the top level types ordering
     * @param formatting the formatting
     * @param backupsEnabled the backups enabled
     * @param printProcessingStatistics the print processing statistics flag
     * @param headerLine the header line
     * @param rootMemberGroups the root member groups
     */
    @Builder
    private FlexibleUnifiedConfig(
            @Nullable UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable FlexibleUnifiedFormatting formatting,
            @Nullable Boolean backupsEnabled,
            @Nullable Boolean printProcessingStatistics,
            @Nullable UnifiedHeaderLine headerLine,
            @Nullable List<UnifiedMemberGroup> rootMemberGroups) {
        Validate.isTrue(
                topLevelTypesOrdering != null
                        || formatting != null
                        || backupsEnabled != null
                        || printProcessingStatistics != null
                        || headerLine != null
                        || rootMemberGroups != null,
                "At least one field must be set in FlexibleUnifiedConfig");
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.printProcessingStatistics = printProcessingStatistics;
        this.headerLine = headerLine;
        this.rootMemberGroups =
                ofNullable(rootMemberGroups).map(Collections::unmodifiableList).orElse(null);
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
     * Returns the backups enabled.
     * @return the backups enabled
     */
    @NonNull
    public Optional<Boolean> getBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    /**
     * Returns the print processing statistics flag.
     * @return the print processing statistics flag
     */
    @NonNull
    public Optional<Boolean> getPrintProcessingStatistics() {
        return ofNullable(printProcessingStatistics);
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
