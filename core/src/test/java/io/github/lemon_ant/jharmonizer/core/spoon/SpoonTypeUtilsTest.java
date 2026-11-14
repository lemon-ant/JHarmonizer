// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseCompilationUnitFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Optional;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpoonTypeUtilsTest {

    @NonNull
    private static final URL FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/canonical-type-names/CanonicalTypeNameReferences.java");

    @NonNull
    private CtType<?> fixtureType;

    @BeforeAll
    void setUp() {
        CtCompilationUnit compilationUnit = parseCompilationUnitFromJavaFixtureResource(FIXTURE);
        fixtureType = compilationUnit.getDeclaredTypes().stream()
                .filter(srcType -> "CanonicalTypeNameReferences".equals(srcType.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing canonical type-name fixture"));
    }

    @ParameterizedTest
    @CsvSource({
        "sourceNested, canonicaltypenames.SourceOwner$Tag, canonicaltypenames.SourceOwner.Tag, true,"
                + " canonicaltypenames.SourceOwner, true",
        "externalNested, canonicaltypenames.library.ExternalAnnotationOwner$Tag,"
                + " canonicaltypenames.library.ExternalAnnotationOwner.Tag, false,"
                + " canonicaltypenames.library.ExternalAnnotationOwner, false",
        "externalNestedDollar, canonicaltypenames.library.ExternalAnnotationOwner$Tag$Kind,"
                + " canonicaltypenames.library.ExternalAnnotationOwner.Tag$Kind, false,"
                + " canonicaltypenames.library.ExternalAnnotationOwner, false",
        "topLevelDollar, canonicaltypenames.Top$Tag, canonicaltypenames.Top$Tag, true, , false",
        "nestedDollar, canonicaltypenames.Dollar$Owner$Tag$Kind, canonicaltypenames.Dollar$Owner.Tag$Kind, false,"
                + " canonicaltypenames.Dollar$Owner, true"
    })
    void formatCanonicalTypeName_parsedReferences_preservesDeclaredTypeBoundaries(
            @NonNull String fieldName,
            @NonNull String expectedQualifiedName,
            @NonNull String expectedCanonicalName,
            boolean expectedDeclarationPresent,
            @Nullable String expectedDeclaringTypeName,
            boolean expectedDeclaringDeclarationPresent) {
        // Given / When
        CtTypeReference<?> typeReference =
                fixtureType.getField(fieldName).getAnnotations().getFirst().getAnnotationType();
        Optional<CtTypeReference<?>> declaringTypeReference = Optional.ofNullable(typeReference.getDeclaringType());
        String canonicalTypeName = formatCanonicalTypeName(typeReference);

        // Then
        assertThat(typeReference.getQualifiedName()).isEqualTo(expectedQualifiedName);
        assertThat(Optional.ofNullable(typeReference.getDeclaration()).isPresent())
                .isEqualTo(expectedDeclarationPresent);
        assertThat(declaringTypeReference.map(CtTypeReference::getQualifiedName))
                .isEqualTo(Optional.ofNullable(expectedDeclaringTypeName));
        assertThat(declaringTypeReference
                        .map(declaringType -> declaringType.getDeclaration() != null)
                        .orElse(false))
                .isEqualTo(expectedDeclaringDeclarationPresent);
        assertThat(canonicalTypeName).isEqualTo(expectedCanonicalName);
    }
}
