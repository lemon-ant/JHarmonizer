package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

@Value
public class JHarmonizerConfig {

    boolean fixImports;

    @NonNull
    FormatterStyle formatterStyle;

    @NonNull
    HeaderLine headerLine;

    @NonNull
    TopLevelTypesOrdering topLevelTypesOrdering;

    @NonNull
    List<MemberGroup> memberGroups;

    @Builder
    JHarmonizerConfig(
            @NonNull @JsonProperty("top-level-types-ordering") TopLevelTypesOrdering topLevelTypesOrdering,
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle,
            @NonNull @JsonProperty(value = "header-line", required = true) HeaderLine headerLine,
            @NonNull @JsonProperty(value = "type-members-ordering", required = true)
                    List<@NonNull MemberGroup> memberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
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
                && topLevelTypesOrdering.equals(that.topLevelTypesOrdering)
                && formatterStyle == that.formatterStyle
                && headerLine.equals(that.headerLine)
                && memberGroups.equals(that.memberGroups);
    }

    @Override
    public int hashCode() {
        int result = topLevelTypesOrdering.hashCode();
        result = 31 * result + Boolean.hashCode(fixImports);
        result = 31 * result + formatterStyle.hashCode();
        result = 31 * result + headerLine.hashCode();
        result = 31 * result + memberGroups.hashCode();
        return result;
    }
}
