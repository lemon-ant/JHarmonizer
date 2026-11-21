// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtType;

/** Import facts shared across annotation lookup in one source file. */
@Value
@Getter(AccessLevel.PACKAGE)
class SpoonTypeImportContext {

    // TODO  Comment
    @NonNull
    Set<String> canonicalSingleTypeNames;

    // TODO  Comment
    @NonNull
    Map<String, CtImport> singleTypeImportsBySimpleName;

    /** Available source owners keyed by the identity of the original wildcard import. */
    @NonNull
    Map<CtImport, CtType<?>> srcOwnerTypesByWildcardImport;

    /**
     * Simple member names from single static imports that do not establish a type identity in this source-file model.
     * Includes unresolved imports and imports Spoon represents only as fields or methods.
     * For example, {@code import static library.Owner.Tag;} may also import an inherited type Tag that Spoon 11.5.0
     * omits from the import model even when it reports a same-name field. Neither shape proves the annotation identity.
     */
    @NonNull
    Set<String> unresolvableStaticMemberSimpleNames;

    /** Every wildcard import in Spoon's model order, including imports without an available source owner. */
    @NonNull
    List<CtImport> wildcardImportsInModelOrder;

    /**
     * Retains read-only views. The supplied collections must remain unchanged after construction.
     * @param canonicalSingleTypeNames canonical names established by single-type imports
     * @param singleTypeImportsBySimpleName definite single-type imports indexed by simple type name
     * @param srcOwnerTypesByWildcardImport available source owners indexed by original wildcard-import identity
     * @param unresolvableStaticMemberSimpleNames simple names that block later resolution guesses
     * @param wildcardImportsInModelOrder all wildcard imports in the original Spoon model order
     */
    SpoonTypeImportContext(
            @NonNull Set<String> canonicalSingleTypeNames,
            @NonNull Map<String, CtImport> singleTypeImportsBySimpleName,
            @NonNull Map<CtImport, CtType<?>> srcOwnerTypesByWildcardImport,
            @NonNull Set<String> unresolvableStaticMemberSimpleNames,
            @NonNull List<CtImport> wildcardImportsInModelOrder) {
        this.canonicalSingleTypeNames = Collections.unmodifiableSet(canonicalSingleTypeNames);
        this.singleTypeImportsBySimpleName = Collections.unmodifiableMap(singleTypeImportsBySimpleName);
        this.srcOwnerTypesByWildcardImport = Collections.unmodifiableMap(srcOwnerTypesByWildcardImport);
        this.unresolvableStaticMemberSimpleNames = Collections.unmodifiableSet(unresolvableStaticMemberSimpleNames);
        this.wildcardImportsInModelOrder = Collections.unmodifiableList(wildcardImportsInModelOrder);
    }

    /**
     * Reports whether the original source file contains any wildcard imports, including unavailable owners.
     * @return whether at least one wildcard import was collected
     */
    boolean hasWildcardImports() {
        return !wildcardImportsInModelOrder.isEmpty();
    }
}
