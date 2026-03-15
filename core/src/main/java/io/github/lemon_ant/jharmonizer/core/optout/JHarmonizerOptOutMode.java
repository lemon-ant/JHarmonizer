package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerOptOutMode {
    FULLY_OFF("fullyOff", "@jharmonizer:off"),
    SORTING_OFF("sortingOff", "@jharmonizer:sort-off"),
    ;

    private final String displayName;
    private final String token;

    @NonNull
    public static JHarmonizerOptOutMode fromToken(String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.token.equals(normalizedToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer opt-out token: " + token));
    }

    public boolean skipsFormatting() {
        return this == FULLY_OFF;
    }
}
