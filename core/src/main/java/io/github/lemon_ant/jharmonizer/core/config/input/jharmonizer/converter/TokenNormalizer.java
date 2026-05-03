// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
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

    /**
     * Normalizes the tokens.
     * <p>
     * Keyword tokens (member kinds, access levels, modifiers) are lowercased to allow
     * case-insensitive YAML authoring. Name matchers ({@code ~…}, {@code =…}) and annotation
     * matchers ({@code @…}) preserve their original case because they are used as regex or
     * exact-match patterns and must match the actual Java identifiers in source code.
     *
     * @param rawTokens the raw tokens to normalize
     * @return the resulting set
     */
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
                .map(token -> isMatcherToken(token) ? token : token.toLowerCase(Locale.ENGLISH))
                .collect(toUnmodifiableSet());
    }

    /**
     * Returns {@code true} for tokens that carry case-sensitive match patterns and must not be
     * lowercased: name matchers starting with {@code ~} or {@code =}, and annotation matchers
     * starting with {@code @}.
     */
    private static boolean isMatcherToken(String token) {
        char first = token.charAt(0);
        return first == '~' || first == '=' || first == '@';
    }
}
