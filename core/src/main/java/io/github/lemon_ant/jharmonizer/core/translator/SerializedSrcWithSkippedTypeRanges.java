// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders @Value class fields, breaking Lombok constructors;
// remove this directive once jharmonizer is upgraded to a version that fixes the @Value field-ordering bug.
import java.util.Collections;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtType;

@Value
public class SerializedSrcWithSkippedTypeRanges {
    @NonNull
    String serializedSrcCode;

    @NonNull
    Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges;

    public SerializedSrcWithSkippedTypeRanges(
            @NonNull String serializedSrcCode,
            @NonNull Map<@NonNull CtType<?>, @NonNull SrcCharacterRange> sortingSkippedTypeRanges) {
        this.serializedSrcCode = serializedSrcCode;
        this.sortingSkippedTypeRanges = Collections.unmodifiableMap(sortingSkippedTypeRanges);
    }
}
