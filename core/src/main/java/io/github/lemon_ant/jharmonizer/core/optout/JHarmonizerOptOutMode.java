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

    public static final String TOKEN_PREFIX = "@JHarmonizer:";

    private final String displayName;

    @NonNull
    public String getToken() {
        return TOKEN_PREFIX + displayName;
    }

    @NonNull
    public static JHarmonizerOptOutMode fromToken(String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.getToken().toLowerCase(Locale.ROOT).equals(normalizedToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer opt-out token: " + token));
    }

    public boolean skipsFormatting() {
        return this == FULLY_OFF;
    }
}
