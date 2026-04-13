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

    boolean printProcessingStatistics;

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
     * @param printProcessingStatistics the print processing statistics flag
     * @param headerLine the header line
     * @param memberGroups the member groups
     */
    public JHarmonizerConfig(
            @NonNull @JsonProperty(value = "top-level-types-ordering", required = true)
                    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull @JsonProperty(value = "formatting", required = true) JHarmonizerFormatting formatting,
            @JsonProperty(value = "backups-enabled", required = true) boolean backupsEnabled,
            @JsonProperty(value = "print-processing-statistics", required = true) boolean printProcessingStatistics,
            @NonNull @JsonProperty(value = "header-line", required = true) JHarmonizerHeaderLine headerLine,
            @NonNull @JsonProperty(value = "type-members-ordering", required = true)
                    List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.printProcessingStatistics = printProcessingStatistics;
        this.headerLine = headerLine;
        Validate.notEmpty(memberGroups, "type-members-ordering cannot be empty");
        this.memberGroups = Collections.unmodifiableList(memberGroups);
    }
}
