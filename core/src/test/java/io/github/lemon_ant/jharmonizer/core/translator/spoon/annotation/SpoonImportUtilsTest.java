// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseCompilationUnitFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonImportUtils.collectTypeImportContext;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonImportUtils.extractCanonicalSingleImportTypeName;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.formatCanonicalTypeName;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeTreeUtils;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
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
    private static final URL WILDCARD_OWNER_SOURCE_DECLARATIONS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/wildcard-owner-source-declarations/package-info.java");

    @Test
    void collectTypeImportContext_unavailableWildcardBeforeSourceOwner_retainsLaterOwner() {
        // Given
        CtCompilationUnit compilationUnit =
                parseCompilationUnitFromJavaFixtureResource(WILDCARD_OWNER_SOURCE_DECLARATIONS);
        CtType<?> srcOwnerType = compilationUnit.getDeclaredTypes().getFirst();
        Map<String, CtType<?>> declaredTypesByCanonicalName = SpoonTypeTreeUtils.streamAllTypes(compilationUnit)
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
