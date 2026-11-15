// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.AnnotationDependencyKeyUtils.formatUnconfirmedGroupKey;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.formatCanonicalTypeName;

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

/**
 * Computes annotation ordering dependency keys from type names and source-declared repeatable containers.
 * Delegates type-name resolution to {@link AnnotationTypeNameResolver}; a key can identify the annotation itself,
 * its repeatable container, or an unresolved written name whose relative order must be preserved.
 */
final class AnnotationDependencyKeyResolver {

    @NonNull
    private final AnnotationTypeNameResolver annotationTypeNameResolver;

    /** Exact single-type import evidence shared with annotation-use lookup. */
    @NonNull
    private final Set<String> canonicalSingleImportedTypeNames;

    /** Shared exact-name index for source annotation declarations and wildcard-import owners. */
    @NonNull
    private final Map<String, CtType<?>> declaredTypesByCanonicalName;

    /**
     * Maps only confirmed canonical annotation names to terminal order-preservation keys:
     * <ul>
     *     <li>{@code p.Tag -> p.Tag}: no repeatable relationship is declared in this source file.
     *         Terminal containers and external types whose metadata is not inspected also retain their own names.</li>
     *     <li>{@code p.Tag -> p.TagGroups}: follows a source-declared chain such as
     *         {@code Tag -> Tags -> TagGroups}; both {@code p.Tag} and {@code p.Tags} map to {@code p.TagGroups}.</li>
     *     <li>{@code p.Tag -> ?.library.Derived.Tags}: the annotation is confirmed, but its container name is not.
     *         Unconfirmed metadata terminals are values only; they receive no separate cache entry.</li>
     * </ul>
     * Source repeatable relationships are prepared before scanning uses, including defaulted containers whose repeated
     * annotation has no earlier confirmed use. Declarations and exact single-type imports supply canonical-name evidence.
     * All cache reads and writes for root-key lookup are confined to {@link #resolveRootGroupKey(String)}.
     * Computed keys are cached only after successful traversal; computation uses no cache entries or recursion markers.
     * Every confirmed terminal key retains a key-to-itself entry, including confirmed external containers.
     * Unconfirmed use keys bypass this cache; the collector reads them directly from the current annotation sequence.
     * External repeatable declarations are not inspected. The collector reads this index to add possible type/container
     * group keys to fragment dependency sets.
     */
    @NonNull
    private final Map<String, String> rootGroupKeyByAnnotationCanonicalTypeName;

    /**
     * Prepares dependency metadata and shared source declarations for one source file.
     *
     * @param compilationUnit source type-resolution context
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    AnnotationDependencyKeyResolver(@NonNull CtCompilationUnit compilationUnit) {
        rootGroupKeyByAnnotationCanonicalTypeName = new HashMap<>();
        // Metadata identities require canonical top-level/member owner chains.
        // Local and anonymous visibility scopes are indexed separately by the declaration resolver.
        declaredTypesByCanonicalName = SpoonTypeUtils.streamAllTypes(compilationUnit)
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));
        SpoonTypeImportContext typeImportContext =
                SpoonImportUtils.collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        annotationTypeNameResolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        canonicalSingleImportedTypeNames = typeImportContext.getCanonicalSingleTypeNames();
        // Declarations expose their kind: ordinary source types cannot be annotation identities.
        // Nested @interface declarations are included independently of their owner's kind.
        // @Tags can precede its @Tag occurrence and omit a defaulted @Tag value.
        // Collect Tag -> Tags even when only @Tags and an unconfirmed @p.Owner.Tag appear before Tag's declaration.
        // A cache populated solely by confirmed visible uses would contain only Tags -> Tags and lose this dependency.
        // Import evidence is available before metadata traversal, so imported containers retain canonical keys.
        declaredTypesByCanonicalName.entrySet().stream()
                .filter(typeEntry -> typeEntry.getValue().isAnnotationType())
                .map(Map.Entry::getKey)
                .map(this::resolveRootGroupKey)
                // Index confirmed terminal identities after computation returns, including imported external
                // containers.
                .filter(AnnotationDependencyKeyUtils::isConfirmedGroupKey)
                .forEach(this::resolveRootGroupKey);
    }

    /**
     * Collects conservative dependency keys while retaining the original keys of each annotation fragment.
     *
     * <p>For example, an external {@code Base} declares a repeatable {@code Tag}, and
     * {@code Derived extends Base} inherits it without declaring another {@code Tag}. One consumer file can use:
     * <pre>{@code
     * import library.Base.Tag;
     * class Example {
     *     @library.Derived.Tag("z")
     *     @Tag("a")
     *     int value;
     * }
     * }</pre>
     * Both occurrences denote {@code library.Base.Tag}; sorting by written length would reverse their values.
     * Without the external hierarchy, {@code Derived.Tag} could instead be an independently declared annotation.
     * Give these two identities a shared dependency key; unrelated annotations remain sortable in the same group.
     * Java permits inherited qualified aliases (JLS 6.7); ordinary single-type imports require canonical names
     * (JLS 7.5.1). An unavailable library alone does not invalidate an ordinary {@code import library.Base.Tag}.
     *
     * <p>Alternative group keys preserve possible alias relationships. If source-declared {@code First.Tag} and
     * {@code Second.Tag} are distinct, but an unavailable {@code Derived.Tag} could denote either one,
     * its key set includes both alternatives. The two confirmed types keep their separate singleton key sets
     * and remain independently sortable.
     * Original keys also remain: source-declared {@code Tag -> Tags} relationships expose Tag's simple name
     * even when only a defaulted {@code @Tags} container is visible.
     *
     * The input list and its sets must be unmodifiable; their producers must not mutate backing collections after handoff.
     * Unchanged input collections can be reused.
     * @param groupKeysPerAnnotationInSrcOrder one key set per outer annotation of the current adjacent sequence;
     *                                       each set includes keys from nested annotation arguments
     * @return unmodifiable expanded key sets in an unmodifiable list in the same annotation order
     */
    @NonNull
    List<Set<String>> collectConservativeOrderPreservationGroupKeys(
            @NonNull List<Set<String>> groupKeysPerAnnotationInSrcOrder) {
        return AnnotationOrderPreservationKeyCollector.collect(
                rootGroupKeyByAnnotationCanonicalTypeName, groupKeysPerAnnotationInSrcOrder);
    }

