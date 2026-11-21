// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Encodes canonical-name confidence in original annotation dependency keys. */
@UtilityClass
class AnnotationDependencyKeyUtils {

    @NonNull
    private static final String UNCONFIRMED_GROUP_KEY_PREFIX = "?.";

    @NonNull
    static String formatUnconfirmedGroupKey(@NonNull String typeName) {
        return UNCONFIRMED_GROUP_KEY_PREFIX + typeName;
    }

    /** Checks whether this original dependency key represents a confirmed canonical identity. */
    static boolean isConfirmedGroupKey(@NonNull String groupKey) {
        return !groupKey.startsWith(UNCONFIRMED_GROUP_KEY_PREFIX);
    }
}
