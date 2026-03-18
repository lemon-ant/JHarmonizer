package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Root deserialization model for a JHarmonizer YAML configuration file.
 * Holds formatting settings, backup flag, header-line descriptor,
 * member-group ordering definitions, and top-level type ordering.
 */
@Value
public class JHarmonizerConfig {

    @NonNull
    JHarmonizerFormatting formatting;

    boolean backupsEnabled;

    @NonNull
    JHarmonizerHeaderLine headerLine;

    @NonNull
    List<JHarmonizerMemberGroup> memberGroups;

    @NonNull
    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering;

    // TODO Make it package
    /**
     * Creates a new JHarmonizerConfig.
     * @param topLevelTypesOrdering the top level types ordering
     * @param formatting the formatting
     * @param backupsEnabled the backups enabled
     * @param headerLine the header line
     * @param memberGroups the member groups
     */
    public JHarmonizerConfig(
            @NonNull @JsonProperty(value = "top-level-types-ordering", required = true)
                    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull @JsonProperty(value = "formatting", required = true) JHarmonizerFormatting formatting,
            @JsonProperty(value = "backups-enabled", required = true) boolean backupsEnabled,
            @NonNull @JsonProperty(value = "header-line", required = true) JHarmonizerHeaderLine headerLine,
            @NonNull @JsonProperty(value = "type-members-ordering", required = true)
                    List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.headerLine = headerLine;
        Validate.notEmpty(memberGroups, "type-members-ordering cannot be empty");
        validateMemberGroupNames(memberGroups);
        this.memberGroups = Collections.unmodifiableList(memberGroups);
    }

    private static void validateMemberGroupNames(List<JHarmonizerMemberGroup> memberGroups) {
        memberGroups.forEach(memberGroup -> {
            Validate.isTrue(
                    memberGroup.getName() != null,
                    "Strict type-members-ordering requires a non-null name for every member group");
            if (!memberGroup.getMemberSubGroups().isEmpty()) {
                validateMemberGroupNames(memberGroup.getMemberSubGroups());
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHarmonizerConfig that)) {
            return false;
        }

        return backupsEnabled == that.backupsEnabled
                && formatting.equals(that.formatting)
                && headerLine.equals(that.headerLine)
                && memberGroups.equals(that.memberGroups)
                && topLevelTypesOrdering.equals(that.topLevelTypesOrdering);
    }

    @Override
    public int hashCode() {
        int result = formatting.hashCode();
        result = 31 * result + Boolean.hashCode(backupsEnabled);
        result = 31 * result + headerLine.hashCode();
        result = 31 * result + memberGroups.hashCode();
        result = 31 * result + topLevelTypesOrdering.hashCode();
        return result;
    }
}
