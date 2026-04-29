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
public class JHarmonizerTopLevelTypeSelector {
    @NonNull
    Set<@NonNull JHarmonizerTypeKind> typeKinds;

    /**
     * Creates a new JHarmonizerTopLevelTypeSelector.
     * @param typeKinds the type kinds
     */
    JHarmonizerTopLevelTypeSelector(
            @NonNull @JsonProperty(value = "kinds", required = true) Set<@NonNull JHarmonizerTypeKind> typeKinds) {
        Validate.notEmpty(typeKinds, "kinds cannot be empty");
        this.typeKinds = Collections.unmodifiableSet(typeKinds);
    }
}
