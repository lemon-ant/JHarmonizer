/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Exact mirror of vendor JHarmonizerTopLevelTypeSelector: just a set of kind tokens.
 */
@Value
// TODO Remove this type
public class UnifiedTopLevelTypeSelector {
    @NonNull
    Set<@NonNull UnifiedTypeKind> typeKinds;

    /**
     * Creates a new UnifiedTopLevelTypeSelector.
     * @param typeKinds the type kinds
     */
    public UnifiedTopLevelTypeSelector(@NonNull Set<@NonNull UnifiedTypeKind> typeKinds) {
        Validate.notEmpty(typeKinds, "Type kinds cannot be empty");
        this.typeKinds = Collections.unmodifiableSet(new TreeSet<>(typeKinds));
    }
}
