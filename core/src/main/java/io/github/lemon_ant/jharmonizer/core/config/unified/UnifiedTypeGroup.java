package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Exact mirror of vendor TypeGroup: just a set of kind tokens.
 */
@Value
public class UnifiedTypeGroup {
    @NonNull
    Set<@NonNull UnifiedTypeKind> typeKinds;

    public UnifiedTypeGroup(@NonNull Set<@NonNull UnifiedTypeKind> typeKinds) {
        Validate.notEmpty(typeKinds, "Type kinds cannot be empty");
        this.typeKinds = Collections.unmodifiableSet(typeKinds);
    }
}
