// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;

@Getter
public enum JHarmonizerOptOutMode {
    FULLY_OFF("fully-off"),
    SORTING_OFF("sort-off"),
    ;

    public static final String TOKEN_PREFIX = "@jharmonizer:";

    private final String displayName;
    private final String token;

    /**
     * Resolves an opt-out mode from the token found in source comments.
     *
     * @param token the full opt-out token to parse
     * @return the matching opt-out mode
     */
    @NonNull
    public static JHarmonizerOptOutMode fromToken(@NonNull String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.getToken().equals(normalizedToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer opt-out token: " + token));
    }

    JHarmonizerOptOutMode(String displayName) {
        this.displayName = displayName;
        this.token = TOKEN_PREFIX + displayName;
    }
}
