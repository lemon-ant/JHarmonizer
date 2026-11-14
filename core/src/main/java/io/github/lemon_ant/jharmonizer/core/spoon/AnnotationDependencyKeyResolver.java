// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

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

    /**
     * Canonical-name evidence from declarations, imports, and resolved uses; consulted only for annotation identities.
     * Names match exactly: importing library.Owner does not confirm library.Owner.Tag.
     * This processor owns and extends the set; the collector only reads it during a synchronous call.
     */
    @NonNull
    private final Set<String> confirmedCanonicalTypeNames;

    /** Shared exact-name index for source annotation declarations and wildcard-import owners. */
    @NonNull
    private final Map<String, CtType<?>> declaredTypesByCanonicalName;

    /**
     * Maps resolved annotation/container names and unresolved lexical identities to order-preservation keys:
     * <ul>
     *     <li>{@code p.Tag -> p.Tag}: no repeatable relationship is declared in this source file.
     *         Terminal containers and external types whose metadata is not inspected also retain their own names.</li>
     *     <li>{@code p.Tag -> p.TagGroups}: follows a source-declared chain such as
     *         {@code Tag -> Tags -> TagGroups}; both {@code p.Tag} and {@code p.Tags} map to {@code p.TagGroups}.</li>
     *     <li>{@code ?.Outer.Tag -> ?.Outer.Tag}: unconfirmed identity, with a fallback name or the original written
     *         name prefixed by {@code ?.}, which cannot occur in a Java type name.</li>
     * </ul>
     * Source annotation declarations are indexed eagerly; lookups also cache identities and containers reached recursively.
     * Resolved names are first seeded with themselves during recursive lookup, then updated if a container is found.
     * External repeatable declarations are not inspected. The collector reads this index to add pair-specific keys
     * to fragment dependency sets.
     */
    @NonNull
    private final Map<String, String> rootContainerTypeNameByAnnotationTypeName;

    /**
     * Prepares dependency metadata and shared source declarations for one source file.
     *
     * @param compilationUnit source type-resolution context
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    AnnotationDependencyKeyResolver(@NonNull CtCompilationUnit compilationUnit) {
        rootContainerTypeNameByAnnotationTypeName = new HashMap<>();
        // Metadata identities require canonical top-level/member owner chains.
        // Local and anonymous visibility scopes are indexed separately by the declaration resolver.
        List<CtType<?>> canonicalTypeDeclarations = SpoonTypeUtils.getAllTypes(compilationUnit);
        declaredTypesByCanonicalName = canonicalTypeDeclarations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        srcType -> formatCanonicalTypeName(srcType.getReference()), srcType -> srcType));
        SpoonTypeImportContext typeImportContext =
                SpoonImportUtils.collectTypeImportContext(compilationUnit, declaredTypesByCanonicalName);
        annotationTypeNameResolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        // Declarations expose their kind: ordinary source types cannot be annotation identities.
        // Nested @interface declarations are included independently of their owner's kind.
        Set<String> confirmedCanonicalTypeNames = declaredTypesByCanonicalName.entrySet().stream()
                .filter(typeEntry -> typeEntry.getValue().isAnnotationType())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
        // @Tags can precede its @Tag occurrence and omit a defaulted @Tag value.
        // Seed source relationships now so group checks still see Tag's original identity behind the Tags key.
        // Populate cache rootContainerTypeNameByAnnotationTypeName
        confirmedCanonicalTypeNames.forEach(this::findRootContainerTypeNameInAllDeclaredTypes);
        // Imported declarations can be unavailable, so keep canonical names without requiring annotation-kind metadata.
        // The collector consults imported names only when an annotation identity matches them exactly.
        confirmedCanonicalTypeNames.addAll(typeImportContext.getCanonicalSingleTypeNames());
        this.confirmedCanonicalTypeNames = confirmedCanonicalTypeNames;
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
     * <p>Pair keys preserve only possible alias relationships. If source-declared {@code First.Tag} and
     * {@code Second.Tag} are distinct, but an unavailable {@code Derived.Tag} could denote either one,
     * it shares a separate pair key with each. The two confirmed types remain independently sortable.
     * Original keys also remain: source-declared {@code Tag -> Tags} relationships expose Tag's simple name
     * even when only a defaulted {@code @Tags} container is visible.
     *
     * The input list and its sets must be unmodifiable; their producers must not mutate backing collections after handoff.
     * Unchanged input collections can be reused.
     * @param groupKeysInAnnotationOrder original key sets of one source group in annotation order
     * @return unmodifiable expanded key sets in an unmodifiable list in the same annotation order
     */
    @NonNull
    List<Set<String>> collectConservativeOrderPreservationGroupKeys(
            @NonNull List<Set<String>> groupKeysInAnnotationOrder) {
        return AnnotationOrderPreservationKeyCollector.collect(
                groupKeysInAnnotationOrder, rootContainerTypeNameByAnnotationTypeName, confirmedCanonicalTypeNames);
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
    String resolveAnnotationTypeNameOrRootContainerTypeName(
            int annotationSrcStartOffset, @NonNull String writtenAnnotationTypeName) {
        AnnotationTypeNameResolution annotationTypeNameResolution =
                annotationTypeNameResolver.resolveAnnotationTypeName(
                        annotationSrcStartOffset, writtenAnnotationTypeName);
        String annotationTypeName = annotationTypeNameResolution.getAnnotationTypeName();
        if (annotationTypeNameResolution.isCanonicalTypeNameConfirmed()) {
            confirmedCanonicalTypeNames.add(annotationTypeName);
            return findRootContainerTypeNameInAllDeclaredTypes(annotationTypeName);
        }
        // Unconfirmed results include a blocked lookup, insufficient context, and accepted provisional names:
        // ordinary @Deprecated with no supported context -> ?.Deprecated;
        // Runtime-confirmed module names are handled above; unknown qualified spellings can remain lexical.
        // A fallback spelling can coincide with a different source type's canonical name:
        // package EqualsAndHashCode; import lombok.*; @EqualsAndHashCode.Exclude uses Lombok's annotation,
        // even if this file declares an ordinary class Exclude. Do not read that class's annotation metadata.
        // Always use a separate lexical key namespace; matching a canonical string cannot confirm this use.
        String lexicalGroupKey = "?." + annotationTypeName;
        rootContainerTypeNameByAnnotationTypeName.putIfAbsent(lexicalGroupKey, lexicalGroupKey);
        return lexicalGroupKey;
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

    /**
     * Resolves and caches the terminal name in the source-declared {@code @Repeatable} container chain.
     * For {@code Tag -> Tags -> TagGroups}, all three names resolve to {@code TagGroups}.
     * Without a relationship declared in this source file, returns the input name; external metadata is not loaded.
     *
     * @param annotationTypeName resolved annotation type name, including a container reached recursively
     * @return last reachable container's name, or the input name if no source-declared container is found
     * @throws IllegalStateException if a source declaration is not an annotation type, repeatable metadata is malformed,
     *                               or container relationships form a cycle
     */
    @NonNull
    private String findRootContainerTypeNameInAllDeclaredTypes(String annotationTypeName) {
        // Keep explicit cache access: resolving @Repeatable(Tags.class) can recursively cache Tags.
        // A computeIfAbsent mapping function must not modify this same map; HashMap can throw
        // ConcurrentModificationException even for an acyclic Tag -> Tags relationship.
        String cachedGroupKey = rootContainerTypeNameByAnnotationTypeName.get(annotationTypeName);
        if (cachedGroupKey != null) {
            return cachedGroupKey;
        }
        Optional<CtAnnotation<?>> repeatableAnnotation = Optional.ofNullable(
                        declaredTypesByCanonicalName.get(annotationTypeName))
                .map(SpoonTypeUtils::requireAnnotationTypeDeclaration)
                .flatMap(annotationDeclaration -> annotationDeclaration.getAnnotations().stream()
                        .filter(metaAnnotation -> Repeatable.class
                                .getName()
                                .equals(formatCanonicalTypeName(metaAnnotation.getAnnotationType())))
                        .findFirst());
        // Cache after annotation-kind validation, so a rejected declaration cannot bypass it on a later lookup.
        // Seed before following containers; a chain returning to this name is rejected below.
        rootContainerTypeNameByAnnotationTypeName.put(annotationTypeName, annotationTypeName);
        // An unavailable external declaration or an annotation without @Repeatable retains its own identity.
        if (repeatableAnnotation.isEmpty()) {
            return annotationTypeName;
        }
        CtTypeReference<?> containerType = resolveRepeatableContainerType(repeatableAnnotation.get());
        String rootContainerTypeName =
                findRootContainerTypeNameInAllDeclaredTypes(formatCanonicalTypeName(containerType));
        if (annotationTypeName.equals(rootContainerTypeName)) {
            throw createInvalidRepeatableContainerException(
                    repeatableAnnotation.get(), "cyclic container relationship for " + annotationTypeName);
        }
        rootContainerTypeNameByAnnotationTypeName.put(annotationTypeName, rootContainerTypeName);
        return rootContainerTypeName;
    }
}
