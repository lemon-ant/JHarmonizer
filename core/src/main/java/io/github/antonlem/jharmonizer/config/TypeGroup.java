package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;

/**
 * A single line in type-order: either a string or list of strings.
 * Supports both: "- enum" and "- [class, record]"
 */
@Value
@JsonDeserialize(using = TypeGroupDeserializer.class)
public class TypeGroup {
    @NonNull
    Set<@NonNull TypeKind> typeKinds;

    public TypeGroup(@NonNull @JsonProperty(value = "kinds", required = true) Set<@NonNull TypeKind> typeKinds) {
        this.typeKinds = Collections.unmodifiableSet(typeKinds);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof TypeGroup that) && typeKinds.equals(that.typeKinds);
    }

    @Override
    public int hashCode() {
        return typeKinds.hashCode();
    }
}
