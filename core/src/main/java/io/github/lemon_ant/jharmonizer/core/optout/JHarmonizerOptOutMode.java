package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerOptOutMode {
    FULLY_OFF("fully-off"),
    SORTING_OFF("sort-off"),
    ;

    public static final String TOKEN_PREFIX = "@jharmonizer:";

    private final String displayName;

    /**
     * Builds the exact source token used in comments to enable this opt-out mode.
     *
     * @return the full opt-out token including the common prefix
     */
    @NonNull
    // TODO Calculate it once in constructor
    public String toToken() {
        return TOKEN_PREFIX + displayName;
    }

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
                .filter(mode -> mode.toToken().equals(normalizedToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer opt-out token: " + token));
    }
}
