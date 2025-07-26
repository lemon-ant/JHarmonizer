package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.Value;

@Value
public class JavaFileEntry {

    boolean mainTypeFirst;
    List<TypeOrderEntry> typeOrderEntries;
    TypeSort typeSort;

    JavaFileEntry(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @JsonProperty(value = "type-order", required = true) List<TypeOrderEntry> typeOrderEntries,
            @JsonProperty(value = "type-sort", required = true) TypeSort typeSort) {
        this.mainTypeFirst = mainTypeFirst;
        this.typeOrderEntries = Collections.unmodifiableList(typeOrderEntries);
        this.typeSort = typeSort;
    }
}
