package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
public class ConfigRoot {

    @NonNull
    TopLevelTypesOrdering topLevelTypesOrdering;

    boolean fixImports;

    @NonNull
    FormatterStyle formatterStyle;

    @NonNull
    HeaderLine headerLine;

    @NonNull
    List<MemberGroup> typeMembersOrdering;

    ConfigRoot(
            @NonNull @JsonProperty("top-level-types-ordering") TopLevelTypesOrdering topLevelTypesOrdering,
            @JsonProperty(value = "fix-imports", required = true) boolean fixImports,
            @NonNull @JsonProperty(value = "formatter-style", required = true) FormatterStyle formatterStyle,
            @NonNull @JsonProperty(value = "header-line", required = true) HeaderLine headerLine,
            @NonNull @JsonProperty(value = "type-members-ordering", required = true)
                    List<@NonNull MemberGroup> typeMembersOrdering) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.headerLine = headerLine;
        this.typeMembersOrdering = Collections.unmodifiableList(typeMembersOrdering);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConfigRoot that)) {
            return false;
        }
        return fixImports == that.fixImports
                && topLevelTypesOrdering.equals(that.topLevelTypesOrdering)
                && formatterStyle == that.formatterStyle
                && headerLine.equals(that.headerLine)
                && typeMembersOrdering.equals(that.typeMembersOrdering);
    }

    @Override
    public int hashCode() {
        int result = topLevelTypesOrdering.hashCode();
        result = 31 * result + Boolean.hashCode(fixImports);
        result = 31 * result + formatterStyle.hashCode();
        result = 31 * result + headerLine.hashCode();
        result = 31 * result + typeMembersOrdering.hashCode();
        return result;
    }
}
