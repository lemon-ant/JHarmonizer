// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationDependencyKeyUtils.formatUnconfirmedGroupKey;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.SpoonTypeNameUtils.formatCanonicalTypeName;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeTreeUtils;
import java.lang.annotation.Repeatable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/** Computes dependency keys from annotation-use names and repeatable relationships declared in one source file. */
final class AnnotationDependencyKeyResolver {

    @NonNull
    private final AnnotationTypeNameResolver annotationTypeNameResolver;

    /** Exact single-type import evidence shared with annotation-use lookup. */
    @NonNull
    private final Set<String> canonicalSingleImportedTypeNames;

    // TODO Comment needed
    @NonNull
    private final Map<String, CtType<?>> declaredTypesByCanonicalName;

    /* TODO Add src examples of all cases. Previously it was as bellow
     *
     *  <ul>
     *     <li>{@code p.Tag -> p.Tag}: no repeatable relationship is declared in this source file.
     *         Terminal containers and external types whose metadata is not inspected also retain their own names.</li>
     *     <li>{@code p.Tag -> p.TagGroups}: follows a source-declared chain such as
     *         {@code Tag -> Tags -> TagGroups}; both {@code p.Tag} and {@code p.Tags} map to {@code p.TagGroups}.</li>
     *     <li>{@code p.Tag -> ?.library.Derived.Tags}: the annotation is confirmed, but its container name is not.
     *         Unconfirmed metadata terminals are values only; they receive no separate cache entry.</li>
     * </ul>
     * What is it Unconfirmed use keys bypass this cache?
     *
     * */
    /**
     * Maps confirmed canonical annotation names to terminal dependency keys. A confirmed terminal has a self-entry.
     * An unconfirmed container can appear as a value, as in {@code p.Tag -> ?.library.Derived.Tags}.
     * Unconfirmed use keys bypass this cache.
     */
    @NonNull
    private final Map<String, String> rootGroupKeyByAnnotationCanonicalTypeName;

