// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
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

/**
 * Determines annotation type names through the complete sequence of declaration, import, AST, and context checks.
 * Delegates declaration traversal to {@link AnnotationTypeNameFromDeclarationsResolver}; that helper is one lookup
 * mechanism within this algorithm. Single imports can establish a name without an available declaration;
 * the final fallback selects a written/contextual name and can confirm the implicit module namespace.
 * This class owns their precedence and the annotation-use AST index shared by stages 4 and 5.
 */
final class AnnotationTypeNameResolver {

    @NonNull
    private final AnnotationTypeNameFromDeclarationsResolver annotationTypeNameFromDeclarationsResolver;

    /**
     * Original annotation-use references indexed by the source start offsets reported by Spoon.
     * These are preliminary bindings: unavailable dependencies can make Spoon select an unrelated type.
     */
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
     * Indexes annotation uses and prepares declaration-based lookup for the complete type-name algorithm.
     * The import context must belong to the supplied original source-file model; source scopes are derived from it.
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
     * Resolves an annotation type name using declarations, imports, the original AST binding, and package context.
     * Stages 1–4 confirm names through declarations/imports; the final stage can also confirm an implicit module name.
     * Other outcomes retain an unconfirmed written/contextual name for conservative grouping.
     * A blocking UNRESOLVED outcome immediately returns the written name without trying later stages.
     * @param annotationUseSrcStartOffset source offset of the annotation's opening {@code @}
     * @param writtenAnnotationTypeName tokenized type name retaining the written qualification
     * @return non-null name and canonical-identity confirmation; uncertainty retains an unconfirmed fallback name
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
        // Earlier stages need no annotation-use binding.
        // The separate Eclipse JDT scanner supplies this source offset independently of Spoon's AST.
        // Three omission categories are reproduced with Spoon 11.5.0 on JDK 21:
        //   module annotations: @java.lang.Deprecated module example {}
        //   array-dimension annotations: String @Tag [] values;
        //   receiver-parameter annotations: void execute(@Tag Example this) {} // Tag targets TYPE_USE.
        // AST annotation traversal misses these uses, so their scanner offsets have no indexed reference.
        // Knowing an offset or repeating that traversal cannot recover the missing use binding.
        // These are the known cases, not a guarantee that every other Java annotation has an AST entry.
        CtTypeReference<?> annotationUseTypeReference =
                annotationTypeReferencesBySrcStartOffset.get(annotationUseSrcStartOffset);
        CanonicalAnnotationTypeNameLookupResult srcBindingLookup =
                annotationTypeNameFromDeclarationsResolver.confirmSrcBoundAnnotationTypeName(
                        annotationUseTypeReference, writtenAnnotationTypeName);
        if (srcBindingLookup.isTerminal()) {
            return createTerminalAnnotationTypeNameResolution(srcBindingLookup, writtenAnnotationTypeName);
        }

        /*
         * Stages 1–4 all returned NOT_FOUND; UNRESOLVED would already have stopped lookup.
         * Stage 4 left one of two states:
         * 1. The use reference exists, but getDeclaration() returned null: no source declaration was available.
         *    This is not limited to java.lang: import lombok.*; @EqualsAndHashCode.Exclude also reaches this point
         *    with reference name lombok.EqualsAndHashCode.Exclude and no declaration. In no-classpath mode,
         *    references can represent unavailable library types; their presence does not prove type resolution.
         *    If it had returned a declaration, stage 4 would have finished with success or UNRESOLVED.
         * 2. The use reference itself is absent, so getDeclaration() was never called.
         *    This does not prove that the file has no annotation declaration: a declaration can exist in the AST
         *    even when Spoon omits its use, as in String @annotationmultihop.MultiHopAnnotationBase.Tag [] values;
         *    with Tag declared inside MultiHopAnnotationBase in this file. Stages 1–2 do not strip the package
         *    prefix, stage 3 has no matching single import, and stage 4 has no use reference to follow.
         * Stage 4 validates the specific use's binding; it does not independently search every declaration.
         *
         * Remaining qualified spellings include names from any package (@java.lang.Deprecated) and relative
         * owners supplied by wildcard imports (import library.*; @Owner.Tag). Stage 3 checks single imports only.
         * Remaining simple spellings can refer to another file in the current package (named or unnamed),
         * implicit java.lang types, or wildcard-imported types. The final stage accepts only supported context evidence;
         * wildcard ambiguity or an ordinary file's unconfirmed java.lang binding retains the original written name.
         */

