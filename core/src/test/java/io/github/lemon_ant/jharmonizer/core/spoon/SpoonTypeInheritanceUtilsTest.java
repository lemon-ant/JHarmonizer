// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeInheritanceUtils.findInheritedMemberTypeDeclaration;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeInheritanceUtils.InheritedMemberTypeLookupResult;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.net.URL;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;

class SpoonTypeInheritanceUtilsTest {

    @NonNull
    private static final URL INHERITED_MEMBER_TYPE_BRANCHES = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/inherited-member-type-branches/InheritedMemberTypeBranches.java");

    @Test
    void findInheritedMemberTypeDeclaration_ambiguousInheritedHomonyms_preservesUnresolvedLookup() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(INHERITED_MEMBER_TYPE_BRANCHES);
        CtType<?> annotationOwner = parsedModel.getCompilationUnit().getDeclaredTypes().stream()
                .filter(srcType -> "AmbiguousInheritedAnnotationOwner".equals(srcType.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ambiguous annotation owner in fixture"));

        // When
        InheritedMemberTypeLookupResult lookup =
                findInheritedMemberTypeDeclaration(parsedModel.getCompilationUnit(), annotationOwner, "Tag");

        // Then
        assertThat(lookup.getDeclaration()).isNull();
        assertThat(lookup.isUnresolved()).isTrue();
    }
}
