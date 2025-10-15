package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * A single line in type-order: either a string or list of strings.
 * Supports both: "- enum" and "- [class, record]"
 */
@Value
@JsonDeserialize(using = TypeGroupDeserializer.class)
public class JHarmonizerTypeGroup {
    @NonNull
    Set<@NonNull JHarmonizerTypeKind> typeKinds;

    JHarmonizerTypeGroup(@NonNull @JsonProperty(value = "kinds", required = true) Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        Validate.notEmpty(typeKinds, "kinds cannot be empty");
        this.typeKinds = Collections.unmodifiableSet(typeKinds);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof JHarmonizerTypeGroup other) && typeKinds.equals(other.typeKinds);
    }

    @Override
    public int hashCode() {
        return typeKinds.hashCode();
    }
}
