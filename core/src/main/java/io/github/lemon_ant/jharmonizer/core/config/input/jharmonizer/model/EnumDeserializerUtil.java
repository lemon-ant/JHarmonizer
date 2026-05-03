/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import java.util.Locale;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Internal helper for deserializing enum values from YAML strings in a case-insensitive,
 * hyphen-tolerant manner.
 */
@UtilityClass
class EnumDeserializerUtil {
    /**
     * Performs the deserialize.
     * @param enumClass the enum type to read from
     * @param value the raw value to parse
     * @return the result
     */
    @NonNull
    static <T extends Enum<T>> T deserialize(@NonNull Class<T> enumClass, @NonNull String value) {
        String trimmedValue = Objects.requireNonNull(StringUtils.trimToNull(value));
        String enumName = trimmedValue.toUpperCase(Locale.ENGLISH).replace('-', '_');
        return Enum.valueOf(enumClass, enumName);
    }
}
