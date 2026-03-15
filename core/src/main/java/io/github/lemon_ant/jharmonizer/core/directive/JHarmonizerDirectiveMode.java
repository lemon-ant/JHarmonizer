package io.github.lemon_ant.jharmonizer.core.directive;

import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;

@Getter
public enum JHarmonizerDirectiveMode {
    OFF("@jharmonizer:off"),
    SORT_OFF("@jharmonizer:sort-off"),
    ;

    private final String token;

    JHarmonizerDirectiveMode(String token) {
        this.token = token;
    }

    @NonNull
    public static JHarmonizerDirectiveMode fromToken(String token) {
        return Arrays.stream(values())
                .filter(mode -> mode.token.equals(token))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported JHarmonizer directive token: " + token));
    }

    public boolean skipsFormatting() {
        return this == OFF;
    }
}