    /**
     * Resolves an annotation identity, optionally sharing its source-declared repeatable container's key.
     * Only canonical-identity-confirmed names can select source repeatable metadata. All other results retain
     * an opaque lexical key for conservative same-name dependencies, using the fallback name supplied by the resolver.
     * A fallback name does not confirm where the annotation was declared.
     * External repeatable metadata is not inspected; unavailable metadata does not invalidate a known type identity.
     * The offset retrieves an optional AST binding hint, which can differ from the written qualification:
     * {@code @Derived.Tag} can bind to {@code Base.Tag}, while import matching still needs {@code Derived.Tag}.
     * The scanner supplies the written name even when no positioned AST annotation is available.
     *
     * @param annotationSrcStartOffset source offset of the annotation's opening {@code @}
     * @param writtenAnnotationTypeName          tokenized annotation type name retaining the qualification written in the source
     * @return resolved dependency key, or a lexical fallback when the type identity is unavailable
     * @throws IllegalStateException if a resolved source declaration is not an annotation type
     */
    @NonNull
    String resolveAnnotationCanonicalTypeNameOrGroupKey(
            int annotationSrcStartOffset, @NonNull String writtenAnnotationTypeName) {
        AnnotationTypeNameResolution annotationTypeNameResolution =
                annotationTypeNameResolver.resolveAnnotationTypeName(
                        annotationSrcStartOffset, writtenAnnotationTypeName);
        String annotationTypeName = annotationTypeNameResolution.getAnnotationTypeName();
        if (annotationTypeNameResolution.isCanonicalTypeNameConfirmed()) {
            return resolveRootGroupKey(annotationTypeName);
        }
        // Unconfirmed results include a blocked lookup, insufficient context, and accepted provisional names:
        // ordinary @Deprecated with no supported context -> ?.Deprecated;
        // Runtime-confirmed module names are handled above; unknown qualified spellings can remain lexical.
        // A fallback spelling can coincide with a different source type's canonical name:
        // package EqualsAndHashCode; import lombok.*; @EqualsAndHashCode.Exclude uses Lombok's annotation,
        // even if this file declares an ordinary class Exclude. Do not read that class's annotation metadata.
        // Always use a separate lexical key namespace; matching a canonical string cannot confirm this use.
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
        // Read the class literal directly: evaluating it tries to load source-only container classes.
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
        // Spoon clones the accessed reference for Tags.class; Objects.equals compares their structure.
        // An absent declaring reference remains invalid even when the accessed reference is also absent.
        return Optional.ofNullable(classLiteral.getVariable().getDeclaringType())
                .filter(containerType -> Objects.equals(containerType, typeAccess.getAccessedType()))
                .orElseThrow(() -> createInvalidRepeatableContainerException(
                        repeatableAnnotation, "missing or inconsistent class-literal type references"));
    }

    /** Computes the source repeatable-chain key independently of the cache, tracking cycles only within this traversal. */
    @NonNull
    private String computeRootGroupKey(String annotationCanonicalTypeName) {
        Set<String> traversedRepeatableTypeNames = new HashSet<>();
        String currentAnnotationCanonicalTypeName = annotationCanonicalTypeName;
        while (true) {
            Optional<CtAnnotation<?>> repeatableAnnotation = Optional.ofNullable(
                            declaredTypesByCanonicalName.get(currentAnnotationCanonicalTypeName))
                    .map(SpoonTypeUtils::requireAnnotationTypeDeclaration)
                    .flatMap(annotationDeclaration -> annotationDeclaration.getAnnotations().stream()
                            .filter(metaAnnotation -> Repeatable.class
                                    .getName()
                                    .equals(formatCanonicalTypeName(metaAnnotation.getAnnotationType())))
                            .findFirst());
            // An unavailable external declaration or an annotation without @Repeatable retains its own identity.
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
            // Follow only declaration/import-confirmed names; otherwise retain an unconfirmed terminal key.
            if (!declaredTypesByCanonicalName.containsKey(containerTypeName)
                    && !canonicalSingleImportedTypeNames.contains(containerTypeName)) {
                return formatUnconfirmedGroupKey(containerTypeName);
            }
            currentAnnotationCanonicalTypeName = containerTypeName;
        }
    }

    /**
     * Finds and caches the terminal group key in a source-declared {@code @Repeatable} container chain.
     * For {@code Tag -> Tags -> TagGroups}, all three names resolve to {@code TagGroups}.
     * Source declarations and exact single-type imports confirm container names before following the chain.
     * An unconfirmed external terminal receives {@code ?.} and stops traversal without becoming a cache key.
     *
     * @param annotationCanonicalTypeName confirmed annotation or container type name
     * @return terminal dependency key, or the input name when no source-declared container is found
     * @throws IllegalStateException if a source declaration is not an annotation type, repeatable metadata is malformed,
     *                               or container relationships form a cycle
     */
    @NonNull
    private String resolveRootGroupKey(String annotationCanonicalTypeName) {
        return rootGroupKeyByAnnotationCanonicalTypeName.computeIfAbsent(
                annotationCanonicalTypeName, this::computeRootGroupKey);
    }
}
