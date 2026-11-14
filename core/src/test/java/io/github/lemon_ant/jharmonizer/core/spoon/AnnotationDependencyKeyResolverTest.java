// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseCompilationUnitFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.lang.annotation.Repeatable;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeMemberWildcardImportReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

class AnnotationDependencyKeyResolverTest {

    @NonNull
    private static final URL ANNOTATION_TYPE_KIND_VALIDATION = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/annotation-type-kind-validation/AnnotationTypeKindValidation.java");

    @NonNull
    private static final URL COMPETING_INHERITED_ANNOTATION_BRANCH = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/competing-inherited-annotation-branch/package-info.java");

    @NonNull
    private static final URL COMPETING_WILDCARD_ANNOTATION_BINDINGS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/competing-wildcard-annotation-bindings/package-info.java");

    @NonNull
    private static final URL IMPORTED_ORDINARY_TYPE_SHADOWING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/ImportedOrdinaryTypeShadowing.java");

    @NonNull
    private static final URL INHERITED_ANNOTATION_OWNER_BINDING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/inherited-annotation-owner-binding/package-info.java");

    @NonNull
    private static final URL INHERITED_MEMBER_TYPE_BRANCHES = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/inherited-member-type-branches/InheritedMemberTypeBranches.java");

    @NonNull
    private static final URL INHERITED_NAME_SHADOWING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/inherited-name-shadowing/InheritedAnnotationNameShadowing.java");

    @NonNull
    private static final URL INHERITED_TREE_ANNOTATION_BINDING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/inherited-tree-annotation-binding/package-info.java");

    @NonNull
    private static final URL MULTIHOP_ANNOTATION_BINDING =
            requireClasspathResourceUrl("/test-cases/core/spoon/valid/multihop-annotation-binding/package-info.java");

    @NonNull
    private static final URL MULTIHOP_UNAVAILABLE_ANNOTATION_BINDING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/multihop-unavailable-annotation-binding/package-info.java");

    @NonNull
    private static final URL ORDINARY_WILDCARD_UNAVAILABLE_ANNOTATION_BINDING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/ordinary-wildcard-unavailable-annotation-binding/package-info.java");

    @NonNull
    private static final URL PACKAGE_ANNOTATION_BINDING =
            requireClasspathResourceUrl("/test-cases/core/spoon/valid/package-annotation-binding/package-info.java");

    @NonNull
    private static final URL REQUIRED_INHERITED_ANNOTATION_PARENT = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/required-inherited-annotation-parent/package-info.java");

    @NonNull
    private static final URL SINGLE_IMPORT_ANNOTATION_IDENTITIES = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/ImportedAnnotationIdentities.java");

    @NonNull
    private static final URL STATIC_WILDCARD_UNAVAILABLE_ANNOTATION_BINDING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/static-wildcard-unavailable-annotation-binding/package-info.java");

    @NonNull
    private static final URL UNAVAILABLE_TREE_ANNOTATION_PARENT = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/unavailable-tree-annotation-parent/package-info.java");

    @NonNull
    private static final URL WILDCARD_IMPORTED_MEMBER_SCOPE = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/wildcard-imported-member-scope/package-info.java");

    @NonNull
    private static final URL WILDCARD_ORDINARY_TYPE_SHADOWING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/single-import-annotation-identities/WildcardOrdinaryTypeShadowing.java");

    @NonNull
    private static final URL WILDCARD_OWNER_SOURCE_DECLARATIONS = requireClasspathResourceUrl(
            "/test-cases/core/spoon/valid/wildcard-owner-source-declarations/package-info.java");

    @NonNull
    private static final URL WILDCARD_PACKAGE_NAME_SHADOWING = requireClasspathResourceUrl(
            "/test-cases/core/spoon/invalid/wildcard-package-name-shadowing/package-info.java");

