// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.JAVA_LANG_PACKAGE_SEGMENT_COUNT;
import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.JAVA_LANG_TYPE_PREFIX;
import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.findJavaLangTopLevelType;
import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.findPublicMemberTypeInHierarchy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

/** Resolves annotation type names and tracks whether their canonical identities are confirmed. */
final class AnnotationTypeNameResolver {

    @NonNull
    private final AnnotationTypeNameFromDeclarationsResolver annotationTypeNameFromDeclarationsResolver;

    @NonNull
    private final Map<Integer, CtTypeReference<?>> annotationTypeReferencesBySrcStartOffset;

    @NonNull
    private final CtCompilationUnit compilationUnit;

    /** Definite single-type imports from the prepared source-wide context. */
    @NonNull
    private final Map<String, CtImport> singleTypeImportsBySimpleName;

    /** Static-member lookup blockers from the prepared source-wide context. */
    @NonNull
    private final Set<String> unresolvableStaticMemberSimpleNames;

    /** Only the presence of wildcard imports is needed here; their owners belong to declaration lookup. */
    private final boolean wildcardImportsPresent;

    /**
     * Prepares declaration and annotation-use lookup for one source file.
     * @param compilationUnit original Spoon model of the source file
     * @param typeImportContext prepared import facts from the same original source-file model
     */
    AnnotationTypeNameResolver(
            @NonNull CtCompilationUnit compilationUnit, @NonNull SpoonTypeImportContext typeImportContext) {
        this.compilationUnit = compilationUnit;
        singleTypeImportsBySimpleName = typeImportContext.getSingleTypeImportsBySimpleName();
        unresolvableStaticMemberSimpleNames = typeImportContext.getUnresolvableStaticMemberSimpleNames();
        wildcardImportsPresent = typeImportContext.hasWildcardImports();
        annotationTypeNameFromDeclarationsResolver =
                new AnnotationTypeNameFromDeclarationsResolver(compilationUnit, typeImportContext);
        annotationTypeReferencesBySrcStartOffset = collectAnnotationTypeReferencesBySrcStartOffset(compilationUnit);
    }

    /**
     * Checks enclosing types, this file's top-level type trees, single imports, and Spoon's source binding, in order.
     * A confirmed result ends lookup. A blocked result returns the written name as unconfirmed. If every lookup
     * misses, the fallback uses file context and can confirm an actual runtime annotation for a module descriptor.
     * @param annotationUseSrcStartOffset source offset of the annotation's opening {@code @}
     * @param writtenAnnotationTypeName tokenized type name retaining the written qualification
     * @return the selected name and its canonical-identity confirmation
     * @throws IllegalStateException if an available selected declaration is not an annotation type
     */
    @NonNull
    AnnotationTypeNameResolution resolveAnnotationTypeName(
            int annotationUseSrcStartOffset, @NonNull String writtenAnnotationTypeName) {
        // 1. Search enclosing types from inner to outer, including inheritance: @Tag inside Derived.
        CanonicalAnnotationTypeNameLookupResult enclosingTypeLookup =
                annotationTypeNameFromDeclarationsResolver.resolveCanonicalAnnotationTypeNameInEnclosingTypes(
                        annotationUseSrcStartOffset, writtenAnnotationTypeName);
        if (enclosingTypeLookup.isTerminal()) {
            return createTerminalAnnotationTypeNameResolution(enclosingTypeLookup, writtenAnnotationTypeName);
        }

        // 2. Search this file's top-level type trees: @Owner.Inner.Tag.
        CanonicalAnnotationTypeNameLookupResult srcTypeTreeLookup =
                annotationTypeNameFromDeclarationsResolver.resolveCanonicalAnnotationTypeNameInSrcRootTypeTrees(
                        writtenAnnotationTypeName);
        if (srcTypeTreeLookup.isTerminal()) {
            return createTerminalAnnotationTypeNameResolution(srcTypeTreeLookup, writtenAnnotationTypeName);
        }

        // 3. Check explicit imports: import library.Tag; @Tag.
        CanonicalAnnotationTypeNameLookupResult importLookup =
                resolveCanonicalAnnotationTypeNameFromImports(writtenAnnotationTypeName);
        if (importLookup.isTerminal()) {
            return createTerminalAnnotationTypeNameResolution(importLookup, writtenAnnotationTypeName);
        }

        // 4. Validate a source AST binding against the path and imports: @demo.Derived.Tag -> demo.Base.Tag.
        // The source scanner also supplies uses that Spoon's AST traversal can omit, such as String @Tag [] values.
        // TODO Don't you think this stage is a part of the
        CtTypeReference<?> annotationUseTypeReference =
                annotationTypeReferencesBySrcStartOffset.get(annotationUseSrcStartOffset);
        CanonicalAnnotationTypeNameLookupResult srcBindingLookup =
                annotationTypeNameFromDeclarationsResolver.confirmSrcBoundAnnotationTypeName(
                        annotationUseTypeReference, writtenAnnotationTypeName);
        if (srcBindingLookup.isTerminal()) {
            return createTerminalAnnotationTypeNameResolution(srcBindingLookup, writtenAnnotationTypeName);
        }

        // 5. Select a fallback name, retaining the unconfirmed written spelling by default.
        return selectFallbackAnnotationTypeName(annotationUseTypeReference, writtenAnnotationTypeName);
    }

