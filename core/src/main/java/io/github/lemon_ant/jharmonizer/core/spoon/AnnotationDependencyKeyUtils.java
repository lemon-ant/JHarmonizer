// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Encodes canonical-name confidence in original annotation dependency keys. */
@UtilityClass
class AnnotationDependencyKeyUtils {

    @NonNull
    private static final String UNCONFIRMED_GROUP_KEY_PREFIX = "?.";

    /**
     * Marks an annotation or container name whose canonical identity is unconfirmed.
     * @param typeName unprefixed annotation or container name
     * @return dependency key in the unconfirmed-name namespace
     */
    @NonNull
    static String formatUnconfirmedGroupKey(@NonNull String typeName) {
        return UNCONFIRMED_GROUP_KEY_PREFIX + typeName;
    }

    /**
     * Reads canonical-name confidence from an original dependency key, before synthetic pair expansion.
     * @param groupKey original annotation or container dependency key
     * @return whether the key represents a confirmed canonical identity
     */
    static boolean isConfirmedGroupKey(@NonNull String groupKey) {
        return !groupKey.startsWith(UNCONFIRMED_GROUP_KEY_PREFIX);
    }
}
