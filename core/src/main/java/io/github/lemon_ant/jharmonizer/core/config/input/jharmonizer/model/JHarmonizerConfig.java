package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class JHarmonizerConfig {

    // TODO Exclude to a dedicated class
    boolean fixImports;

    @NonNull
    // TODO Exclude to a dedicated class
    FormatterStyle formatterStyle;

    boolean backupsEnabled;

    @NonNull
    JHarmonizerHeaderLine headerLine;

    @NonNull
    List<JHarmonizerMemberGroup> memberGroups;

    @NonNull
    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering;

    @Builder
    JHarmonizerConfig(
            @NonNull @JsonProperty(value = "top-level-types-ordering", required = true)
                    JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering,
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle,
            @JsonProperty(value = "backups-enabled", required = true) boolean backupsEnabled,
            @NonNull @JsonProperty(value = "header-line", required = true) JHarmonizerHeaderLine headerLine,
            @NonNull @JsonProperty(value = "type-members-ordering", required = true)
                    List<@NonNull JHarmonizerMemberGroup> memberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.backupsEnabled = backupsEnabled;
        this.headerLine = headerLine;
        Validate.notEmpty(memberGroups, "type-members-ordering cannot be empty");
        this.memberGroups = Collections.unmodifiableList(memberGroups);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHarmonizerConfig that)) {
            return false;
        }

        return fixImports == that.fixImports
                && backupsEnabled == that.backupsEnabled
                && formatterStyle == that.formatterStyle
                && headerLine.equals(that.headerLine)
                && memberGroups.equals(that.memberGroups)
                && topLevelTypesOrdering.equals(that.topLevelTypesOrdering);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(fixImports);
        result = 31 * result + formatterStyle.hashCode();
        result = 31 * result + Boolean.hashCode(backupsEnabled);
        result = 31 * result + headerLine.hashCode();
        result = 31 * result + memberGroups.hashCode();
        result = 31 * result + topLevelTypesOrdering.hashCode();
        return result;
    }
}