    /**
     * Indexes annotation-use references by Spoon-reported start offset, within the original source-file model.
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<Integer, CtTypeReference<?>> collectAnnotationTypeReferencesBySrcStartOffset(
            CtCompilationUnit compilationUnit) {
        Map<Integer, CtTypeReference<?>> annotationTypeReferencesBySrcStartOffset = new HashMap<>();
        CtScanner annotationScanner = new CtScanner() {
            @Override
            @SuppressWarnings("PMD.CompareObjectsWithEquals")
            protected void enter(@NonNull CtElement element) {
                if (element instanceof CtAnnotation<?> annotation) {
                    SourcePosition position = annotation.getPosition();
                    // A shared CtPackage can expose annotations from another file at the same offset.
                    if (position.isValidPosition() && position.getCompilationUnit() == compilationUnit) {
                        annotationTypeReferencesBySrcStartOffset.put(
                                position.getSourceStart(), annotation.getAnnotationType());
                    }
                }
            }
        };
        annotationScanner.scan(compilationUnit.getDeclaredTypes());
        annotationScanner.scan(compilationUnit.getPackageDeclaration());
        CtPackage declaredPackage = compilationUnit.getDeclaredPackage();
        if (declaredPackage != null) {
            annotationScanner.scan(declaredPackage.getAnnotations());
        }
        CtModule declaredModule = compilationUnit.getDeclaredModule();
        if (declaredModule != null) {
            annotationScanner.scan(declaredModule.getAnnotations());
        }
        return Collections.unmodifiableMap(annotationTypeReferencesBySrcStartOffset);
    }

    @NonNull
    private static AnnotationTypeNameResolution createTerminalAnnotationTypeNameResolution(
            CanonicalAnnotationTypeNameLookupResult canonicalTypeNameLookupResult, String writtenAnnotationTypeName) {
        return canonicalTypeNameLookupResult.getCanonicalAnnotationTypeName() == null
                ? new AnnotationTypeNameResolution(writtenAnnotationTypeName)
                : new AnnotationTypeNameResolution(canonicalTypeNameLookupResult);
    }

    /** Follows a module annotation name through the runtime JDK's public {@code java.lang} types. */
    @NonNull
    private static AnnotationTypeNameResolution resolveModuleAnnotationTypeName(String writtenAnnotationTypeName) {
        String[] typeNameSegments = writtenAnnotationTypeName.split("\\.");
        // A type in the implicit import takes precedence over a package with the same name.
        Optional<Class<?>> javaLangRootType = findJavaLangTopLevelType(typeNameSegments[0]);
        if (javaLangRootType.isEmpty() && writtenAnnotationTypeName.startsWith(JAVA_LANG_TYPE_PREFIX)) {
            typeNameSegments =
                    Arrays.copyOfRange(typeNameSegments, JAVA_LANG_PACKAGE_SEGMENT_COUNT, typeNameSegments.length);
            javaLangRootType = findJavaLangTopLevelType(typeNameSegments[0]);
        }
        if (javaLangRootType.isEmpty()) {
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        Class<?> currentRuntimeType = javaLangRootType.orElseThrow();
        for (int segmentIndex = 1; segmentIndex < typeNameSegments.length; segmentIndex++) {
            Optional<Class<?>> memberType =
                    findPublicMemberTypeInHierarchy(currentRuntimeType, typeNameSegments[segmentIndex]);
            if (memberType.isEmpty()) {
                return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
            }
            currentRuntimeType = memberType.orElseThrow();
        }
        if (!currentRuntimeType.isAnnotation()) {
            throw new IllegalStateException(
                    "Expected an annotation declaration, but resolved runtime type " + currentRuntimeType.getName());
        }
        return new AnnotationTypeNameResolution(currentRuntimeType.getCanonicalName(), true);
    }

    @NonNull
    private CanonicalAnnotationTypeNameLookupResult resolveCanonicalAnnotationTypeNameFromImports(
            String writtenAnnotationTypeName) {
        int firstDotIndex = writtenAnnotationTypeName.indexOf('.');
        String writtenFirstSegment =
                firstDotIndex < 0 ? writtenAnnotationTypeName : writtenAnnotationTypeName.substring(0, firstDotIndex);
        Optional<CtImport> matchingTypeImport =
                Optional.ofNullable(singleTypeImportsBySimpleName.get(writtenFirstSegment));
        if (matchingTypeImport.isPresent()) {
            // An imported owner in @Derived.Tag can inherit Tag from Base, so the import confirms only Derived.
            return firstDotIndex < 0
                    ? new CanonicalAnnotationTypeNameLookupResult(matchingTypeImport.orElseThrow())
                    : CanonicalAnnotationTypeNameLookupResult.UNRESOLVED;
        }
        return unresolvableStaticMemberSimpleNames.contains(writtenFirstSegment)
                ? CanonicalAnnotationTypeNameLookupResult.UNRESOLVED
                : CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
    }

    @NonNull
    private AnnotationTypeNameResolution selectFallbackAnnotationTypeName(
            @Nullable CtTypeReference<?> annotationUseTypeReference, String writtenAnnotationTypeName) {
        if (wildcardImportsPresent) {
            // A wildcard can introduce the annotation or the first owner in its written path.
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        if (compilationUnit.getUnitType() == CtCompilationUnit.UNIT_TYPE.MODULE_DECLARATION) {
            // A module descriptor has no current package that could shadow the implicit JDK import.
            return resolveModuleAnnotationTypeName(writtenAnnotationTypeName);
        }
        if (writtenAnnotationTypeName.indexOf('.') >= 0) {
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        if (annotationUseTypeReference == null) {
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        CtPackage declaredPackage = compilationUnit.getDeclaredPackage();
        String packageName =
                declaredPackage == null || declaredPackage.isUnnamedPackage() ? "" : declaredPackage.getQualifiedName();
        String annotationPackageName = annotationUseTypeReference.getPackage() == null
                ? ""
                : annotationUseTypeReference.getPackage().getQualifiedName();
        // Package agreement supplies a contextual spelling. An unseen declaration can still change the binding.
        return new AnnotationTypeNameResolution(
                packageName.equals(annotationPackageName)
                        ? formatCanonicalTypeName(annotationUseTypeReference)
                        : writtenAnnotationTypeName);
    }
}
