// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenNormalizerAndTokenMapsTest {

    @Test
    void normalizeTokens_mixedCasingWhitespaceAndNulls_returnsTrimmedLowercaseNonBlankTokens() {
        // Given
        Set<String> rawTokens = new HashSet<>();
        rawTokens.add("  Static  ");
        rawTokens.add("INITIALIZER");
        rawTokens.add("field");
        rawTokens.add("  ");
        rawTokens.add("");
        rawTokens.add(null);

        // When
        Set<String> normalizedTokens = TokenNormalizer.normalizeTokens(rawTokens);

        // Then
        assertThat(normalizedTokens).containsExactlyInAnyOrder("static", "initializer", "field");
    }

    @Test
    void normalizeTokens_matcherTokens_preserveOriginalCase() {
        // Given — name-regex, exact-name, and annotation-matcher tokens must keep their case
        // so that patterns like ~.*Test$ and @ExtendWith match the actual source identifiers
        Set<String> rawTokens = Set.of("~.*Test$", "~.*IT$", "=toString", "@BeforeEach", "@~.*Test$", "class");

        // When
        Set<String> normalizedTokens = TokenNormalizer.normalizeTokens(rawTokens);

        // Then
        assertThat(normalizedTokens)
                .contains("~.*Test$", "~.*IT$", "=toString", "@BeforeEach", "@~.*Test$")
                .contains("class");
    }

    @Test
    void tokenMaps_commonAliasesAndSynonyms_areRegistered() {
        // When / Then
        // Kind aliases
        assertThat(TokenMaps.KIND_BY_TOKEN.get("init")).isEqualTo(MemberKind.INIT_BLOCK);
        assertThat(TokenMaps.KIND_BY_TOKEN.get("initializer")).isEqualTo(MemberKind.INIT_BLOCK);
        // Access aliases
        assertThat(TokenMaps.ACCESS_BY_TOKEN.get("package")).isEqualTo(MemberAccess.PACKAGE);
        assertThat(TokenMaps.ACCESS_BY_TOKEN.get("package-private")).isEqualTo(MemberAccess.PACKAGE);
        // Modifier aliases
        assertThat(TokenMaps.MODIFIER_BY_TOKEN.get("nonsealed")).isEqualTo(DeclarationModifier.NON_SEALED);
        assertThat(TokenMaps.MODIFIER_BY_TOKEN.get("non-sealed")).isEqualTo(DeclarationModifier.NON_SEALED);
    }
}
