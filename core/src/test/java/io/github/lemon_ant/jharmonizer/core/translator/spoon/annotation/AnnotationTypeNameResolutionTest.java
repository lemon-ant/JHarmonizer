// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnotationTypeNameResolutionTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void construct_canonicalLookupWithoutName_rejectsLookup(boolean terminalLookup) {
        CanonicalAnnotationTypeNameLookupResult unsuccessfulLookup = terminalLookup
                ? CanonicalAnnotationTypeNameLookupResult.UNRESOLVED
                : CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;

        // When / Then
        assertThatThrownBy(() -> new AnnotationTypeNameResolution(unsuccessfulLookup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Canonical annotation lookup did not establish a name");
    }
}
