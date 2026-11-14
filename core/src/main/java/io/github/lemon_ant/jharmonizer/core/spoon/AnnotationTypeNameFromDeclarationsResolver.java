// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeInheritanceUtils.findInheritedMemberTypeDeclaration;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.startsWithTypeName;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeInheritanceUtils.InheritedMemberTypeLookupResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Derives annotation type names by traversing available type declarations, including nested and inherited members.
 * Enclosing types, top-level roots, and owners selected through AST bindings or wildcard imports supply starting points
 * for the same traversal. These entry points serve stages 1, 2, and 4 of {@link AnnotationTypeNameResolver}.
 * Searches declarations in the current file; import spelling and qualified/contextual name inference remain in the caller.
 * Binding confirmation runs after single imports, which can shadow package names.
 */
final class AnnotationTypeNameFromDeclarationsResolver {

    @NonNull
    private final CtCompilationUnit compilationUnit;

    /** Available source owners indexed by their original imports; an absent entry denotes an unavailable owner. */
    @NonNull
    private final Map<CtImport, CtType<?>> srcOwnerTypesByWildcardImport;

    /** All type visibility scopes, including local and anonymous types omitted from canonical declaration indexes. */
    @NonNull
    private final List<CtType<?>> srcTypeScopes;

    /** All wildcard imports in the order supplied by Spoon. */
    @NonNull
    private final List<CtImport> wildcardImportsInModelOrder;

    /**
     * Prepares source type scopes and retains the required wildcard-import facts from the shared context.
     * @param compilationUnit original Spoon model of the source file
     * @param typeImportContext prepared import facts from the same original source-file model
     */
    AnnotationTypeNameFromDeclarationsResolver(
            @NonNull CtCompilationUnit compilationUnit, @NonNull SpoonTypeImportContext typeImportContext) {
        this.compilationUnit = compilationUnit;
        srcTypeScopes = SpoonTypeUtils.collectSrcTypeScopes(compilationUnit);
        wildcardImportsInModelOrder = typeImportContext.getWildcardImportsInModelOrder();
        srcOwnerTypesByWildcardImport = typeImportContext.getSrcOwnerTypesByWildcardImport();
    }

    /**
     * Stage 4: confirms Spoon's source binding against the written name and wildcard imports.
     * Spoon 11.5.0 can bind {@code @p.Derived.Tag} to source {@code Base.Tag} even when an unavailable
     * wildcard import introduces type {@code p}, changing the annotation's identity.
     * Reproduced by the compilable {@code wildcard-package-name-shadowing} fixture, without AST changes.
     * @param annotationUseTypeReference original annotation-use binding, if Spoon reported one
     * @param writtenAnnotationTypeName tokenized type name retaining the written qualification
     * @return confirmed canonical annotation name, {@code UNRESOLVED} for a rejected source binding,
     *         or {@code NOT_FOUND} when no binding to this file's declarations exists
     * @throws IllegalStateException if a selected source declaration is not an annotation type
     */
    @NonNull
    CanonicalAnnotationTypeNameLookupResult confirmSrcBoundAnnotationTypeName(
            @Nullable CtTypeReference<?> annotationUseTypeReference, @NonNull String writtenAnnotationTypeName) {
        if (annotationUseTypeReference == null) {
            // Missing use references are reproduced for module, array-dimension, and receiver annotations
            // in Spoon 11.5.0. The separate source scanner still supplies their names and offsets.
            return CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
        }
        // getDeclaration() searches only sources, and our fresh parser model contains one source file.
        // Classpath types such as java.lang.Deprecated return null here, unlike getTypeDeclaration().
        CtType<?> boundTypeDeclaration = annotationUseTypeReference.getDeclaration();
        if (boundTypeDeclaration == null) {
            // No source declaration to confirm; later stages may still use the reference's name.
            return CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
        }
        if (hasMatchingSrcDeclaration(formatCanonicalTypeName(annotationUseTypeReference), writtenAnnotationTypeName)) {
            return new CanonicalAnnotationTypeNameLookupResult(boundTypeDeclaration);
        }
        return CanonicalAnnotationTypeNameLookupResult.UNRESOLVED;
    }

