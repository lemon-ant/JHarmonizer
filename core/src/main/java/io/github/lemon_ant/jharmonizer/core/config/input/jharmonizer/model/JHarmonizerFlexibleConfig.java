package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Flexible overlay for JHarmonizerConfig. Each field is individually optional, but at least one must be set.
 */
@Value
@SuppressWarnings("PMD.DataClass")
@Getter(AccessLevel.NONE)
public class JHarmonizerFlexibleConfig {

    @Nullable
    JHarmonizerFlexibleFormatting formatting;

    @Nullable
    Boolean backupsEnabled;

    @Nullable
    Boolean printProcessingStatistics;

    @Nullable
    JHarmonizerHeaderLine headerLine;

    @Nullable
    List<JHarmonizerMemberGroup> memberGroups;

    @Nullable
    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Creates a flexible JHarmonizer configuration with optional overlay values.
     *
     * @param topLevelTypesOrdering the optional top-level types ordering override
     * @param formatting the optional partial formatting override
     * @param backupsEnabled the optional backups-enabled override
     * @param printProcessingStatistics the optional print-processing-statistics override
     * @param headerLine the optional header-line override
     * @param memberGroups the optional member group overrides
     */
    public JHarmonizerFlexibleConfig(
            @Nullable @JsonProperty("top-level-types-ordering") JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable @JsonProperty("formatting") JHarmonizerFlexibleFormatting formatting,
            @Nullable @JsonProperty("backups-enabled") Boolean backupsEnabled,
            @Nullable @JsonProperty("print-processing-statistics") Boolean printProcessingStatistics,
            @Nullable @JsonProperty("header-line") JHarmonizerHeaderLine headerLine,
            @Nullable @JsonProperty("type-members-ordering") List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        Validate.isTrue(
                topLevelTypesOrdering != null
                        || formatting != null
                        || backupsEnabled != null
                        || printProcessingStatistics != null
                        || headerLine != null
                        || memberGroups != null,
                "At least one field must be set in JHarmonizerFlexibleConfig");
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.printProcessingStatistics = printProcessingStatistics;
        this.headerLine = headerLine;
        this.memberGroups =
                ofNullable(memberGroups).map(Collections::unmodifiableList).orElse(null);
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
     * Returns the optional backups-enabled override.
     *
     * @return the optional backups-enabled override
     */
    @NonNull
    public Optional<Boolean> getBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    /**
     * Returns the optional print-processing-statistics override.
     *
     * @return the optional print-processing-statistics override
     */
    @NonNull
    public Optional<Boolean> getPrintProcessingStatistics() {
        return ofNullable(printProcessingStatistics);
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
     * Returns the optional top-level types ordering override.
     *
     * @return the optional top-level types ordering override
     */
    @NonNull
    public Optional<JHarmonizerTopLevelTypesOrdering> getTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }
}
