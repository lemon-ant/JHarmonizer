// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import spoon.experimental.CtUnresolvedImport;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeMemberWildcardImportReference;
import spoon.reflect.reference.CtTypeReference;

/** Reads import names and shapes from the original Spoon model. */
@UtilityClass
class SpoonImportUtils {

    /**
     * Collects reusable import facts without choosing an annotation identity or validating imported declaration kinds.
     * @param compilationUnit original Spoon model of the source file
     * @param declaredTypesByCanonicalName source declarations indexed by canonical type name
     * @return read-only import names, static-member blockers, all wildcard imports, and their available source owners
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static SpoonTypeImportContext collectTypeImportContext(
            @NonNull CtCompilationUnit compilationUnit, @NonNull Map<String, CtType<?>> declaredTypesByCanonicalName) {
        Set<String> canonicalSingleTypeNames = new HashSet<>();
        Map<String, CtImport> singleTypeImportsBySimpleName = new HashMap<>();
        Set<String> unresolvableStaticMemberSimpleNames = new HashSet<>();
        List<CtImport> wildcardImportsInModelOrder = new ArrayList<>();
        Map<CtImport, CtType<?>> srcOwnerTypesByWildcardImport = new IdentityHashMap<>();
        for (CtImport typeImport : compilationUnit.getImports()) {
            if (isWildcardImport(typeImport)) {
                wildcardImportsInModelOrder.add(typeImport);
                Optional.ofNullable(findSrcWildcardImportOwnerType(declaredTypesByCanonicalName, typeImport))
                        .ifPresent(srcOwnerType -> srcOwnerTypesByWildcardImport.put(typeImport, srcOwnerType));
                continue;
            }
            if (typeImport instanceof CtUnresolvedImport unresolvedImport && unresolvedImport.isStatic()) {
                // An owner from another file/library can be unavailable in this single-file, no-classpath model.
                // E.g. import static library.Owner.Tag; stays unresolved; Tag could be a type, field, or method.
                // Without its declaration, no canonical annotation identity can be established: keep a lookup blocker.
                // a.Owner.Tag and b.Owner.Tag both block Tag; the full imports remain in the source model.
                String writtenStaticImportName = unresolvedImport.getUnresolvedReference();
                unresolvableStaticMemberSimpleNames.add(
                        writtenStaticImportName.substring(writtenStaticImportName.lastIndexOf('.') + 1));
                continue;
            }
            String canonicalSingleTypeName = extractCanonicalSingleImportTypeName(typeImport);
            if (canonicalSingleTypeName == null) {
                // Wildcards and unresolved static imports were handled above; resolved fields and methods remain:
                // import static java.lang.Integer.MAX_VALUE; -> CtFieldReference
                // import static java.util.Collections.emptyList; -> CtExecutableReference
                // Spoon can omit a co-imported inherited type: Base declares annotation Deprecated, while
                // Owner extends Base declares field Deprecated; import static Owner.Deprecated can appear only
                // as a field reference. Keep its simple name as a blocker, without inventing a type identity.
                unresolvableStaticMemberSimpleNames.add(
                        typeImport.getReference().getSimpleName());
                continue;
            }
            canonicalSingleTypeNames.add(canonicalSingleTypeName);
            String importedSimpleName = canonicalSingleTypeName.substring(canonicalSingleTypeName.lastIndexOf('.') + 1);
            singleTypeImportsBySimpleName.putIfAbsent(importedSimpleName, typeImport);
        }
        return new SpoonTypeImportContext(
                canonicalSingleTypeNames,
                singleTypeImportsBySimpleName,
                srcOwnerTypesByWildcardImport,
                unresolvableStaticMemberSimpleNames,
                wildcardImportsInModelOrder);
    }

    /**
     * Extracts a formatted type-reference name or the canonical spelling of an unresolved ordinary single import.
     * Reads Spoon's classification without resolving imports or guessing the kind of an unresolved static member.
     * @param typeImport original parsed import
     * @return type name, or {@code null} for wildcards, field/method references, or unresolved static imports
     */
    @Nullable
    static String extractCanonicalSingleImportTypeName(@NonNull CtImport typeImport) {
        if (typeImport.getReference() instanceof CtTypeReference<?> importedType) {
            // Wildcard imports use package/member-wildcard references, so this is a single type.
            return formatCanonicalTypeName(importedType);
        }
        if (typeImport instanceof CtUnresolvedImport unresolvedImport
                && !unresolvedImport.isStatic()
                && !isWildcardImport(typeImport)) {
            // JDTImportBuilder always stores the source spelling; a null string requires an incomplete/modified model.
            // A compilable ordinary single import names a type even when its declaration is unavailable.
            return unresolvedImport.getUnresolvedReference();
        }
        // Unknown static members can denote types, fields, or methods; they provide no definite type name.
        // Wildcards and resolved non-type references also return null:
        // import java.util.*;                         -> CtPackageReference
        // import static java.lang.annotation.ElementType.*; -> CtTypeMemberWildcardImportReference
        // import static java.lang.Integer.MAX_VALUE;  -> CtFieldReference
        // import static java.util.Collections.emptyList; -> CtExecutableReference
        return null;
    }

    /**
     * Checks whether a parsed import ends in {@code .*}, including static and unresolved imports.
     * @param typeImport original parsed import to inspect
     * @return {@code true} for a wildcard import
     */
    static boolean isWildcardImport(@NonNull CtImport typeImport) {
        return typeImport.getImportKind() == CtImportKind.ALL_TYPES
                || typeImport.getImportKind() == CtImportKind.ALL_STATIC_MEMBERS
                || typeImport instanceof CtUnresolvedImport unresolvedImport
                        && unresolvedImport.getUnresolvedReference().endsWith(".*");
    }

    /** Receives an import already classified as wildcard; source owners may be ordinary classes or interfaces. */
    @Nullable
    private static CtType<?> findSrcWildcardImportOwnerType(
            Map<String, CtType<?>> declaredTypesByCanonicalName, CtImport wildcardImport) {
        if (wildcardImport.getReference()
                instanceof CtTypeMemberWildcardImportReference wildcardAllStaticTypeMembersImport) {
            // Our parser loads one source file, so an import owner's source declaration belongs to this file.
            // Keep the type reference's getDeclaration(): external owners return null, not classpath shadows.
            return wildcardAllStaticTypeMembersImport.getTypeReference().getDeclaration();
        }
        if (wildcardImport instanceof CtUnresolvedImport unresolvedImport) {
            // Spoon 11.5.0 preserves import p.Owner.* as unresolved even for a source-declared Owner.
            // Both ordinary and static on-demand type imports require a canonical owner name (JLS 7.5.2/7.5.4).
            String writtenOwnerTypeName = unresolvedImport.getUnresolvedReference();
            // Remove the trailing ".*" (two characters): "p.Owner.*" becomes "p.Owner".
            // The source declaration index needs the owner's type name, without the wildcard suffix.
            String canonicalOwnerTypeName = writtenOwnerTypeName.substring(0, writtenOwnerTypeName.length() - 2);
            return declaredTypesByCanonicalName.get(canonicalOwnerTypeName);
        }
        // The remaining wildcard form is a package import: import java.util.*; -> CtPackageReference.
        // It imports top-level types from a package, so there is no single declaring owner type to return.
        return null;
    }
}
