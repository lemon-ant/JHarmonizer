package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JHarmonizerOptOutMode {
    FULLY_OFF("fullyOff", List.of("@jharmonizer:fully-off", "@jharmonizer:off")),
    SORTING_OFF("sortingOff", List.of("@jharmonizer:sort-off")),
    ;

    public static final String TOKEN_PREFIX = "@jharmonizer:";

    private final String displayName;
    private final List<String> acceptedTokens;

    @NonNull
    public String getToken() {
        return acceptedTokens.getFirst();
    }

    @NonNull
    public static JHarmonizerOptOutMode fromToken(String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.acceptedTokens.contains(normalizedToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer opt-out token: " + token));
    }

    public boolean skipsFormatting() {
        return this == FULLY_OFF;
    }
}
