// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseCompilationUnitFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonImportUtils.collectTypeImportContext;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.formatCanonicalTypeName;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeTreeUtils;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;

class AnnotationImportResolutionTest {

    @NonNull
    private static final URL RESOLVED_MEMBER_IMPORTS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/import-priority/ResolvedStaticMemberImports.java");

    @NonNull
    private static final URL WILDCARD_BEFORE_SINGLE_TYPE_IMPORT = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/import-priority/WildcardBeforeSingleTypeImport.java");

    @Test
    void resolveAnnotationCanonicalTypeNameOrGroupKey_unavailableWildcardBeforeSingleType_retainsDefiniteImport() {
        // Given
        CtCompilationUnit compilationUnit =
                parseCompilationUnitFromJavaFixtureResource(WILDCARD_BEFORE_SINGLE_TYPE_IMPORT);
        CtType<?> consumerType = compilationUnit.getDeclaredTypes().getFirst();
        Map<String, CtType<?>> declaredTypesByCanonicalName =
                Map.of(formatCanonicalTypeName(consumerType.getReference()), consumerType);
        int annotationSrcStartOffset = consumerType
                .getField("value")
                .getAnnotations()
                .getFirst()
                .getPosition()
                .getSourceStart();
        AnnotationDependencyKeyResolver dependencyResolver = new AnnotationDependencyKeyResolver(compilationUnit);

        // When
        SpoonTypeImportContext typeImportContext =
                collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(annotationSrcStartOffset, "Deprecated");

        // Then
        // Assert the import order in the parsed model because it can differ from source order.
        assertThat(typeImportContext.getWildcardImportsInModelOrder())
                .singleElement()
                .isSameAs(compilationUnit.getImports().getFirst());
        assertThat(typeImportContext.getSrcOwnerTypesByWildcardImport()).isEmpty();
        assertThat(typeImportContext.hasWildcardImports()).isTrue();
        assertThat(dependencyKey).isEqualTo("java.lang.Deprecated");
    }

    @ParameterizedTest
    @CsvSource({"DuplicateUnavailableStaticMembers, false", "ExplicitTypeWithDuplicateStaticMembers, true"})
    void resolveAnnotationTypeName_duplicateUnavailableMembers_preservesLookupCertainty(
            @NonNull String fixtureName, boolean expectedShortNameConfirmation) {
        // Given
        URL fixtureResource =
                requireClasspathResourceUrl("/test-cases/core/spoon/valid/import-priority/" + fixtureName + ".java");
        CtCompilationUnit compilationUnit = parseCompilationUnitFromJavaFixtureResource(fixtureResource);
        CtType<?> consumerType = compilationUnit.getDeclaredTypes().getFirst();
        Map<String, CtType<?>> declaredTypesByCanonicalName =
                Map.of(formatCanonicalTypeName(consumerType.getReference()), consumerType);
        int simpleUseOffset = consumerType
                .getField("simple")
                .getAnnotations()
                .getFirst()
                .getPosition()
                .getSourceStart();
        int qualifiedUseOffset = consumerType
                .getField("qualified")
                .getAnnotations()
                .getFirst()
                .getPosition()
                .getSourceStart();

        // When
        SpoonTypeImportContext typeImportContext =
                collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        AnnotationTypeNameResolver resolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        AnnotationTypeNameResolution simpleResolution =
                resolver.resolveAnnotationTypeName(simpleUseOffset, "Deprecated");
        AnnotationTypeNameResolution qualifiedResolution =
                resolver.resolveAnnotationTypeName(qualifiedUseOffset, "java.lang.Deprecated");

        // Then
        assertThat(compilationUnit.getImports().stream()
                        .filter(typeImport -> typeImport instanceof CtUnresolvedImport unresolvedImport
                                && unresolvedImport.isStatic())
                        .map(typeImport -> ((CtUnresolvedImport) typeImport).getUnresolvedReference()))
                .containsExactlyInAnyOrder(
                        "annotationimports.StaticAnnotationNames.Deprecated",
                        "annotationimports.other.OtherStaticAnnotationNames.Deprecated");
        assertThat(typeImportContext.getUnresolvableStaticMemberSimpleNames()).containsExactly("Deprecated");
        assertThat(simpleResolution.getAnnotationTypeName())
                .isEqualTo(expectedShortNameConfirmation ? "java.lang.Deprecated" : "Deprecated");
        assertThat(simpleResolution.isCanonicalTypeNameConfirmed()).isEqualTo(expectedShortNameConfirmation);
        assertThat(qualifiedResolution.getAnnotationTypeName()).isEqualTo("java.lang.Deprecated");
        assertThat(qualifiedResolution.isCanonicalTypeNameConfirmed()).isFalse();
    }

    @Test
    void resolveAnnotationTypeName_resolvedMembersWithDefiniteTypes_retainsBlockersAndTypePriority() {
        // Given
        CtCompilationUnit compilationUnit = parseCompilationUnitFromJavaFixtureResource(RESOLVED_MEMBER_IMPORTS);
        CtType<?> consumerType = compilationUnit.getDeclaredTypes().getFirst();
        Map<String, CtType<?>> declaredTypesByCanonicalName = SpoonTypeTreeUtils.streamAllTypes(compilationUnit)
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));

        // When
        SpoonTypeImportContext typeImportContext =
                collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        AnnotationTypeNameResolver resolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        List<AnnotationTypeNameResolution> typeNameResolutions = consumerType.getAnnotations().stream()
                .map(annotationUse -> resolver.resolveAnnotationTypeName(
                        annotationUse.getPosition().getSourceStart(),
                        annotationUse.getAnnotationType().getSimpleName()))
                .toList();

        // Then
        assertThat(compilationUnit.getImports())
                .filteredOn(typeImport -> typeImport.getImportKind() == CtImportKind.FIELD)
                .singleElement()
                .satisfies(typeImport -> assertThat(typeImport.getReference()).isInstanceOf(CtFieldReference.class));
        assertThat(compilationUnit.getImports())
                .filteredOn(typeImport -> typeImport.getImportKind() == CtImportKind.METHOD)
                .singleElement()
                .satisfies(
                        typeImport -> assertThat(typeImport.getReference()).isInstanceOf(CtExecutableReference.class));
        assertThat(typeImportContext.getUnresolvableStaticMemberSimpleNames())
                .containsExactlyInAnyOrder("Deprecated", "SuppressWarnings");
        assertThat(typeImportContext.getCanonicalSingleTypeNames())
                .containsExactlyInAnyOrder(
                        "java.lang.Deprecated",
                        "java.lang.SuppressWarnings",
                        "annotationimports.ResolvedStaticMemberImports.NestedTag");
        assertThat(typeImportContext.getSingleTypeImportsBySimpleName())
                .containsOnlyKeys("Deprecated", "SuppressWarnings", "NestedTag");
        assertThat(typeNameResolutions)
                .extracting(AnnotationTypeNameResolution::getAnnotationTypeName)
                .containsExactlyInAnyOrder("java.lang.Deprecated", "java.lang.SuppressWarnings");
        assertThat(typeNameResolutions)
                .allSatisfy(typeNameResolution -> assertThat(typeNameResolution.isCanonicalTypeNameConfirmed())
                        .isTrue());
    }
}
