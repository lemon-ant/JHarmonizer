// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.unified;

import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/** Precomputed annotation sorting keys, independent of source ranges and the AST implementation. */
@Value
public class AnnotationDescriptor {

    /**
     * Argument tokens inside parentheses, excluding comments and inter-token whitespace;
     * null if the annotation has no parentheses, empty if the parentheses contain no argument tokens.
     */
    @Nullable
    String arguments;

    /** Length of declaration tokens and internal comments, including the qualified name. */
    int declarationLength;

    /** Simple annotation type name. */
    @NonNull
    String name;
}
