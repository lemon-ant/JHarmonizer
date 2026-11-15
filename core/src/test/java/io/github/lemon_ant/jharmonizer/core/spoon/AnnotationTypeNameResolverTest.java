// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.sortAnnotationGroups;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotationTypeNameResolverTest {

    @NonNull
    private static final URL EXTERNAL_REPEATABLE_CONTAINER_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/external-repeatable-container/ExternalRepeatableContainerUse.java");

    @NonNull
    private static final URL IMPORTED_EXTERNAL_REPEATABLE_CONTAINER_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/imported-external-repeatable-container/ImportedExternalRepeatableContainer.java");

    @NonNull
    private static final URL INHERITED_EXTERNAL_REPEATABLE_CONTAINER_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/inherited-external-repeatable-container/InheritedExternalRepeatableContainer.java");

    @NonNull
    private static final URL JAVA_LANG_OWNER_SHADOWING_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/java-lang-owner-shadowing/DottedJavaLangConsumer.java");

    @NonNull
    private static final URL LOCAL_ANNOTATION_SCOPES_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/context-name-confidence/LocalAnnotationScopes.java");

    @NonNull
    private static final URL LOCAL_INHERITED_ANNOTATION_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/context-name-confidence/LocalInheritedAnnotationConsumer.java");

    @NonNull
    private static final URL MODULE_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/20-non-type-compilation-units/input/module-info.java");

    @NonNull
    private static final URL MODULE_WITH_STATIC_METHOD_IMPORT_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/42-module-static-method-import/input/module-info.java");

    @NonNull
    private static final URL NON_ANNOTATION_SPELLING_COLLISION_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/qualified-annotation-spelling-collision/QualifiedAnnotationSpellingCollision.java");

    @NonNull
    private static final URL SAME_PACKAGE_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/implicit-java-lang-shadowing/JavaLangShadowConsumer.java");

    @NonNull
    private static final URL SHADOWED_EXTERNAL_ANNOTATION_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/qualified-annotation-shadowing/ExternalAnnotationShadowConsumer.java");

    @NonNull
    private static final URL SIMPLE_MODULE_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/reorder/21-non-type-compilation-units-opt-out-matrix/input/module-info.java");

    @NonNull
    private static final URL SINGLE_IMPORT_ANNOTATION_IDENTITIES_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/ImportedAnnotationIdentities.java");

    @NonNull
    private static final URL TYPE_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/qualified-annotation-fallback/QualifiedAnnotationFallback.java");

    @NonNull
    private static final URL UNAVAILABLE_SAME_PACKAGE_ANNOTATION_FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/SamePackageAnnotationUse.java");

    @NonNull
    private SpoonAstModel javaLangOwnerShadowingModel;

    private SpoonAstModel localScopeModel;
    private SpoonAstModel moduleModel;
    private AnnotationTypeNameResolver moduleResolver;
    private SpoonAstModel samePackageModel;
    private SpoonAstModel simpleModuleModel;
    private SpoonAstModel srcQualifiedArrayModel;
    private SpoonAstModel typeModel;
    private AnnotationTypeNameResolver typeResolver;
    private SpoonAstModel unnamedPackageModel;

    @BeforeAll
    void setUp() {
        javaLangOwnerShadowingModel = parseAstModelFromJavaFixtureResource(JAVA_LANG_OWNER_SHADOWING_FIXTURE);
        localScopeModel = parseAstModelFromJavaFixtureResource(LOCAL_ANNOTATION_SCOPES_FIXTURE);
        moduleModel = parseAstModelFromJavaFixtureResource(MODULE_FIXTURE);
        moduleResolver = createResolver(moduleModel.getCompilationUnit());
        samePackageModel = parseAstModelFromJavaFixtureResource(SAME_PACKAGE_FIXTURE);
        simpleModuleModel = parseAstModelFromJavaFixtureResource(SIMPLE_MODULE_FIXTURE);
        String srcQualifiedArrayCode = readClasspathResourceAsString(
                        "/test-cases/core/spoon/valid/multihop-annotation-binding/package-info.java")
                .replace(
                        "@MultiHopOwnerDerived.Inner.Tag []",
                        "@annotationmultihop.MultiHopAnnotationBase.Tag @MultiHopOwnerDerived.Inner.Tag []");
        srcQualifiedArrayModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcQualifiedArrayCode, Path.of("package-info.java")));
        typeModel = parseAstModelFromJavaFixtureResource(TYPE_FIXTURE);
        typeResolver = createResolver(typeModel.getCompilationUnit());
        // Reuse the fixture without its package clause; a global default-package Deprecated would affect other
        // fixtures.
        String unnamedPackageSrc =
                readClasspathResourceAsString(SAME_PACKAGE_FIXTURE).replace("package annotationjavalangshadow;", "");
        unnamedPackageModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(unnamedPackageSrc, Path.of("JavaLangShadowConsumer.java")));
    }

    @Test
    void resolveAnnotationTypeName_importedExternalContainer_confirmsCanonicalIdentityAndContainerKey() {
        // Given
        SpoonAstModel parsedModel =
                parseAstModelFromJavaFixtureResource(IMPORTED_EXTERNAL_REPEATABLE_CONTAINER_FIXTURE);
        String containerTypeName = "annotationimportedexternalcontainer.library.Stamps";
        int containerSrcOffset = parsedModel.getSrcCode().indexOf("@Stamps");
        int stampSrcOffset = parsedModel.getSrcCode().indexOf("@Stamp(\"a\")");
        CtAnnotation<?> containerUse = requireAnnotationUse(parsedModel, containerSrcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution containerTypeNameResolution =
                resolver.resolveAnnotationTypeName(containerSrcOffset, "Stamps");
        AnnotationTypeNameResolution stampTypeNameResolution =
                resolver.resolveAnnotationTypeName(stampSrcOffset, "Stamp");
        String stampDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(stampSrcOffset, "Stamp");
        String containerDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(containerSrcOffset, "Stamps");

        // Then
        assertThat(containerUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(containerTypeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(containerTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(stampTypeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName())
                    .isEqualTo("annotationimportedexternalcontainer.ImportedExternalRepeatableContainer.Stamp");
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(stampDependencyKey).isEqualTo(containerTypeName).doesNotStartWith("?.");
        assertThat(containerDependencyKey).isEqualTo(stampDependencyKey);
    }

    @Test
    void resolveAnnotationTypeName_importedOwnerWithSourceBinding_retainsUnconfirmedWrittenName() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(SINGLE_IMPORT_ANNOTATION_IDENTITIES_FIXTURE);
        String writtenTypeName = "Owner.NestedTag";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNotNull();
        assertThat(SpoonTypeUtils.streamAllTypes(parsedModel.getCompilationUnit()))
                .contains(annotationUse.getAnnotationType().getDeclaration());
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType()))
                .isEqualTo("annotationimportidentities.ImportSource.Owner.NestedTag");
        // Stage 3 matched only imported Owner; UNRESOLVED stops lookup despite Spoon supplying a source binding.
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
    }

    @Test
    void resolveAnnotationTypeName_inheritedExternalContainer_marksUnconfirmedRootAndPreservesOrder() {
        // Given
        SpoonAstModel parsedModel =
                parseAstModelFromJavaFixtureResource(INHERITED_EXTERNAL_REPEATABLE_CONTAINER_FIXTURE);
        int containerSrcOffset = parsedModel.getSrcCode().indexOf("@Owner.Tags");
        int tagSrcOffset = parsedModel.getSrcCode().indexOf("@Tag(\"a\")");
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());
        AnnotationSrcGroup originalGroup = parsedModel.getAnnotationSrcGroups().stream()
                .filter(annotationGroup -> annotationGroup.getStart() == containerSrcOffset)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No field annotation group at offset " + containerSrcOffset));
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragments();
        Comparator<AnnotationDescriptor> nameComparator = Comparator.comparing(AnnotationDescriptor::getName);

        // When
        String containerDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(containerSrcOffset, "Owner.Tags");
        String tagDependencyKey = dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(tagSrcOffset, "Tag");
        List<AnnotationSrcGroup> sortedGroups = sortAnnotationGroups(List.of(originalGroup), nameComparator);

        // Then
        assertThat(containerDependencyKey)
                .isEqualTo("annotationinheritedexternalcontainer.InheritedExternalRepeatableContainer.Owner.Tags")
                .doesNotStartWith("?.");
        assertThat(tagDependencyKey)
                .isEqualTo("?.annotationinheritedexternalcontainer.Derived.Tags")
                .startsWith("?.")
                .isNotEqualTo(containerDependencyKey);
        assertThat(originalAnnotations)
                .extracting(
                        annotationFragment -> annotationFragment.getDescriptor().getName())
                .containsExactly("Tags", "Tag");
        assertThat(originalAnnotations.getFirst().getDescriptor().getOrderPreservationGroupKeys())
                .contains(containerDependencyKey)
                .containsAnyElementsOf(
                        originalAnnotations.getLast().getDescriptor().getOrderPreservationGroupKeys());
        assertThat(originalAnnotations.getLast().getDescriptor().getOrderPreservationGroupKeys())
                .contains(tagDependencyKey);
        assertThat(sortedGroups)
                .singleElement()
                .satisfies(sortedGroup -> assertThat(sortedGroup.getAnnotationSrcFragments())
                        .containsExactlyElementsOf(originalAnnotations));
    }

    @ParameterizedTest
    @CsvSource({
        "root, annotationcontextconfidence.Tag, true",
        "unavailable, Tag, false",
        "available, annotationcontextconfidence.LocalAnnotationScopeBase.Tag, true",
        "anonymous, Tag, false"
    })
    void resolveAnnotationTypeName_localAndAnonymousTypeScopes_preservesLexicalPrecedence(
            @NonNull String fieldName, @NonNull String expectedTypeName, boolean expectedConfirmation) {
        // Given
        CtType<?> fixtureType = localScopeModel.getCompilationUnit().getDeclaredTypes().stream()
                .filter(srcType -> "LocalAnnotationScopes".equals(srcType.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing LocalAnnotationScopes fixture type"));
        CtField<?> annotatedField = fixtureType.getElements(new TypeFilter<CtField<?>>(CtField.class)).stream()
                .filter(field -> fieldName.equals(field.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing annotated field: " + fieldName));
        CtAnnotation<?> annotationUse = annotatedField.getAnnotations().getFirst();
        int srcOffset = annotationUse.getPosition().getSourceStart();
        AnnotationTypeNameResolver resolver = createResolver(localScopeModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(localScopeModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution = resolver.resolveAnnotationTypeName(srcOffset, "Tag");
        String dependencyKey = dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, "Tag");

        // Then
        assertScannedUse(localScopeModel, srcOffset);
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(expectedTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isEqualTo(expectedConfirmation);
        assertThat(dependencyKey).isEqualTo(expectedConfirmation ? expectedTypeName : "?." + expectedTypeName);
    }

    @Test
    void resolveAnnotationTypeName_localInheritedAnnotation_retainsWrittenNameForUnavailableParent() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(LOCAL_INHERITED_ANNOTATION_FIXTURE);
        String writtenTypeName = "Tag";
        String guessedTypeName = "annotationcontextconfidence.Tag";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        // LocalAnnotationBase.Tag is declared in another fixture file; Spoon instead guesses a top-level Tag.
        assertThat(parsedModel.getCompilationUnit().getImports()).isEmpty();
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType())).isEqualTo(guessedTypeName);
        assertThat(annotationUse.getAnnotationType().getPackage().getQualifiedName())
                .isEqualTo(parsedModel.getCompilationUnit().getDeclaredPackage().getQualifiedName());
        assertScannedUse(parsedModel, srcOffset);
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
    }

    @ParameterizedTest
    @CsvSource({
        "import static java.util.Collections.emptyList;, METHOD, java.lang.Deprecated, true",
        "import static java.lang.Integer.MAX_VALUE;, FIELD, java.lang.Deprecated, true",
        "import java.util.List;, TYPE, java.lang.Deprecated, true",
        "import java.lang.Deprecated;, TYPE, java.lang.Deprecated, true",
        "import java.util.*;, ALL_TYPES, Deprecated, false",
        "import static java.util.Collections.*;, ALL_STATIC_MEMBERS, Deprecated, false"
    })
    void resolveAnnotationTypeName_moduleImports_confirmsOnlyUnambiguousNames(
            @NonNull String importDeclaration,
            @NonNull CtImportKind expectedImportKind,
            @NonNull String expectedTypeName,
            boolean expectedCanonicalConfirmation) {
        // Given
        String moduleSrcCode = readClasspathResourceAsString(MODULE_WITH_STATIC_METHOD_IMPORT_FIXTURE)
                .replace("import static java.util.Collections.emptyList;", importDeclaration);
        SpoonAstModel parsedModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(moduleSrcCode, Path.of("module-info.java")));
        String writtenTypeName = "Deprecated";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(parsedModel.getCompilationUnit().getImports())
                .extracting(typeImport -> typeImport.getImportKind())
                .containsExactly(expectedImportKind);
        assertThat(parsedModel.getCompilationUnit().getDeclaredModule().getAnnotations())
                .isEmpty();
        assertScannedUse(parsedModel, srcOffset);
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(expectedTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isEqualTo(expectedCanonicalConfirmation);
        assertThat(dependencyKey).isEqualTo(expectedCanonicalConfirmation ? expectedTypeName : "?." + expectedTypeName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Thread", "Thread.State", "java.lang.Thread.State"})
    void resolveAnnotationTypeName_nonAnnotationModuleTypeInput_rejectsKnownRuntimeType(
            @NonNull String writtenTypeName) {
        // The source model is valid; supplying a non-annotation name intentionally violates the helper input contract.
        int srcOffset = moduleModel.getSrcCode().indexOf("@java.lang.Deprecated");

        // When / Then
        assertThatThrownBy(() -> moduleResolver.resolveAnnotationTypeName(srcOffset, writtenTypeName))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith(
                        "Expected an annotation declaration, but resolved runtime type java.lang.Thread");
    }

    @Test
    void resolveAnnotationTypeName_nonAnnotationSpellingCollision_retainsUnconfirmedName() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(NON_ANNOTATION_SPELLING_COLLISION_FIXTURE);
        String writtenTypeName = "EqualsAndHashCode.Exclude";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(parsedModel.getCompilationUnit().getDeclaredTypes())
                .filteredOn(srcType -> writtenTypeName.equals(formatCanonicalTypeName(srcType.getReference())))
                .singleElement()
                .satisfies(srcType -> assertThat(srcType.isAnnotationType()).isFalse());
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType()))
                .isEqualTo("lombok.EqualsAndHashCode.Exclude");
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
    }

    @Test
    void resolveAnnotationTypeName_qualifiedExternalContainer_preservesSourceRepeatableDependency() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(EXTERNAL_REPEATABLE_CONTAINER_FIXTURE);
        String containerTypeName = "annotationexternalcontainer.library.Stamps";
        int containerSrcOffset = parsedModel.getSrcCode().indexOf("@" + containerTypeName);
        int stampSrcOffset = parsedModel.getSrcCode().indexOf("@Stamp");
        CtAnnotation<?> containerUse = requireAnnotationUse(parsedModel, containerSrcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution containerTypeNameResolution =
                resolver.resolveAnnotationTypeName(containerSrcOffset, containerTypeName);
        AnnotationTypeNameResolution stampTypeNameResolution =
                resolver.resolveAnnotationTypeName(stampSrcOffset, "Stamp");
        String containerDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(containerSrcOffset, containerTypeName);
        String stampDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(stampSrcOffset, "Stamp");
        List<Set<String>> dependencyKeys = dependencyResolver.collectConservativeOrderPreservationGroupKeys(
                List.of(Set.of(containerDependencyKey), Set.of(stampDependencyKey)));

        // Then
        assertThat(containerUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(containerTypeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(containerTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
        assertThat(stampTypeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName())
                    .isEqualTo("annotationexternalcontainer.ExternalRepeatableContainerUse.Stamp");
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(containerDependencyKey).isEqualTo("?." + containerTypeName);
        assertThat(stampDependencyKey).isEqualTo(containerDependencyKey);
        assertThat(dependencyKeys).containsExactly(Set.of(containerDependencyKey), Set.of(containerDependencyKey));
        assertThat(parsedModel.getAnnotationSrcGroups())
                .filteredOn(group -> group.getStart() == containerSrcOffset)
                .singleElement()
                .satisfies(group -> {
                    assertThat(group.getAnnotationSrcFragments()).hasSize(2);
                    assertThat(group.getAnnotationSrcFragments()
                                    .getFirst()
                                    .getDescriptor()
                                    .getOrderPreservationGroupKeys())
                            .isEqualTo(dependencyKeys.getFirst());
                    assertThat(group.getAnnotationSrcFragments()
                                    .getLast()
                                    .getDescriptor()
                                    .getOrderPreservationGroupKeys())
                            .isEqualTo(dependencyKeys.getLast());
                });
    }

    @Test
    void resolveAnnotationTypeName_qualifiedExternalUseWithMatchingAst_keepsWrittenName() {
        // Given
        String writtenTypeName = "java.lang.Deprecated";
        int srcOffset = typeModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(typeModel, srcOffset);

        // When
        AnnotationTypeNameResolution typeNameResolution =
                typeResolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);

        // Then
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType())).isEqualTo(writtenTypeName);
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
    }

    @Test
    void resolveAnnotationTypeName_qualifiedModuleUseWithoutAst_confirmsRuntimeJavaLangName() {
        // Given
        String writtenTypeName = "java.lang.Deprecated";
        int srcOffset = moduleModel.getSrcCode().indexOf("@" + writtenTypeName);
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(moduleModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                moduleResolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(moduleModel.getCompilationUnit().getDeclaredTypes()).isEmpty();
        assertThat(moduleModel.getCompilationUnit().getImports()).isEmpty();
        assertThat(moduleModel.getCompilationUnit().getDeclaredModule().getAnnotations())
                .isEmpty();
        assertScannedUse(moduleModel, srcOffset);
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(dependencyKey).isEqualTo(writtenTypeName);
    }

    @Test
    void resolveAnnotationTypeName_relativeArrayUseWithoutAst_keepsNonCanonicalSpelling() {
        // Given
        String writtenTypeName = "ExternalOwner.Tag";
        int srcOffset = typeModel.getSrcCode().indexOf("@" + writtenTypeName);

        // When
        AnnotationTypeNameResolution typeNameResolution =
                typeResolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);

        // Then
        assertThat(findAnnotationUse(typeModel, srcOffset)).isEmpty();
        assertScannedUse(typeModel, srcOffset);
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName())
                    .isEqualTo(writtenTypeName)
                    .isNotEqualTo("annotationqualifiedfallback.library.ExternalOwner.Tag");
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"Thread.Tag", "java.lang.Thread.Tag", "java.lang.Deprecated"})
    void resolveAnnotationTypeName_samePackageOwnerObscuresJavaLang_retainsUnconfirmedWrittenName(
            @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = javaLangOwnerShadowingModel;
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertScannedUse(parsedModel, srcOffset);
        assertThat(parsedModel.getCompilationUnit().getImports()).isEmpty();
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
    }

    @Test
    void resolveAnnotationTypeName_shadowedExternalAstName_retainsWrittenName() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(SHADOWED_EXTERNAL_ANNOTATION_FIXTURE);
        String writtenTypeName = "EqualsAndHashCode.Exclude";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());
        String directTypeName = "ShadowAnnotationBase.Exclude";
        int directSrcOffset = parsedModel.getSrcCode().indexOf("@" + directTypeName);

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        AnnotationTypeNameResolution directTypeNameResolution =
                resolver.resolveAnnotationTypeName(directSrcOffset, directTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);
        String directDependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(directSrcOffset, directTypeName);
        List<Set<String>> dependencyKeys = dependencyResolver.collectConservativeOrderPreservationGroupKeys(
                List.of(Set.of(dependencyKey), Set.of(directDependencyKey)));

        // Then
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType()))
                .isEqualTo("lombok.EqualsAndHashCode.Exclude");
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
        assertThat(directTypeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName())
                    .isEqualTo("annotationqualifiedshadow.ShadowAnnotationBase.Exclude");
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
        assertThat(directDependencyKey).isEqualTo("annotationqualifiedshadow.ShadowAnnotationBase.Excludes");
        assertThat(dependencyKeys.getFirst()).containsAnyElementsOf(dependencyKeys.getLast());
    }

    @Test
    void resolveAnnotationTypeName_simpleModuleUseWithoutAst_resolvesJavaLangName() {
        // Given
        String writtenTypeName = "Deprecated";
        int srcOffset = simpleModuleModel.getSrcCode().indexOf("@" + writtenTypeName);
        AnnotationTypeNameResolver resolver = createResolver(simpleModuleModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(simpleModuleModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(simpleModuleModel.getCompilationUnit().getDeclaredModule().getAnnotations())
                .isEmpty();
        assertThat(simpleModuleModel.getCompilationUnit().getImports()).isEmpty();
        assertScannedUse(simpleModuleModel, srcOffset);
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo("java.lang.Deprecated");
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isTrue();
        });
        assertThat(dependencyKey).isEqualTo("java.lang.Deprecated");
    }

    @Test
    void resolveAnnotationTypeName_srcQualifiedArrayUseWithoutAst_retainsDeclaredName() {
        // Given
        String writtenTypeName = "annotationmultihop.MultiHopAnnotationBase.Tag";
        int srcOffset = srcQualifiedArrayModel.getSrcCode().indexOf("@" + writtenTypeName);
        AnnotationTypeNameResolver resolver = createResolver(srcQualifiedArrayModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(srcQualifiedArrayModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.streamAllTypes(srcQualifiedArrayModel.getCompilationUnit()))
                .filteredOn(CtType::isAnnotationType)
                .extracting(srcType -> formatCanonicalTypeName(srcType.getReference()))
                .contains(writtenTypeName);
        assertThat(findAnnotationUse(srcQualifiedArrayModel, srcOffset)).isEmpty();
        assertScannedUse(srcQualifiedArrayModel, srcOffset);
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
        // Matching a known canonical spelling must not turn an unbound use into confirmed container metadata.
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
        assertThat(srcQualifiedArrayModel.getAnnotationSrcGroups())
                .filteredOn(group -> group.getStart() == srcOffset)
                .singleElement()
                .satisfies(group -> {
                    assertThat(group.getAnnotationSrcFragments()).hasSize(2);
                    assertThat(group.getAnnotationSrcFragments()
                                    .getFirst()
                                    .getDescriptor()
                                    .getOrderPreservationGroupKeys())
                            .containsAnyElementsOf(group.getAnnotationSrcFragments()
                                    .getLast()
                                    .getDescriptor()
                                    .getOrderPreservationGroupKeys());
                });
    }

    @Test
    void resolveAnnotationTypeName_suffixOfExternalAstName_retainsWrittenName() {
        // Given
        String writtenTypeName = "EqualsAndHashCode.Exclude";
        int srcOffset = typeModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(typeModel, srcOffset);
        String boundTypeName = formatCanonicalTypeName(annotationUse.getAnnotationType());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(typeModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                typeResolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(boundTypeName).isEqualTo("lombok.EqualsAndHashCode.Exclude");
        assertThat(boundTypeName).endsWith(writtenTypeName).isNotEqualTo(writtenTypeName);
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(typeNameResolution).satisfies(annotationTypeNameResolution -> {
            assertThat(annotationTypeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
            assertThat(annotationTypeNameResolution.isCanonicalTypeNameConfirmed())
                    .isFalse();
        });
        assertThat(dependencyKey).isEqualTo("?." + writtenTypeName);
    }

    @Test
    void resolveAnnotationTypeName_unavailableSamePackageAnnotation_retainsUnconfirmedQualifiedName() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(UNAVAILABLE_SAME_PACKAGE_ANNOTATION_FIXTURE);
        String writtenTypeName = "ImportedOnlyTag";
        String qualifiedTypeName = "annotationimportdependencies.ImportedOnlyTag";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(parsedModel.getCompilationUnit().getImports()).isEmpty();
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(annotationUse.getAnnotationType().getPackage().getQualifiedName())
                .isEqualTo(parsedModel.getCompilationUnit().getDeclaredPackage().getQualifiedName());
        assertScannedUse(parsedModel, srcOffset);
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(qualifiedTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?." + qualifiedTypeName);
    }

    @ParameterizedTest(name = "unnamedPackage={0}")
    @ValueSource(booleans = {false, true})
    void resolveAnnotationTypeName_unavailableSamePackageHomonym_retainsUnconfirmedWrittenName(boolean unnamedPackage) {
        // Given
        SpoonAstModel parsedModel = unnamedPackage ? unnamedPackageModel : samePackageModel;
        AnnotationTypeNameResolver resolver = createResolver(parsedModel.getCompilationUnit());
        AnnotationDependencyKeyResolver dependencyResolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());
        String writtenTypeName = "Deprecated";
        int srcOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> annotationUse = requireAnnotationUse(parsedModel, srcOffset);

        // When
        AnnotationTypeNameResolution typeNameResolution =
                resolver.resolveAnnotationTypeName(srcOffset, writtenTypeName);
        String dependencyKey =
                dependencyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(srcOffset, writtenTypeName);

        // Then
        assertThat(parsedModel.getCompilationUnit().getDeclaredPackage().isUnnamedPackage())
                .isEqualTo(unnamedPackage);
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType())).isEqualTo("java.lang.Deprecated");
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(typeNameResolution.getAnnotationTypeName()).isEqualTo(writtenTypeName);
        assertThat(typeNameResolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?.Deprecated");
    }

    private static void assertScannedUse(SpoonAstModel parsedModel, int srcOffset) {
        assertThat(parsedModel.getAnnotationSrcGroups().stream()
                        .flatMap(annotationGroup -> annotationGroup.getAnnotationSrcFragments().stream()))
                .anyMatch(annotationFragment -> annotationFragment.getAnnotationStart() == srcOffset);
    }

    @NonNull
    private static AnnotationTypeNameResolver createResolver(CtCompilationUnit compilationUnit) {
        Map<String, CtType<?>> declarationsByName = SpoonTypeUtils.streamAllTypes(compilationUnit)
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));
        SpoonTypeImportContext typeImportContext =
                SpoonImportUtils.collectTypeImportContext(compilationUnit, declarationsByName);
        return new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
    }

    @NonNull
    private static Optional<CtAnnotation<?>> findAnnotationUse(SpoonAstModel parsedModel, int srcOffset) {
        return parsedModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(srcType -> srcType.getElements(new TypeFilter<CtAnnotation<?>>(CtAnnotation.class)).stream())
                .filter(annotationUse -> annotationUse.getPosition().isValidPosition()
                        && annotationUse.getPosition().getSourceStart() == srcOffset)
                .findFirst();
    }

    @NonNull
    private static CtAnnotation<?> requireAnnotationUse(SpoonAstModel parsedModel, int srcOffset) {
        return findAnnotationUse(parsedModel, srcOffset)
                .orElseThrow(() -> new IllegalStateException("No annotation AST use at offset " + srcOffset));
    }
}
