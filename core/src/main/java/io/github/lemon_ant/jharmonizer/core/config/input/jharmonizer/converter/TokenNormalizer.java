package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Locale;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Token normalization
 */
@Slf4j
@UtilityClass
class TokenNormalizer {

    @NonNull
    static Set<String> normalizeTokens(@NonNull Set<String> rawTokens) {
        return rawTokens.stream()
                .map(StringUtils::trimToNull)
                .filter(rawToken -> {
                    if (rawToken == null) {
                        log.warn("An empty token was found in line {}", rawTokens);
                        return false;
                    } else {
                        return true;
                    }
                })
                .map(token -> token.toLowerCase(Locale.ENGLISH))
                .collect(toUnmodifiableSet());
    }
}
