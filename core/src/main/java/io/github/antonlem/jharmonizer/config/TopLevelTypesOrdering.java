package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Value;

@Value
public class TopLevelTypesOrdering {

    boolean mainTypeFirst;
    List<TypeOrderEntry> typeGroups;
    TypeSort intraGroupSorting;

    TopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @JsonProperty(value = "type-groups", required = true) List<TypeOrderEntry> typeGroups,
            @JsonProperty(value = "intra-group-sorting", required = true) TypeSort intraGroupSorting) {
        this.mainTypeFirst = mainTypeFirst;
        this.typeGroups = Collections.unmodifiableList(typeGroups);
        this.intraGroupSorting = intraGroupSorting;
    }
}
