package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

/**
 * Flexible overlay for JHarmonizerConfig. All fields are optional.
 */
@Value
public class JHarmonizerFlexibleConfig {

    @Nullable
    JHarmonizerFormatting formatting;

    @Nullable
    Boolean backupsEnabled;

    @Nullable
    JHarmonizerHeaderLine headerLine;

    @Nullable
    List<JHarmonizerMemberGroup> memberGroups;

    @Nullable
    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering;

    public JHarmonizerFlexibleConfig(
            @Nullable @JsonProperty("top-level-types-ordering") JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable @JsonProperty("formatting") JHarmonizerFormatting formatting,
            @Nullable @JsonProperty("backups-enabled") Boolean backupsEnabled,
            @Nullable @JsonProperty("header-line") JHarmonizerHeaderLine headerLine,
            @Nullable @JsonProperty("type-members-ordering") List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.headerLine = headerLine;
        this.memberGroups =
                ofNullable(memberGroups).map(Collections::unmodifiableList).orElse(null);
    }

    @NonNull
    public Optional<JHarmonizerFormatting> getOptionalFormatting() {
        return ofNullable(formatting);
    }

    @NonNull
    public Optional<Boolean> getOptionalBackupsEnabled() {
        return ofNullable(backupsEnabled);
    }

    @NonNull
    public Optional<JHarmonizerHeaderLine> getOptionalHeaderLine() {
        return ofNullable(headerLine);
    }

    @NonNull
    public Optional<List<JHarmonizerMemberGroup>> getOptionalMemberGroups() {
        return ofNullable(memberGroups);
    }

    @NonNull
    public Optional<JHarmonizerTopLevelTypesOrdering> getOptionalTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }
}