    /**
     * Stage 1: searches enclosing source types from inner to outer, following nested and inherited members.
     * @param annotationUseSrcStartOffset source offset of the annotation's opening {@code @}
     * @param writtenAnnotationTypeName tokenized type name retaining the written qualification
     * @return canonical annotation name, a blocking unresolved result, or no match in the enclosing types
     * @throws IllegalStateException if a selected source declaration is not an annotation type
     */
    @NonNull
    CanonicalAnnotationTypeNameLookupResult resolveCanonicalAnnotationTypeNameInEnclosingTypes(
            int annotationUseSrcStartOffset, @NonNull String writtenAnnotationTypeName) {
        List<CtType<?>> enclosingTypesInInnerToOuterOrder =
                SpoonTypeUtils.collectEnclosingSrcTypes(srcTypeScopes, annotationUseSrcStartOffset);
        // Any annotation use inside an indexed type's valid Spoon-reported range enters this loop, including
        // ordinary field, method, parameter and type-use annotations; nested types are not required.
        // class Example { @Tag int value; } gives [Example]; inside Outer.Inner the list is [Outer.Inner, Outer].
        // Local/anonymous scopes must precede outer types: their unavailable parents can hide a top-level @Tag.
        // Annotation uses on packages/modules normally give an empty list. Uses on a type qualify only when
        // their '@' offsets are included in its reported range; earlier annotations can precede Spoon's start.
        for (CtType<?> enclosingType : enclosingTypesInInnerToOuterOrder) {
            // In Outer.Inner extends lib.Base, an unavailable Base.Tag can shadow Outer.Tag.
            // Resolve the written path in this scope before moving outward or accepting a later AST binding.
            CanonicalAnnotationTypeNameLookupResult enclosingTypeLookup =
                    resolveCanonicalAnnotationTypeNameInSrcTypeScope(enclosingType, writtenAnnotationTypeName);
            if (enclosingTypeLookup.isTerminal()) {
                return enclosingTypeLookup;
            }
        }
        return CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
    }

    /**
     * Stage 2: matches the first written segment to a top-level type, then follows the remaining member-type path.
     * @param writtenAnnotationTypeName tokenized type name retaining the written qualification
     * @return canonical annotation name, a blocking unresolved result, or no matching top-level type
     * @throws IllegalStateException if a selected source declaration is not an annotation type
     */
    @NonNull
    CanonicalAnnotationTypeNameLookupResult resolveCanonicalAnnotationTypeNameInSrcRootTypeTrees(
            @NonNull String writtenAnnotationTypeName) {
        // Resolve the whole written name through declared type nesting, starting at a top-level type in the source
        // file.
        // Match a top-level path root: @Tag selects Tag; @Outer.Inner.Tag selects Outer, then follows Inner.Tag.
        // Match the whole first segment, so Outer cannot match OuterOther.
        // Do not strip the current package prefix: package demo; import library.demo; @demo.Owner.Tag
        // selects library.demo.Owner.Tag, even when this file also declares demo.Owner.Tag.
        // NOT_FOUND here does not exclude an imported first segment; stage 3 must check the original spelling.
        // A same-package type from another file can obscure the package prefix even without imports.
        // Stage 4 checks a package-qualified source path as part of AST-binding confirmation, not at this stage.
        Optional<CtType<?>> srcPathRootType = SpoonTypeUtils.getRootTypes(compilationUnit).stream()
                .filter(srcType -> startsWithTypeName(writtenAnnotationTypeName, srcType.getSimpleName()))
                .findFirst();
        return srcPathRootType
                .map(ctType -> resolveCanonicalAnnotationTypeNameInSrcTypeScope(ctType, writtenAnnotationTypeName))
                .orElse(CanonicalAnnotationTypeNameLookupResult.NOT_FOUND);
    }

    /** Checks the written path and every wildcard-import owner before accepting a source binding's canonical name. */
    private boolean hasMatchingSrcDeclaration(String boundTypeName, String writtenAnnotationTypeName) {
        // E.g. @p.Derived.Tag reaches stage 4 because stage 2 matches only simple-name roots.
        // Now establish the source path; this check is name confirmation, not a separate Spoon workaround.
        CanonicalAnnotationTypeNameLookupResult qualifiedLookup =
                resolveCanonicalAnnotationTypeNameInSrcRootTypeTreesWithPackagePrefix(writtenAnnotationTypeName);
        if (qualifiedLookup.isTerminal() && !boundTypeName.equals(qualifiedLookup.getCanonicalAnnotationTypeName())) {
            // A needed unavailable branch has no name; a different declaration cannot confirm this binding.
            return false;
        }
        // Reached for NOT_FOUND (false), or a successful lookup whose canonical name equals boundTypeName (true).
        // The preceding guard rejected UNRESOLVED and name mismatches; wildcard imports still need checking below.
        boolean hasSrcDeclarationMatchingBoundTypeName = qualifiedLookup.isTerminal();
        // Stage 3 handled single imports; a member wildcard can introduce an owner absent from top-level roots:
        // import static p.Owner.*; @Derived.Inner.Tag package p;
        for (CtImport wildcardImport : wildcardImportsInModelOrder) {
            CtType<?> importedOwnerType = srcOwnerTypesByWildcardImport.get(wildcardImport);
            if (importedOwnerType == null) {
                // An unavailable import static lib.Owner.* can introduce type p in @p.Derived.Tag,
                // invalidating a source binding. Stop at that import after checking its preceding owners.
                return false;
            }
            // Java imports the owner's members, not the owner itself: wildcard-imported-member-scope.
            CanonicalAnnotationTypeNameLookupResult importedScopeLookup =
                    resolveAnnotationTypeNameInSrcTypeHierarchy(importedOwnerType, writtenAnnotationTypeName, false);
            if (!importedScopeLookup.isTerminal()) {
                continue;
            }
            if (!boundTypeName.equals(importedScopeLookup.getCanonicalAnnotationTypeName())) {
                return false;
            }
            hasSrcDeclarationMatchingBoundTypeName = true;
        }
        return hasSrcDeclarationMatchingBoundTypeName;
    }

