// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonImportUtils.collectTypeImportContext;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonImportUtils.extractCanonicalSingleImportTypeName;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseCompilationUnitFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeMemberWildcardImportReference;

class SpoonImportUtilsTest {

    @NonNull
    private static final URL RESOLVED_MEMBER_IMPORTS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/import-priority/ResolvedStaticMemberImports.java");

    @NonNull
    private static final URL SINGLE_IMPORT_IDENTITIES = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/ImportedAnnotationIdentities.java");

    @NonNull
    private static final URL UNRESOLVED_STATIC_IMPORT =
            requireClasspathResourceUrl("/test-cases/core/spoon/valid/import-priority/UnresolvedStaticMember.java");

    @NonNull
    private static final URL WILDCARD_BEFORE_SINGLE_TYPE_IMPORT = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/import-priority/WildcardBeforeSingleTypeImport.java");

    @NonNull
    private static final URL WILDCARD_OWNER_SOURCE_DECLARATIONS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/wildcard-owner-source-declarations/package-info.java");

    @ParameterizedTest
    @CsvSource({"DuplicateUnavailableStaticMembers, false", "ExplicitTypeWithDuplicateStaticMembers, true"})
    void collectTypeImportContext_duplicateUnavailableMembers_preservesLookupCertainty(
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
    void collectTypeImportContext_resolvedMembersWithDefiniteTypes_retainsBlockersAndTypePriority() {
        // Given
        CtCompilationUnit compilationUnit = parseCompilationUnitFromJavaFixtureResource(RESOLVED_MEMBER_IMPORTS);
        CtType<?> consumerType = compilationUnit.getDeclaredTypes().getFirst();
        List<CtType<?>> declaredTypes = SpoonTypeUtils.getAllTypes(compilationUnit);
        Map<String, CtType<?>> declaredTypesByCanonicalName = declaredTypes.stream()
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

    @Test
    void collectTypeImportContext_unavailableWildcardBeforeSingleType_retainsDefiniteImport() {
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
        // Spoon 11.5.0 collects imports through a HashSet; source order alone does not establish this precondition.
        // This fixture supplies the required order in the original parsed model without rearranging its imports.
        assertThat(SpoonImportUtils.isWildcardImport(
                        compilationUnit.getImports().getFirst()))
                .isTrue();
        assertThat(typeImportContext.getWildcardImportsInModelOrder())
                .singleElement()
                .isSameAs(compilationUnit.getImports().getFirst());
        assertThat(typeImportContext.getSrcOwnerTypesByWildcardImport()).isEmpty();
        assertThat(typeImportContext.hasWildcardImports()).isTrue();
        assertThat(dependencyKey).isEqualTo("java.lang.Deprecated");
    }

    @Test
    void collectTypeImportContext_unavailableWildcardBeforeSourceOwner_retainsLaterOwner() {
        // Given
        CtCompilationUnit compilationUnit =
                parseCompilationUnitFromJavaFixtureResource(WILDCARD_OWNER_SOURCE_DECLARATIONS);
        CtType<?> srcOwnerType = compilationUnit.getDeclaredTypes().getFirst();
        Map<String, CtType<?>> declaredTypesByCanonicalName = SpoonTypeUtils.getAllTypes(compilationUnit).stream()
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));
        CtImport srcOwnerImport = requireStaticWildcardImportForOwner(
                compilationUnit, formatCanonicalTypeName(srcOwnerType.getReference()));
        CtImport externalOwnerImport =
                requireStaticWildcardImportForOwner(compilationUnit, "java.lang.annotation.ElementType");

        // When
        SpoonTypeImportContext typeImportContext =
                collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);

        // Then
        assertThat(compilationUnit.getImports()).hasSize(2).allMatch(SpoonImportUtils::isWildcardImport);
        // The fixture requires this order in Spoon's original model, regardless of the order of source lines.
        assertThat(compilationUnit.getImports().getFirst()).isSameAs(externalOwnerImport);
        assertThat(compilationUnit.getImports().getLast()).isSameAs(srcOwnerImport);
        assertThat(typeImportContext.getWildcardImportsInModelOrder())
                .containsExactlyElementsOf(compilationUnit.getImports());
        assertThat(typeImportContext.getSrcOwnerTypesByWildcardImport()).containsOnlyKeys(srcOwnerImport);
        assertThat(typeImportContext.getSrcOwnerTypesByWildcardImport().get(srcOwnerImport))
                .isSameAs(srcOwnerType);
        assertThat(typeImportContext.getSrcOwnerTypesByWildcardImport()).doesNotContainKey(externalOwnerImport);
        assertThat(typeImportContext.hasWildcardImports()).isTrue();
    }

    @Test
    void extractCanonicalSingleImportTypeName_resolvedTypesAndMembers_returnsOnlyTypeNames() {
        // Given
        List<CtImport> typeImports = parseCompilationUnitFromJavaFixtureResource(RESOLVED_MEMBER_IMPORTS)
                .getImports();

        // When
        List<String> typeNames = typeImports.stream()
                .map(SpoonImportUtils::extractCanonicalSingleImportTypeName)
                .filter(Objects::nonNull)
                .toList();

        // Then
        assertThat(typeImports)
                .extracting(CtImport::getImportKind)
                .containsExactlyInAnyOrder(
                        CtImportKind.TYPE,
                        CtImportKind.TYPE,
                        CtImportKind.TYPE,
                        CtImportKind.FIELD,
                        CtImportKind.METHOD);
        assertThat(typeNames)
                .containsExactlyInAnyOrder(
                        "java.lang.Deprecated",
                        "java.lang.SuppressWarnings",
                        "annotationimports.ResolvedStaticMemberImports.NestedTag");
    }

    @Test
    void extractCanonicalSingleImportTypeName_unresolvedOrdinaryImport_retainsWrittenTypeName() {
        // Given
        CtImport typeImport =
                parseCompilationUnitFromJavaFixtureResource(SINGLE_IMPORT_IDENTITIES).getImports().stream()
                        .filter(importEntry -> importEntry instanceof CtUnresolvedImport unresolvedImport
                                && !unresolvedImport.isStatic())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Missing unresolved ordinary import"));

        // When
        String typeName = extractCanonicalSingleImportTypeName(typeImport);

        // Then
        assertThat(typeName).isEqualTo("annotationimportdependencies.ImportedOnlyTag");
    }

    @Test
    void extractCanonicalSingleImportTypeName_unresolvedStaticMember_returnsNull() {
        // Given
        CtImport memberImport = parseCompilationUnitFromJavaFixtureResource(UNRESOLVED_STATIC_IMPORT)
                .getImports()
                .getFirst();

        // When
        String typeName = extractCanonicalSingleImportTypeName(memberImport);

        // Then
        assertThat(memberImport)
                .isInstanceOfSatisfying(
                        CtUnresolvedImport.class,
                        unresolvedImport ->
                                assertThat(unresolvedImport.isStatic()).isTrue());
        assertThat(typeName).isNull();
    }

    @NonNull
    private static CtImport requireStaticWildcardImportForOwner(
            CtCompilationUnit compilationUnit, String ownerTypeName) {
        return compilationUnit.getImports().stream()
                .filter(typeImport ->
                        typeImport.getReference() instanceof CtTypeMemberWildcardImportReference wildcardImport
                                && ownerTypeName.equals(formatCanonicalTypeName(wildcardImport.getTypeReference())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No static wildcard import for " + ownerTypeName));
    }
}
