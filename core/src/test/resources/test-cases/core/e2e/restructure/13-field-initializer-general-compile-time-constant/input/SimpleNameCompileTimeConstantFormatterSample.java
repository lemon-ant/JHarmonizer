package io.github.lemon_ant.jharmonizer.core.e2e;

import java.time.format.DateTimeFormatter;

public class SimpleNameCompileTimeConstantFormatterSample {
    static final String zPattern = "yyyy/MM/dd HH:mm:ss";
    static final int cAnchor = Integer.parseInt("1");
    static final DateTimeFormatter aFormatter = DateTimeFormatter.ofPattern(zPattern);

    public static void main(String[] args) {
        if (!"yyyy/MM/dd HH:mm:ss".equals(zPattern)
                || !"2000/01/02 03:04:05".equals(aFormatter.format(java.time.LocalDateTime.of(2000, 1, 2, 3, 4, 5)))
                || cAnchor != 1) {
            throw new IllegalStateException(
                    "Unexpected values: zPattern=" + zPattern + ", formatted="
                            + aFormatter.format(java.time.LocalDateTime.of(2000, 1, 2, 3, 4, 5))
                            + ", cAnchor=" + cAnchor);
        }
    }
}