    @NonNull
    private CanonicalAnnotationTypeNameLookupResult resolveAnnotationTypeNameInSrcTypeHierarchy(
            CtType<?> scopeType, String relativeAnnotationTypeName, boolean scopeTypeMatchesWrittenPrefix) {
        CtType<?> currentOwnerType = scopeType;
        // Tracks matched written-name segments, not merely visited/resolved superclasses.
        // The caller may already have matched and removed the initial owner's prefix, e.g. "Owner.".
        boolean hasMatchedPath = scopeTypeMatchesWrittenPrefix;
        for (String nestedMemberTypeName : relativeAnnotationTypeName.split("\\.")) {
            CtType<?> declaredMemberType = currentOwnerType.getNestedType(nestedMemberTypeName);
            if (declaredMemberType == null) {
                InheritedMemberTypeLookupResult inheritedLookup =
                        findInheritedMemberTypeDeclaration(compilationUnit, currentOwnerType, nestedMemberTypeName);
                if (inheritedLookup.isUnresolved()) {
                    // A required parent is unavailable, or several distinct inherited declarations match.
                    // Even a candidate found on one branch cannot establish a unique result; stop lookup here.
                    return CanonicalAnnotationTypeNameLookupResult.UNRESOLVED;
                }
                // A conclusive lookup returns an inherited source type, or null if no member was found.
                declaredMemberType = inheritedLookup.getDeclaration();
                if (declaredMemberType == null) {
                    // This segment is absent from direct nesting and inherited members.
                    // Unavailable inheritance was handled above; this null means absence, not a missing
                    // declaration.
                    return hasMatchedPath
                            // UNRESOLVED: a prefix such as Owner.Inner in Owner.Inner.Tag already selected an
                            // owner.
                            // Do not replace its missing suffix Tag with a match in another enclosing type or
                            // import.
                            ? CanonicalAnnotationTypeNameLookupResult.UNRESOLVED
                            // NOT_FOUND: this starting type did not match even the first segment of the written
                            // name.
                            // Example: class Outer { @interface Tag {} class Inner { @Tag int value; } }
                            // Lookup in Inner finds no Tag, so the caller can try Outer and resolve Outer.Tag.
                            // The annotation exists; this starting type simply contributes no prefix to its name.
                            : CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
                }
            }
            // Continue the written path inside the matched direct or inherited member type.
            currentOwnerType = declaredMemberType;
            hasMatchedPath = true;
        }
        return new CanonicalAnnotationTypeNameLookupResult(currentOwnerType);
    }

    @NonNull
    private CanonicalAnnotationTypeNameLookupResult
            resolveCanonicalAnnotationTypeNameInSrcRootTypeTreesWithPackagePrefix(
                    String writtenPackageQualifiedAnnotationTypeName) {
        CtPackage declaredPackage = compilationUnit.getDeclaredPackage();
        if (declaredPackage == null || declaredPackage.isUnnamedPackage()) {
            return CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
        }
        String packagePrefix = declaredPackage.getQualifiedName() + '.';
        // Only after import checks: an imported type can itself be named like the package prefix.
        return writtenPackageQualifiedAnnotationTypeName.startsWith(packagePrefix)
                ? resolveCanonicalAnnotationTypeNameInSrcRootTypeTrees(
                        writtenPackageQualifiedAnnotationTypeName.substring(packagePrefix.length()))
                : CanonicalAnnotationTypeNameLookupResult.NOT_FOUND;
    }

    /**
     * Resolves the complete annotation name; intermediate owners may be ordinary types.
     * Requires the final declaration to be an annotation before creating a successful result.
     */
    @NonNull
    private CanonicalAnnotationTypeNameLookupResult resolveCanonicalAnnotationTypeNameInSrcTypeScope(
            CtType<?> scopeType, String writtenAnnotationTypeName) {
        if (writtenAnnotationTypeName.equals(scopeType.getSimpleName())) {
            return new CanonicalAnnotationTypeNameLookupResult(scopeType);
        }
        String scopePrefix = scopeType.getSimpleName() + '.';
        boolean hasMatchedPath = writtenAnnotationTypeName.startsWith(scopePrefix);
        String relativeAnnotationTypeName =
                hasMatchedPath ? writtenAnnotationTypeName.substring(scopePrefix.length()) : writtenAnnotationTypeName;
        return resolveAnnotationTypeNameInSrcTypeHierarchy(scopeType, relativeAnnotationTypeName, hasMatchedPath);
    }
}
