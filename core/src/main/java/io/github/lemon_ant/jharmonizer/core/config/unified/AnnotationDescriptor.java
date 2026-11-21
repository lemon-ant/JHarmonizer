// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** Comparison values and order-preservation keys for one outer annotation. */
@Value
public class AnnotationDescriptor {

    /**
     * Concatenated argument tokens, excluding comments and whitespace between tokens.
     * {@code null} represents absent parentheses. The empty string represents parentheses without argument tokens.
     */
    @Nullable
    String arguments;

    /**
     * Length of written declaration tokens and internal comments in UTF-16 code units, including the full
     * spelling of Unicode escapes. Whitespace between tokens and incidental text-block indentation are excluded.
     */
    int declarationLength;

    /** Simple annotation type name. */
    @NonNull
    String name;

    /**
     * Unique keys from this outer annotation and its nested annotation uses, including possible aliases.
     * Fragments in the same source group that share a key retain their relative source order.
     */
    @NonNull
    Set<String> orderPreservationGroupKeys;

    /**
     * Creates an ordering descriptor. The supplied key set backs a read-only view and must stay unchanged.
     * @param arguments tokenized argument text, or null when parentheses are absent
     * @param declarationLength written length of declaration tokens and internal comments
     * @param name simple annotation type name
     * @param orderPreservationGroupKeys dependency keys linking fragments whose relative source order is preserved
     */
    public AnnotationDescriptor(
            @Nullable String arguments,
            int declarationLength,
            @NonNull String name,
            @NonNull Set<String> orderPreservationGroupKeys) {
        this.arguments = arguments;
        this.declarationLength = declarationLength;
        this.name = name;
        this.orderPreservationGroupKeys = Collections.unmodifiableSet(orderPreservationGroupKeys);
    }
}
