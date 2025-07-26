package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Value;

@Value
public class TopLevelTypesOrdering {

    boolean mainTypeFirst;
    List<TypeGroup> typeGroups;
    IntraGroupSorting intraGroupSorting;

    TopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @JsonProperty(value = "type-groups", required = true) List<TypeGroup> typeGroups,
            @JsonProperty(value = "intra-group-sorting", required = true) IntraGroupSorting intraGroupSorting) {
        this.mainTypeFirst = mainTypeFirst;
        this.typeGroups = Collections.unmodifiableList(typeGroups);
        this.intraGroupSorting = intraGroupSorting;
    }
}
