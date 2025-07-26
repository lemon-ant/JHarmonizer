package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.List;
import lombok.Value;

/**
 * A single line in type-order: either a string or list of strings.
 * Supports both: "- enum" and "- [class, record]"
 */
@Value
@JsonDeserialize(using = TypeOrderEntryDeserializer.class)
public class TypeGroup {

    List<TypeKind> kinds;

    public TypeGroup(@JsonProperty(value = "kinds", required = true) List<TypeKind> kinds) {
        this.kinds = Collections.unmodifiableList(kinds);
    }
}
