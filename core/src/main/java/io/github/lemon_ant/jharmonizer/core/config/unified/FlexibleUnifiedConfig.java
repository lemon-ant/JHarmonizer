package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Optional.ofNullable;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Flexible overlay for UnifiedConfig. All fields are optional.
 * Use UnifiedConfigMerger.merge(baseline, overlay) to produce a strict UnifiedConfig.
 * This class does NOT define defaults and does NOT invent values.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class FlexibleUnifiedConfig {

    /**
     * Cohesive formatting definition (preferred API).
     */
    @Nullable
    UnifiedFormatting formatting;

    @Nullable
    Boolean backupsEnabled;

    /**
     * Optional override for header line.
     */
    @Nullable
    UnifiedHeaderLine headerLine;

    /**
     * Optional override for root member groups (full replacement).
     */
    @Nullable
    List<UnifiedMemberGroup> rootMemberGroups;

    /**
     * Optional override for top-level types ordering.
     */
    @Nullable
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

    public FlexibleUnifiedConfig(
            @Nullable UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable UnifiedFormatting formatting,
            @Nullable Boolean backupsEnabled,
            @Nullable UnifiedHeaderLine headerLine,
            @Nullable @Singular List<UnifiedMemberGroup> rootMemberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.headerLine = headerLine;
        this.rootMemberGroups =
                ofNullable(rootMemberGroups).map(Collections::unmodifiableList).orElse(null);
    }

    @NonNull
    public Optional<UnifiedFormatting> getFormatting() {
        return ofNullable(formatting);
    }

    @NonNull
    public Optional<Boolean> getBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    @NonNull
    public Optional<UnifiedHeaderLine> getHeaderLine() {
        return ofNullable(headerLine);
    }

    @NonNull
    public Optional<List<UnifiedMemberGroup>> getRootMemberGroups() {
        return ofNullable(rootMemberGroups);
    }

    @NonNull
    public Optional<UnifiedTopLevelTypesOrdering> getTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }
}