    /**
     * Prepares source declarations and repeatable relationships before scanning annotation uses.
     * @param compilationUnit original Spoon model of the source file
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    AnnotationDependencyKeyResolver(@NonNull CtCompilationUnit compilationUnit) {
        rootGroupKeyByAnnotationCanonicalTypeName = new HashMap<>();
        declaredTypesByCanonicalName = SpoonTypeTreeUtils.streamAllTypes(compilationUnit)
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));
        SpoonTypeImportContext typeImportContext =
                SpoonImportUtils.collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        annotationTypeNameResolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        canonicalSingleImportedTypeNames = typeImportContext.getCanonicalSingleTypeNames();
        // Index every source annotation declaration. A defaulted @Tags container can hide its @Tag values,
        // and an unconfirmed @Tag use still needs the Tag -> Tags relationship for alias matching.
        // TODO Is it a warm up of the cache? Can we omit it?
        declaredTypesByCanonicalName.entrySet().stream()
                .filter(typeEntry -> typeEntry.getValue().isAnnotationType())
                .map(Map.Entry::getKey)
                .map(this::resolveRootGroupKey)
                // Register terminal self-entries after traversal, including confirmed external containers.
                // TODO Why do we do it??? Can it really be nested 2 times???
                .filter(AnnotationDependencyKeyUtils::isConfirmedGroupKey)
                .forEach(this::resolveRootGroupKey);
    }

    // TODO Need example of an original code, incoming groupKeysPerAnnotationInSrcOrder and result of processing
    /**
     * Expands the original keys of one adjacent annotation group using the file's repeatable relationships.
     * @param groupKeysPerAnnotationInSrcOrder read-only list of read-only key sets, one per outer annotation,
     *                                       including keys from its nested arguments
     * @return read-only key sets in the original annotation order
     */
    @NonNull
    List<Set<String>> collectConservativeOrderPreservationGroupKeys(
            @NonNull List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        return AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationCanonicalTypeName, groupKeysPerAnnotationInSrcOrder);
    }

    /**
     * Selects a confirmed type's terminal key or prefixes the unconfirmed name with {@code ?.}.
     * Only confirmed names can select source repeatable metadata.
     * @param annotationSrcStartOffset source offset of the annotation's opening {@code @}
     * @param writtenAnnotationTypeName tokenized type name with the written qualification
     * @return original dependency key for the annotation use
     */
    @NonNull
    // TODO Rename it, the name doesn't reflect the logic
    String resolveAnnotationCanonicalTypeNameOrGroupKey(
            int annotationSrcStartOffset, @NonNull String writtenAnnotationTypeName) {
        AnnotationTypeNameResolution annotationTypeNameResolution =
                annotationTypeNameResolver.resolveAnnotationTypeName(
                        annotationSrcStartOffset, writtenAnnotationTypeName);
        String annotationTypeName = annotationTypeNameResolution.getAnnotationTypeName();
        if (annotationTypeNameResolution.isCanonicalTypeNameConfirmed()) {
            return resolveRootGroupKey(annotationTypeName);
        }
        return formatUnconfirmedGroupKey(annotationTypeName);
    }

    @NonNull
    private static IllegalStateException createInvalidRepeatableContainerException(
            CtAnnotation<?> repeatableAnnotation, String reason) {
        SourcePosition position = repeatableAnnotation.getPosition();
        String location = position.isValidPosition() ? " at source offset " + position.getSourceStart() : "";
        return new IllegalStateException("Invalid @Repeatable container" + location + ": " + reason);
    }

    @NonNull
    private static CtTypeReference<?> resolveRepeatableContainerType(CtAnnotation<?> repeatableAnnotation) {
        // Read the class literal from Spoon's AST to avoid loading container classes during evaluation.
        CtExpression<?> containerValue = repeatableAnnotation.getValues().get("value");
        // Spoon parses Tags.class as a field read named "class", with Tags in both declaring and target types.
        // References can be unresolved, but the parsed class-literal structure must remain intact.
        if (!(containerValue instanceof CtFieldRead<?> classLiteral)
                || classLiteral.getVariable() == null
                || !"class".equals(classLiteral.getVariable().getSimpleName())
                || !(classLiteral.getTarget() instanceof CtTypeAccess<?> typeAccess)) {
            throw createInvalidRepeatableContainerException(
                    repeatableAnnotation, "expected a parsed class literal such as Tags.class");
        }
        // In a parsed Tags.class literal, the separate declaring and target references must both denote Tags.
        return Optional.ofNullable(classLiteral.getVariable().getDeclaringType())
                .filter(containerType -> Objects.equals(containerType, typeAccess.getAccessedType()))
                .orElseThrow(() -> createInvalidRepeatableContainerException(
                        repeatableAnnotation, "missing or inconsistent class-literal type references"));
    }

    /**
     * Follows source {@code @Repeatable} links and checks for cycles during the traversal.
     * Source declarations and exact single-type imports confirm each next name. An unconfirmed name ends the
     * traversal with a {@code ?.} key. A confirmed type without source metadata is its own terminal key.
     */
    @NonNull
    private String computeRootGroupKey(String annotationCanonicalTypeName) {
        Set<String> traversedRepeatableTypeNames = new HashSet<>();
        String currentAnnotationCanonicalTypeName = annotationCanonicalTypeName;
        while (true) {
            Optional<CtAnnotation<?>> repeatableAnnotation = Optional.ofNullable(
                            declaredTypesByCanonicalName.get(currentAnnotationCanonicalTypeName))
                    .map(SpoonTypeNameUtils::requireAnnotationTypeDeclaration)
                    .flatMap(annotationDeclaration -> annotationDeclaration.getAnnotations().stream()
                            .filter(metaAnnotation -> Repeatable.class
                                    .getName()
                                    .equals(formatCanonicalTypeName(metaAnnotation.getAnnotationType())))
                            .findFirst());
            if (repeatableAnnotation.isEmpty()) {
                return currentAnnotationCanonicalTypeName;
            }
            if (!traversedRepeatableTypeNames.add(currentAnnotationCanonicalTypeName)) {
                throw createInvalidRepeatableContainerException(
                        repeatableAnnotation.get(),
                        "cyclic container relationship for " + currentAnnotationCanonicalTypeName);
            }
            CtTypeReference<?> containerType = resolveRepeatableContainerType(repeatableAnnotation.get());
            String containerTypeName = formatCanonicalTypeName(containerType);
            // A formatted Spoon reference can still be an inherited owner alias such as Derived.Tags.
            if (!declaredTypesByCanonicalName.containsKey(containerTypeName)
                    && !canonicalSingleImportedTypeNames.contains(containerTypeName)) {
                return formatUnconfirmedGroupKey(containerTypeName);
            }
            currentAnnotationCanonicalTypeName = containerTypeName;
        }
    }

    @NonNull
    private String resolveRootGroupKey(String annotationCanonicalTypeName) {
        return rootGroupKeyByAnnotationCanonicalTypeName.computeIfAbsent(
                annotationCanonicalTypeName, this::computeRootGroupKey);
    }
}
