/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.translator;

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