        /*
         * 5. Select a fallback name, retaining the unconfirmed written spelling by default.
         * Without wildcard imports, modules resolve actual runtime java.lang types and their nested members:
         * @Deprecated and @java.lang.Deprecated both confirm java.lang.Deprecated.
         * A relative @Owner.Tag needs a real java.lang.Owner; its spelling alone does not identify that package.
         * Ordinary files retain dotted names and may provisionally qualify simple names from same-package AST refs.
         * Only a successful runtime module lookup establishes canonical identity here; other names remain lexical.
         */
        return selectFallbackAnnotationTypeName(annotationUseTypeReference, writtenAnnotationTypeName);
    }

    /**
     * Indexes AST-visible annotation uses with valid source positions in this source file.
     * For {@code @Deprecated @interface Tag {}}, records the Deprecated type reference, not the Tag declaration.
     *
     * @param compilationUnit original Spoon model of the source file to inspect
     * @return unmodifiable type references keyed by the source start offset reported for each annotation use
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<Integer, CtTypeReference<?>> collectAnnotationTypeReferencesBySrcStartOffset(
            CtCompilationUnit compilationUnit) {
        Map<Integer, CtTypeReference<?>> annotationTypeReferencesBySrcStartOffset = new HashMap<>();
        CtScanner annotationScanner = new CtScanner() {
            /**
             * Captures annotations belonging to the requested source-file model.
             * @param element original source element being visited
             */
            @Override
            @SuppressWarnings("PMD.CompareObjectsWithEquals")
            protected void enter(@NonNull CtElement element) {
                if (element instanceof CtAnnotation<?> annotation) {
                    SourcePosition position = annotation.getPosition();
                    // CtPackage is shared across files; its annotations can belong to package-info.java.
                    // Match the original CtCompilationUnit instance: offsets from other files can collide.
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
        // UNRESOLVED stops the stage sequence; retaining the written name does not resume lookup or confirm identity.
        return canonicalTypeNameLookupResult.getCanonicalAnnotationTypeName() == null
                ? new AnnotationTypeNameResolution(writtenAnnotationTypeName)
                : new AnnotationTypeNameResolution(canonicalTypeNameLookupResult);
    }

    /** Confirms a module annotation only after resolving its actual runtime java.lang declaration. */
    @NonNull
    private static AnnotationTypeNameResolution resolveModuleAnnotationTypeName(String writtenAnnotationTypeName) {
        String[] typeNameSegments = writtenAnnotationTypeName.split("\\.");
        // Try the first segment as an implicitly imported type; a matching type takes precedence over a package.
        Optional<Class<?>> javaLangRootType = findJavaLangTopLevelType(typeNameSegments[0]);
        if (javaLangRootType.isEmpty() && writtenAnnotationTypeName.startsWith(JAVA_LANG_TYPE_PREFIX)) {
            // For @java.lang.Deprecated, the first lookup found no top-level type named "java".
            // Strip the explicitly written package prefix "java.lang." and retry with the type name "Deprecated".
            typeNameSegments =
                    Arrays.copyOfRange(typeNameSegments, JAVA_LANG_PACKAGE_SEGMENT_COUNT, typeNameSegments.length);
            javaLangRootType = findJavaLangTopLevelType(typeNameSegments[0]);
        }
        if (javaLangRootType.isEmpty()) {
            // @Owner.Tag may name package Owner; absence from java.lang never justifies adding that prefix.
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        Class<?> currentRuntimeType = javaLangRootType.orElseThrow();
        for (int segmentIndex = 1; segmentIndex < typeNameSegments.length; segmentIndex++) {
            Optional<Class<?>> memberType =
                    findPublicMemberTypeInHierarchy(currentRuntimeType, typeNameSegments[segmentIndex]);
            if (memberType.isEmpty()) {
                // Once a visible type owns the path, do not restart it as a package name.
                return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
            }
            currentRuntimeType = memberType.orElseThrow();
        }
        if (!currentRuntimeType.isAnnotation()) {
            throw new IllegalStateException(
                    "Expected an annotation declaration, but resolved runtime type " + currentRuntimeType.getName());
        }
        // Derived.Tag can denote Base.Tag: canonical identity comes from the selected declaration, not concatenation.
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
            // A missing import library.Deprecated still proves identity; Spoon may invent java.lang's.
            // Validate only the selected annotation import; other imported owners can be ordinary classes.
            return firstDotIndex < 0
                    ? new CanonicalAnnotationTypeNameLookupResult(matchingTypeImport.orElseThrow())
                    // E.g. import library.Derived; matches only Derived in @Derived.Tag.
                    // Derived may inherit Base.Tag, whose canonical name is library.Base.Tag.
                    // Appending ".Tag" to the import would incorrectly claim library.Derived.Tag.
                    // Stage 3 does not inspect imported owners or their parents to find Tag.
                    // Stop here so later fallbacks cannot guess a different annotation identity.
                    : CanonicalAnnotationTypeNameLookupResult.UNRESOLVED;
        }
        // A definite type import wins regardless of its order relative to a static-member blocker.
        // A field/method reference can omit a co-imported inherited type, so it also blocks a matching name.
        // Only the first segment can name that member: Tag blocks @Tag and @Tag.Nested, but not @library.Tag.
        // Matching several owners still means UNRESOLVED, never a chosen identity.
        return unresolvableStaticMemberSimpleNames.contains(writtenFirstSegment)
                ? CanonicalAnnotationTypeNameLookupResult.UNRESOLVED
                : CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
    }

    /**
     * Selects a fallback name after stages 1–4 returned NOT_FOUND.
     * Modules can confirm a runtime java.lang annotation through its actual nested/inherited declarations.
     * Ordinary files retain written qualification or provisionally qualify simple names from their AST references.
     * @param annotationUseTypeReference optional annotation-use AST binding
     * @param writtenAnnotationTypeName annotation type name retaining its written qualification
     * @return name and canonical-identity confirmation, retaining an unconfirmed spelling when evidence is insufficient
     */
    @NonNull
    private AnnotationTypeNameResolution selectFallbackAnnotationTypeName(
            @Nullable CtTypeReference<?> annotationUseTypeReference, String writtenAnnotationTypeName) {
        if (wildcardImportsPresent) {
            // @Tag and @Owner.Tag can come from an unavailable imported owner instead of java.lang.
            return new AnnotationTypeNameResolution(writtenAnnotationTypeName);
        }
        if (compilationUnit.getUnitType() == CtCompilationUnit.UNIT_TYPE.MODULE_DECLARATION) {
            // A module descriptor has no current package. Stage 3 handled matching single imports, and
            // wildcards were excluded above. Resolve actual JDK declarations even when the use has no AST entry:
            // Spoon 11.5.0 omits module annotations. Review its parser workarounds when upgrading Spoon.
            return resolveModuleAnnotationTypeName(writtenAnnotationTypeName);
        }
        if (writtenAnnotationTypeName.indexOf('.') >= 0) {
            // An unseen same-package Thread or java can obscure java.lang in @Thread.Tag or @java.lang.Thread.Tag.
            // Module lookup therefore does not apply to ordinary files; qualified spelling stays unconfirmed.
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
        // Only provisional qualification remains here: an ordinary file, a simple name, and no wildcard imports.
        // A reference in the current package can supply its contextual spelling: @Tag with AST demo.Tag -> demo.Tag.
        // Another file can declare demo.Deprecated and hide java.lang.Deprecated. In the compilable
        // implicit-java-lang-shadowing fixture, Spoon 11.5.0 nevertheless binds @Deprecated("custom") to
        // java.lang.Deprecated when parsing only the consumer, without seeing the custom annotation declaration.
        return new AnnotationTypeNameResolution(
                packageName.equals(annotationPackageName)
                        ? formatCanonicalTypeName(annotationUseTypeReference)
                        // Keep the written name instead of the external guess; both branches remain unconfirmed.
                        : writtenAnnotationTypeName);
    }
}
