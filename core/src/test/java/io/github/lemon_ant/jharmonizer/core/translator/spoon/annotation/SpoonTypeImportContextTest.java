// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import org.junit.jupiter.api.Test;

class SpoonTypeImportContextTest {

    @Test
    void construct_mutableCollections_exposesReadOnlyViews() {
        // When
        SpoonTypeImportContext typeImportContext = new SpoonTypeImportContext(
                new HashSet<>(), new HashMap<>(), new IdentityHashMap<>(), new HashSet<>(), new ArrayList<>());

        // Then
        assertThatThrownBy(typeImportContext.getCanonicalSingleTypeNames()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(typeImportContext.getSingleTypeImportsBySimpleName()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(typeImportContext.getSrcOwnerTypesByWildcardImport()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(typeImportContext.getUnresolvableStaticMemberSimpleNames()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(typeImportContext.getWildcardImportsInModelOrder()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