    @Test
    void parseJavaSrcFile_knownNonAnnotationLookup_rejectsFile() {
        assertThatThrownBy(() -> parseAstModelFromJavaFixtureResource(ANNOTATION_TYPE_KIND_VALIDATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected an annotation declaration, but resolved source type "
                        + "annotationtypekindvalidation.AnnotationTypeKindValidation.SelfNameDerived");
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_competingUnavailableWildcardScope_preservesUnknownIdentity() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(COMPETING_WILDCARD_ANNOTATION_BINDINGS);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@Tag");
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, "Tag");

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit, packageAnnotation, "annotationwildcardorder.AvailableWildcardOwner.Tag");
        assertThat(identity).isEqualTo("?.Tag");
    }

    @Test
    void
            resolveAnnotationTypeNameOrRootContainerTypeName_directDeclarationWithUnavailableOwnerParent_resolvesDeclaredType() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(UNAVAILABLE_TREE_ANNOTATION_PARENT);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "UnavailableTreeAnnotationOwner.Direct.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(identity).isEqualTo("annotationtreeunavailable.UnavailableTreeAnnotationOwner.Direct.Tag");
    }

    @ParameterizedTest
    @CsvSource({
        "UnavailableChainEnclosingDerived, inherited, Tag",
        "UnavailableChainConsumer, qualified, UnavailableChainOwnerDerived.Inner.Tag"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_hiddenUnavailableFieldAncestor_resolvesSourceContainer(
            @NonNull String ownerTypeName, @NonNull String fieldName, @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(MULTIHOP_UNAVAILABLE_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        CtAnnotation<?> annotationUse = requireFieldAnnotation(parsedCompilationUnit, ownerTypeName, fieldName);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), writtenTypeName);

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit, annotationUse, "annotationchainunavailable.UnavailableChainAnnotationBase.Tag");
        assertThat(identity).isEqualTo("annotationchainunavailable.UnavailableChainAnnotationBase.Tags");
    }

    @ParameterizedTest
    @CsvSource({
        "UnavailableTreeAnnotationDerived.Tag, annotationtreeunavailable.UnavailableTreeAnnotationBase.Tag",
        "UnavailableTreeAnnotationOwner.Derived.Tag, annotationtreeunavailable.UnavailableTreeAnnotationOwner.Base.Tag"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_hiddenUnavailableOwnerParent_resolvesDeclaredType(
            @NonNull String writtenTypeName, @NonNull String expectedSourceTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(UNAVAILABLE_TREE_ANNOTATION_PARENT);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.collectEnclosingSrcTypes(
                        SpoonTypeUtils.getAllTypes(parsedCompilationUnit), annotationSrcStartOffset))
                .isEmpty();
        assertThat(SpoonTypeUtils.getAllTypes(parsedCompilationUnit))
                .as("The source declaration hides the unavailable ancestor on its inheritance branch")
                .contains(packageAnnotation.getAnnotationType().getDeclaration());
        assertThat(formatCanonicalTypeName(packageAnnotation.getAnnotationType()))
                .isEqualTo(expectedSourceTypeName);
        assertThat(identity).isEqualTo(expectedSourceTypeName);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "UnavailableChainDerived.Tag",
                "annotationchainunavailable.UnavailableChainDerived.Tag",
                "UnavailableChainOwnerDerived.Inner.Tag",
                "annotationchainunavailable.UnavailableChainOwnerDerived.Inner.Tag"
            })
    void resolveAnnotationTypeNameOrRootContainerTypeName_hiddenUnavailablePackageAncestor_resolvesSourceContainer(
            @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(MULTIHOP_UNAVAILABLE_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit,
                packageAnnotation,
                "annotationchainunavailable.UnavailableChainAnnotationBase.Tag");
        assertThat(identity).isEqualTo("annotationchainunavailable.UnavailableChainAnnotationBase.Tags");
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_importedOrdinaryHomonym_resolvesInheritedAnnotation() {
        // Given
        CtCompilationUnit parsedCompilationUnit =
                parseCompilationUnitFromJavaFixtureResource(IMPORTED_ORDINARY_TYPE_SHADOWING);
        CtAnnotation<?> annotationUse = parsedCompilationUnit
                .getDeclaredTypes()
                .getFirst()
                .getElements(new TypeFilter<CtAnnotation<?>>(CtAnnotation.class))
                .getFirst();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), "Entry");

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit,
                annotationUse,
                "annotationimportidentities.ImportedOrdinaryTypeShadowing.Base.Entry");
        assertThat(annotationUse.getAnnotationType().getDeclaration().isAnnotationType())
                .isTrue();
        assertThat(identity).isEqualTo("annotationimportidentities.ImportedOrdinaryTypeShadowing.Base.Entry");
    }

    @Test
    void
            resolveAnnotationTypeNameOrRootContainerTypeName_inheritedArrayAnnotationWithoutAstUse_resolvesSourceContainer() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(MULTIHOP_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "MultiHopOwnerDerived.Inner.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().lastIndexOf("@" + writtenTypeName);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(parsedCompilationUnit.getDeclaredTypes().stream()
                        .flatMap(srcType ->
                                srcType.getElements(new TypeFilter<CtAnnotation<?>>(CtAnnotation.class)).stream()))
                .noneMatch(annotationUse -> annotationUse.getPosition().isValidPosition()
                        && annotationUse.getPosition().getSourceStart() == annotationSrcStartOffset);
        assertThat(identity).isEqualTo("annotationmultihop.MultiHopAnnotationBase.Tags");
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_inheritedIntermediateOwner_resolvesDeclaredType() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(INHERITED_ANNOTATION_OWNER_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "InheritedAnnotationOwnerDerived.Inner.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.getAllTypes(parsedCompilationUnit))
                .contains(packageAnnotation.getAnnotationType().getDeclaration());
        assertThat(formatCanonicalTypeName(packageAnnotation.getAnnotationType()))
                .isEqualTo("annotationtreeinheritedowner.InheritedAnnotationOwnerBase.Inner.Tag");
        assertThat(identity).isEqualTo("annotationtreeinheritedowner.InheritedAnnotationOwnerBase.Inner.Tag");
    }

    @ParameterizedTest
    @CsvSource({
        "diamond, MemberDiamondOwner.Tag, annotationmemberbranches.MemberDiamondRoot.Tag",
        "hiddenAncestor, MemberHidingOwner.Inner.Tag, annotationmemberbranches.MemberVisibleBranch.Inner.Tag"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_inheritedMemberBranches_resolvesUniqueVisibleDeclaration(
            @NonNull String fieldName, @NonNull String writtenTypeName, @NonNull String expectedTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(INHERITED_MEMBER_TYPE_BRANCHES);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        CtAnnotation<?> annotationUse =
                requireFieldAnnotation(parsedCompilationUnit, "InheritedMemberTypeBranches", fieldName);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), writtenTypeName);

        // Then
        assertSourceAnnotationBinding(parsedCompilationUnit, annotationUse, expectedTypeName);
        assertThat(identity).isEqualTo(expectedTypeName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"InheritedTreeAnnotationDerived.Tag", "InheritedTreeAnnotationOwner.NestedDerived.Tag"})
    void
            resolveAnnotationTypeNameOrRootContainerTypeName_inheritedPackageAnnotationInSourceTree_resolvesSourceContainer(
                    @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(INHERITED_TREE_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.collectEnclosingSrcTypes(
                        SpoonTypeUtils.getAllTypes(parsedCompilationUnit), annotationSrcStartOffset))
                .as("Package annotations have no enclosing type before source-tree lookup")
                .isEmpty();
        assertThat(parsedCompilationUnit.getImports()).isEmpty();
        assertThat(SpoonTypeUtils.getAllTypes(parsedCompilationUnit))
                .contains(packageAnnotation.getAnnotationType().getDeclaration());
        assertThat(formatCanonicalTypeName(packageAnnotation.getAnnotationType()))
                .isEqualTo("annotationtreebindings.InheritedTreeAnnotationBase.Tag");
        assertThat(identity).isEqualTo("annotationtreebindings.InheritedTreeAnnotationBase.Tags");
    }

    @ParameterizedTest
    @CsvSource({
        "selfName, SelfNameDerived, SelfNameBase.SelfNameDerived, SelfNameDerived",
        "qualifiedName, QualifiedNameDerived.Tag, QualifiedNameBase.QualifiedNameDerived.Tag, QualifiedNameDerived.Tag"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_knownNonAnnotationLookup_rejectsBeforeCaching(
            @NonNull String fieldName,
            @NonNull String writtenTypeName,
            @NonNull String boundTypeName,
            @NonNull String resolvedTypeName) {
        // Given
        CtCompilationUnit parsedCompilationUnit =
                parseCompilationUnitFromJavaFixtureResource(ANNOTATION_TYPE_KIND_VALIDATION);
        CtAnnotation<?> annotationUse = requireAnnotationOnDescendantField(
                parsedCompilationUnit.getDeclaredTypes().getFirst(), fieldName);
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = annotationUse.getPosition().getSourceStart();
        String fixtureTypePrefix = "annotationtypekindvalidation.AnnotationTypeKindValidation.";
        String expectedFailureMessage =
                "Expected an annotation declaration, but resolved source type " + fixtureTypePrefix + resolvedTypeName;

        // When
        Throwable initialFailure = catchThrowable(() ->
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName));
        Throwable repeatedFailure = catchThrowable(() ->
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName));

        // Then
        assertSourceAnnotationBinding(parsedCompilationUnit, annotationUse, fixtureTypePrefix + boundTypeName);
        assertThat(annotationUse.getAnnotationType().getDeclaration().isAnnotationType())
                .isTrue();
        assertThat(initialFailure).isInstanceOf(IllegalStateException.class).hasMessage(expectedFailureMessage);
        assertThat(repeatedFailure).isInstanceOf(IllegalStateException.class).hasMessage(expectedFailureMessage);
    }

    @ParameterizedTest
    @CsvSource({"local, LocalBase.Tag", "anonymous, AnonymousBase.Tag"})
    void resolveAnnotationTypeNameOrRootContainerTypeName_localAndAnonymousInheritance_resolvesSourceAnnotation(
            @NonNull String fieldName, @NonNull String boundTypeName) {
        // Given
        CtCompilationUnit parsedCompilationUnit =
                parseCompilationUnitFromJavaFixtureResource(ANNOTATION_TYPE_KIND_VALIDATION);
        CtAnnotation<?> annotationUse = requireAnnotationOnDescendantField(
                parsedCompilationUnit.getDeclaredTypes().getFirst(), fieldName);
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String expectedTypeName = "annotationtypekindvalidation.AnnotationTypeKindValidation." + boundTypeName;

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), "Tag");

        // Then
        assertSourceAnnotationBinding(parsedCompilationUnit, annotationUse, expectedTypeName);
        assertThat(identity).isEqualTo(expectedTypeName);
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_multihopEnclosingInheritance_resolvesSourceContainer() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(MULTIHOP_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        CtAnnotation<?> annotationUse =
                requireFieldAnnotation(parsedCompilationUnit, "MultiHopEnclosingDerived", "value");

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), "Tag");

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit, annotationUse, "annotationmultihop.MultiHopAnnotationBase.Tag");
        assertThat(identity).isEqualTo("annotationmultihop.MultiHopAnnotationBase.Tags");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "MultiHopOwnerDerived.Inner.Tag",
                "annotationmultihop.MultiHopOwnerDerived.Inner.Tag",
                "Derived.Inner.Tag"
            })
    void resolveAnnotationTypeNameOrRootContainerTypeName_multihopInheritedPackageAnnotation_resolvesSourceContainer(
            @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(MULTIHOP_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.collectEnclosingSrcTypes(
                        SpoonTypeUtils.getAllTypes(parsedCompilationUnit), annotationSrcStartOffset))
                .isEmpty();
        assertSourceAnnotationBinding(
                parsedCompilationUnit, packageAnnotation, "annotationmultihop.MultiHopAnnotationBase.Tag");
        assertThat(identity).isEqualTo("annotationmultihop.MultiHopAnnotationBase.Tags");
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_privateWildcardTypeCandidate_rejectsLookup() {
        // Given
        CtCompilationUnit parsedCompilationUnit =
                parseCompilationUnitFromJavaFixtureResource(WILDCARD_ORDINARY_TYPE_SHADOWING);
        CtAnnotation<?> annotationUse =
                requireFieldAnnotation(parsedCompilationUnit, "WildcardOrdinaryTypeShadowing", "value");
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);

        // When
        Throwable failure = catchThrowable(() -> resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), "Tag"));

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit,
                annotationUse,
                "annotationimportidentities.WildcardOrdinaryTypeShadowing.VisibleOwner.Tag");
        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected an annotation declaration, but resolved source type "
                        + "annotationimportidentities.WildcardOrdinaryTypeShadowing.HiddenOwner.Tag");
    }

    @Test
    void
            resolveAnnotationTypeNameOrRootContainerTypeName_qualifiedInheritedPackageAnnotation_resolvesSourceContainer() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(PACKAGE_ANNOTATION_BINDING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "annotationbindings.PackageAnnotationDerived.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                parsedCompilationUnit.getDeclaredPackage().getAnnotations().getFirst();
        CtType<?> declaredAnnotationType =
                parsedCompilationUnit.getDeclaredTypes().getFirst().getNestedType("Tag");

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(SpoonTypeUtils.collectEnclosingSrcTypes(
                        SpoonTypeUtils.getAllTypes(parsedCompilationUnit), annotationSrcStartOffset))
                .as("Stage 1 has no enclosing type for the package annotation")
                .isEmpty();
        assertThat(parsedCompilationUnit.getDeclaredTypes())
                .as("Stage 2 cannot start at the written package prefix annotationbindings")
                .extracting(CtType::getSimpleName)
                .containsExactly("PackageAnnotationBase", "PackageAnnotationDerived");
        assertThat(parsedCompilationUnit.getImports())
                .as("Stage 3 has no imports to resolve")
                .isEmpty();
        assertThat(packageAnnotation.getAnnotationType().getDeclaration())
                .as("The original Spoon reference resolves to an annotation declaration in this file")
                .isSameAs(declaredAnnotationType);
        assertThat(formatCanonicalTypeName(packageAnnotation.getAnnotationType()))
                .as("Without stage 4, the spelling fallback cannot identify the source repeatable container")
                .isEqualTo("annotationbindings.PackageAnnotationBase.Tag")
                .isNotEqualTo(writtenTypeName);
        assertThat(identity).isEqualTo("annotationbindings.PackageAnnotationBase.Tags");
    }

    @ParameterizedTest
    @CsvSource({
        "sourceAnnotation, Tag, annotationimportidentities.ImportSource.Tag",
        "sourceOwner, Owner.NestedTag, ?.Owner.NestedTag",
        "unavailableAnnotation, ImportedOnlyTag, annotationimportdependencies.ImportedOnlyTag",
        "platformAnnotation, Deprecated, java.lang.Deprecated"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_singleTypeImports_returnsExpectedIdentity(
            @NonNull String fieldName, @NonNull String writtenTypeName, @NonNull String expectedIdentity) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(SINGLE_IMPORT_ANNOTATION_IDENTITIES);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        CtAnnotation<?> annotationUse =
                requireFieldAnnotation(parsedCompilationUnit, "ImportedAnnotationIdentities", fieldName);
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String importedSimpleName = writtenTypeName.split("\\.")[0];
        CtImport matchingImport = parsedCompilationUnit.getImports().stream()
                .filter(typeImport -> {
                    String importedTypeName = SpoonImportUtils.extractCanonicalSingleImportTypeName(typeImport);
                    return importedTypeName != null && importedTypeName.endsWith('.' + importedSimpleName);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No single type import named " + importedSimpleName));

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), writtenTypeName);

        // Then
        assertThat(identity).isEqualTo(expectedIdentity);
        if ("unavailableAnnotation".equals(fieldName)) {
            assertThat(matchingImport).isInstanceOf(CtUnresolvedImport.class);
            assertThat(matchingImport.getReference()).isNull();
        } else {
            assertThat(matchingImport.getReference()).isInstanceOf(CtTypeReference.class);
            CtTypeReference<?> importedTypeReference = (CtTypeReference<?>) matchingImport.getReference();
            if ("platformAnnotation".equals(fieldName)) {
                assertThat(importedTypeReference.getDeclaration()).isNull();
            } else {
                assertSourceAnnotationBinding(
                        parsedCompilationUnit,
                        annotationUse,
                        "annotationimportidentities.ImportSource." + writtenTypeName);
                CtType<?> annotationDeclaration =
                        annotationUse.getAnnotationType().getDeclaration();
                CtType<?> expectedDeclaration = "sourceAnnotation".equals(fieldName)
                        ? annotationDeclaration
                        : annotationDeclaration.getDeclaringType();
                assertThat(importedTypeReference.getDeclaration()).isNotNull().isSameAs(expectedDeclaration);
                assertThat(importedTypeReference.getDeclaration().isAnnotationType())
                        .isEqualTo("sourceAnnotation".equals(fieldName));
            }
        }
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_sourceAndJdkWildcardOwners_preservesUnknownIdentity() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(WILDCARD_OWNER_SOURCE_DECLARATIONS);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "Nested.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);
        String srcOwnerTypeName = "annotationwildcarddeclarations.WildcardAnnotationOwners";
        Map<String, CtTypeReference<?>> wildcardOwnerReferencesByCanonicalName =
                parsedCompilationUnit.getImports().stream()
                        .map(CtImport::getReference)
                        .filter(CtTypeMemberWildcardImportReference.class::isInstance)
                        .map(CtTypeMemberWildcardImportReference.class::cast)
                        .map(CtTypeMemberWildcardImportReference::getTypeReference)
                        .collect(Collectors.toMap(
                                SpoonTypeUtils::formatCanonicalTypeName, ownerTypeReference -> ownerTypeReference));

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(parsedCompilationUnit.getImports())
                .hasSize(2)
                .allSatisfy(typeImport ->
                        assertThat(typeImport.getReference()).isInstanceOf(CtTypeMemberWildcardImportReference.class));
        assertThat(wildcardOwnerReferencesByCanonicalName)
                .containsOnlyKeys(srcOwnerTypeName, "java.lang.annotation.ElementType");
        assertThat(wildcardOwnerReferencesByCanonicalName.get(srcOwnerTypeName).getDeclaration())
                .isNotNull()
                .isSameAs(parsedCompilationUnit.getDeclaredTypes().getFirst());
        assertThat(SpoonTypeUtils.getAllTypes(parsedCompilationUnit))
                .contains(wildcardOwnerReferencesByCanonicalName
                        .get(srcOwnerTypeName)
                        .getDeclaration());
        assertThat(wildcardOwnerReferencesByCanonicalName
                        .get("java.lang.annotation.ElementType")
                        .getDeclaration())
                .isNull();
        assertSourceAnnotationBinding(parsedCompilationUnit, packageAnnotation, srcOwnerTypeName + ".Nested.Tag");
        assertThat(identity).isEqualTo("?.Nested.Tag");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"CompetingAnnotationDerived.Tag", "annotationcompetingparents.CompetingAnnotationDerived.Tag"})
    void resolveAnnotationTypeNameOrRootContainerTypeName_unavailableCompetingParent_preservesUnknownIdentity(
            @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(COMPETING_INHERITED_ANNOTATION_BRANCH);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit, packageAnnotation, "annotationcompetingparents.CompetingAnnotationBase.Tag");
        assertThat(identity).isEqualTo("?." + writtenTypeName);
    }

    @ParameterizedTest
    @CsvSource({
        "RequiredParentFieldOwner, inherited, Tag",
        "RequiredParentFieldConsumer, qualified, RequiredParentOwnerDerived.Inner.Tag"
    })
    void resolveAnnotationTypeNameOrRootContainerTypeName_unavailableRequiredFieldParent_preservesUnknownIdentity(
            @NonNull String ownerTypeName, @NonNull String fieldName, @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(REQUIRED_INHERITED_ANNOTATION_PARENT);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        CtAnnotation<?> annotationUse = requireFieldAnnotation(parsedCompilationUnit, ownerTypeName, fieldName);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), writtenTypeName);

        // Then
        assertThat(annotationUse.getAnnotationType().getDeclaration()).isNull();
        assertThat(identity).isEqualTo("?." + writtenTypeName);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "RequiredParentDerived.Tag",
                "annotationrequiredparents.RequiredParentDerived.Tag",
                "RequiredParentOwnerDerived.Inner.Tag",
                "annotationrequiredparents.RequiredParentOwnerDerived.Inner.Tag"
            })
    void resolveAnnotationTypeNameOrRootContainerTypeName_unavailableRequiredPackageParent_preservesUnconfirmedIdentity(
            @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(REQUIRED_INHERITED_ANNOTATION_PARENT);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(packageAnnotation.getAnnotationType().getDeclaration()).isNull();
        // A source root blocks on the missing parent; a package prefix reaches stage 5. Both remain unconfirmed.
        assertThat(identity).isEqualTo("?." + writtenTypeName);
    }

    @ParameterizedTest
    @MethodSource("wildcardHiddenAncestorCases")
    void resolveAnnotationTypeNameOrRootContainerTypeName_wildcardHiddenUnavailableAncestor_resolvesSourceContainer(
            @NonNull URL fixture, @NonNull String expectedTypeName, @NonNull String expectedContainerTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(fixture);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "Derived.Inner.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertSourceAnnotationBinding(parsedCompilationUnit, packageAnnotation, expectedTypeName);
        assertThat(identity).isEqualTo(expectedContainerTypeName);
    }

    @Test
    void
            resolveAnnotationTypeNameOrRootContainerTypeName_wildcardOwnerMatchesWrittenQualifier_resolvesImportedMember() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(WILDCARD_IMPORTED_MEMBER_SCOPE);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "Nested.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit,
                packageAnnotation,
                "annotationwildcardmembers.WildcardImportSource.Other.Nested.Tag");
        assertThat(identity).isEqualTo("annotationwildcardmembers.WildcardImportSource.Other.Nested.Tag");
    }

    @Test
    void resolveAnnotationTypeNameOrRootContainerTypeName_wildcardTypeShadowsPackagePrefix_preservesUnknownIdentity() {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(WILDCARD_PACKAGE_NAME_SHADOWING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);
        String writtenTypeName = "annotationwildcardshadow.Derived.Tag";
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);
        CtAnnotation<?> packageAnnotation =
                requirePackageAnnotationAtSrcOffset(parsedCompilationUnit, annotationSrcStartOffset);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertSourceAnnotationBinding(
                parsedCompilationUnit, packageAnnotation, "annotationwildcardshadow.WildcardShadowAnnotationBase.Tag");
        assertThat(identity).isEqualTo("?." + writtenTypeName);
    }

    @ParameterizedTest
    @CsvSource({
        "ExplicitTypeAfterStaticMember, Deprecated, java.lang.Deprecated",
        "ExplicitTypeBeforeStaticMember, Deprecated, java.lang.Deprecated",
        "UnresolvedStaticMember, Deprecated, ?.Deprecated",
        "ResolvedStaticMemberImports, Deprecated, java.lang.Deprecated",
        "ResolvedStaticMemberImports, SuppressWarnings, java.lang.SuppressWarnings"
    })
    void resolveOrderPreservationGroupKey_importPriority_returnsExpectedIdentity(
            @NonNull String fixtureName, @NonNull String writtenTypeName, @NonNull String expectedIdentity) {
        // Given
        URL fixtureResource =
                requireClasspathResourceUrl("/test-cases/core/spoon/valid/import-priority/" + fixtureName + ".java");
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(fixtureResource);
        AnnotationDependencyKeyResolver resolver =
                new AnnotationDependencyKeyResolver(parsedModel.getCompilationUnit());
        int annotationSrcStartOffset = parsedModel.getSrcCode().indexOf("@" + writtenTypeName);

        // When
        String identity =
                resolver.resolveAnnotationTypeNameOrRootContainerTypeName(annotationSrcStartOffset, writtenTypeName);

        // Then
        assertThat(identity).isEqualTo(expectedIdentity);
    }

    @ParameterizedTest
    @CsvSource({"shortName, Tag", "qualifiedName, java.lang.Deprecated"})
    void resolveOrderPreservationGroupKey_matchingCanonicalNameWithUnavailableSupertype_preservesUnknownIdentity(
            @NonNull String fieldName, @NonNull String writtenTypeName) {
        // Given
        SpoonAstModel parsedModel = parseAstModelFromJavaFixtureResource(INHERITED_NAME_SHADOWING);
        CtCompilationUnit parsedCompilationUnit = parsedModel.getCompilationUnit();
        CtType<?> annotationOwner = parsedCompilationUnit.getDeclaredTypes().getFirst();
        CtAnnotation<?> annotationUse =
                annotationOwner.getField(fieldName).getAnnotations().getFirst();
        AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(parsedCompilationUnit);

        // When
        String identity = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                annotationUse.getPosition().getSourceStart(), writtenTypeName);

        // Then
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType())).isEqualTo(writtenTypeName);
        assertThat(identity).isEqualTo("?." + writtenTypeName);
    }

    private static void assertSourceAnnotationBinding(
            CtCompilationUnit parsedCompilationUnit, CtAnnotation<?> annotationUse, String expectedSourceTypeName) {
        assertThat(SpoonTypeUtils.getAllTypes(parsedCompilationUnit))
                .as("The unmodified Spoon reference points to a declaration in the parsed source file")
                .contains(annotationUse.getAnnotationType().getDeclaration());
        assertThat(formatCanonicalTypeName(annotationUse.getAnnotationType())).isEqualTo(expectedSourceTypeName);
    }

    @NonNull
    private static CtAnnotation<?> requireAnnotationOnDescendantField(CtType<?> ownerType, String fieldName) {
        return ownerType.getElements(new TypeFilter<CtAnnotation<?>>(CtAnnotation.class)).stream()
                .filter(annotationUse -> annotationUse.getParent() instanceof CtField<?> annotationOwner
                        && fieldName.equals(annotationOwner.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No annotation on field " + fieldName));
    }

    @NonNull
    private static CtAnnotation<?> requireFieldAnnotation(
            CtCompilationUnit parsedCompilationUnit, String ownerTypeName, String fieldName) {
        return parsedCompilationUnit.getDeclaredTypes().stream()
                .filter(srcType -> ownerTypeName.equals(srcType.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No source type named " + ownerTypeName))
                .getField(fieldName)
                .getAnnotations()
                .getFirst();
    }

    @NonNull
    private static CtAnnotation<?> requirePackageAnnotationAtSrcOffset(
            CtCompilationUnit parsedCompilationUnit, int annotationSrcStartOffset) {
        return parsedCompilationUnit.getDeclaredPackage().getAnnotations().stream()
                .filter(annotationUse -> annotationUse.getPosition().getSourceStart() == annotationSrcStartOffset)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No package annotation at source offset " + annotationSrcStartOffset));
    }

    @NonNull
    private static Stream<Arguments> wildcardHiddenAncestorCases() {
        return Stream.of(
                Arguments.of(
                        STATIC_WILDCARD_UNAVAILABLE_ANNOTATION_BINDING,
                        "annotationstaticwildcard.StaticWildcardAnnotationBase.Tag",
                        "annotationstaticwildcard.StaticWildcardAnnotationBase.Tags"),
                Arguments.of(
                        ORDINARY_WILDCARD_UNAVAILABLE_ANNOTATION_BINDING,
                        "annotationordinarywildcard.WildcardAnnotationBase.Tag",
                        "annotationordinarywildcard.WildcardAnnotationBase.Tags"));
    }

    @Nested
    class RepeatableMetadataTests {

        @NonNull
        private static final URL FIXTURE = requireClasspathResourceUrl(
                "/test-cases/core/e2e/reorder/39-repeatable-annotation-order/input/RepeatableAnnotationRuntimeOrder.java");

        @NonNull
        private CtCompilationUnit compilationUnit;

        @NonNull
        private CtFieldRead<?> containerClassLiteral;

        @NonNull
        private CtType<?> fixtureType;

        @NonNull
        private CtAnnotation<?> repeatableAnnotation;

        @NonNull
        private CtType<?> repeatedAnnotationType;

        @BeforeEach
        void parseFixture() {
            compilationUnit = parseAstModelFromJavaFixtureResource(FIXTURE).getCompilationUnit();
            fixtureType = compilationUnit.getDeclaredTypes().getFirst();
            repeatedAnnotationType = fixtureType.getNestedType("Tag");
            repeatableAnnotation = repeatedAnnotationType.getAnnotations().stream()
                    .filter(metaAnnotation -> Repeatable.class
                            .getName()
                            .equals(metaAnnotation.getAnnotationType().getQualifiedName()))
                    .findFirst()
                    .orElseThrow();
            containerClassLiteral =
                    (CtFieldRead<?>) repeatableAnnotation.getValues().get("value");
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "missingValue",
                    "literalValue",
                    "nonClassField",
                    "bareTypeAccess",
                    "missingOwner",
                    "missingTarget",
                    "missingAccessedType",
                    "missingBothTypes",
                    "inconsistentTarget",
                    "selfContainer",
                    "containerCycle"
                })
        void construct_malformedRepeatableMetadata_rejectsModel(@NonNull String malformedShape) {
            // Given
            mutateRepeatableMetadata(malformedShape);

            // When / Then
            assertThatThrownBy(() -> new AnnotationDependencyKeyResolver(compilationUnit))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageStartingWith("Invalid @Repeatable container");
        }

        @Test
        void resolveOrderPreservationGroupKey_parsedRepeatableMetadata_sharesContainerKey() {
            // Given
            AnnotationDependencyKeyResolver resolver = new AnnotationDependencyKeyResolver(compilationUnit);
            List<CtAnnotation<?>> explicitContainerAnnotations =
                    fixtureType.getField("explicitContainerFirst").getAnnotations();
            CtAnnotation<?> containerAnnotation = explicitContainerAnnotations.getFirst();
            CtAnnotation<?> repeatedAnnotation = explicitContainerAnnotations.get(1);
            CtAnnotation<?> qualifiedAnnotation = fixtureType
                    .getField("qualifiedOccurrences")
                    .getAnnotations()
                    .getFirst();

            // When
            String containerKey = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                    containerAnnotation.getPosition().getSourceStart(), "Tags");
            String repeatedKey = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                    repeatedAnnotation.getPosition().getSourceStart(), "Tag");
            String qualifiedKey = resolver.resolveAnnotationTypeNameOrRootContainerTypeName(
                    qualifiedAnnotation.getPosition().getSourceStart(), "RepeatableAnnotationRuntimeOrder.Tag");

            // Then
            assertThat(containerKey).isEqualTo("RepeatableAnnotationRuntimeOrder.Tags");
            assertThat(repeatedKey).isEqualTo(containerKey);
            assertThat(qualifiedKey).isEqualTo(containerKey);
        }

        private void addContainerCycle() {
            CtAnnotation<?> cyclicRepeatableAnnotation = repeatableAnnotation.clone();
            cyclicRepeatableAnnotation.setValues(Map.of(
                    "value", fixtureType.getFactory().Code().createClassAccess(repeatedAnnotationType.getReference())));
            fixtureType.getNestedType("Tags").addAnnotation(cyclicRepeatableAnnotation);
        }

        private void mutateRepeatableMetadata(String malformedShape) {
            switch (malformedShape) {
                case "missingValue" -> repeatableAnnotation.setValues(Map.of());
                case "literalValue" ->
                    repeatableAnnotation.setValues(
                            Map.of("value", fixtureType.getFactory().Code().createLiteral("Tags")));
                case "nonClassField" -> containerClassLiteral.getVariable().setSimpleName("VALUE");
                case "bareTypeAccess" ->
                    repeatableAnnotation.setValues(Map.of("value", containerClassLiteral.getTarget()));
                case "missingOwner" -> containerClassLiteral.getVariable().setDeclaringType(null);
                case "missingTarget" -> containerClassLiteral.setTarget(null);
                case "missingAccessedType" ->
                    ((CtTypeAccess<?>) containerClassLiteral.getTarget()).setAccessedType(null);
                case "missingBothTypes" -> {
                    containerClassLiteral.getVariable().setDeclaringType(null);
                    ((CtTypeAccess<?>) containerClassLiteral.getTarget()).setAccessedType(null);
                }
                case "inconsistentTarget" ->
                    containerClassLiteral.setTarget(
                            fixtureType.getFactory().Code().createTypeAccess(repeatedAnnotationType.getReference()));
                case "selfContainer" ->
                    repeatableAnnotation.setValues(Map.of(
                            "value",
                            fixtureType.getFactory().Code().createClassAccess(repeatedAnnotationType.getReference())));
                case "containerCycle" -> addContainerCycle();
                default -> throw new IllegalArgumentException("Unknown malformed metadata shape: " + malformedShape);
            }
        }
    }
}
