/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import lombok.NonNull;

/**
 * Intra-group sorting strategies available in the JHarmonizer YAML config.
 */
public enum IntraGroupSorting {
    ALPHA,
    PRESERVE,
    SIGNATURE,
    VISIBILITY_ASC,
    VISIBILITY_DESC,
    ;

    /**
     * Performs the from string.
     * @param value the raw value to parse
     * @return the result
     */
    @NonNull
    @JsonCreator
    static IntraGroupSorting fromString(@NonNull String value) {
        return IntraGroupSorting.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
}
