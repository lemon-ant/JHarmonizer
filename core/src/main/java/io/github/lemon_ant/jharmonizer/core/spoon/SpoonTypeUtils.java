// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.Nullable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
// Shared type formatting, declaration traversal, and lexical-scope helpers belong to one stateless Spoon utility.
@SuppressWarnings("PMD.TooManyMethods")
public class SpoonTypeUtils {
    private static final int ONE_ROOT_TYPE = 1;

    /**
     * Finds the earliest source offset of a member declaration and its annotations.
     * Retains leading comments included in the member's source range reported by Spoon.
     * @param member source-positioned type member to inspect
     * @return first source offset including annotations outside the member's reported source range
     */
    // TODO(SPOON_SOURCE_START): Recheck CommentedAnnotation in printer scenario 21-annotation-group-gaps
    // after the next Spoon GA upgrade. Reproduced on 11.5.0 (latest GA checked 2026-10-19).
    // Remove this correction only when reported starts include earlier annotations and retain leading comments.
    public static int findEffectiveMemberStart(@NonNull CtTypeMember member) {
        // Spoon 11.5.0 reports CommentedAnnotation's start at "// moves with Bbb", after both @Ccc and @Bbb.
        // Taking the minimum restores both annotations and retains earlier comments already in the member range.
        return member.getAnnotations().stream()
                .map(CtAnnotation::getPosition)
                .filter(SourcePosition::isValidPosition)
                .mapToInt(SourcePosition::getSourceStart)
                .reduce(member.getPosition().getSourceStart(), Math::min);
    }

    /**
     * Finds the main type.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return the matching main type
     */
    @Nullable
    public static CtType<?> findMainType(@NonNull CtCompilationUnit compilationUnit) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() == ONE_ROOT_TYPE) {
            return declaredTypes.get(0);
        }

        String baseName =
                FilenameUtils.removeExtension(compilationUnit.getFile().getName());
        CtType<?> fileNameMatchType = null;
        for (CtType<?> type : declaredTypes) {
            if (type.hasModifier(ModifierKind.PUBLIC)) {
                // Any java file can contain only one main public class, that is main
                return type;
            }

            // If there is no public class, we fall back to the class that matches the file name by its class name
            if (fileNameMatchType == null && type.getSimpleName().equals(baseName)) {
                fileNameMatchType = type;
            }
        }

        // If name matching was found, then return this matched class, otherwise null
        return fileNameMatchType;
    }

    /**
     * Checks whether the source-file model has no sortable type declarations.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return {@code true} when the unit is not TYPE_DECLARATION or has no declared types
     */
    public static boolean hasNoDeclaredTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getUnitType() != CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION
                || compilationUnit.getDeclaredTypes().isEmpty();
    }

    /**
     * Streams top-level and member types through direct declaration nesting, excluding local and anonymous classes.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return top-level/member declarations in nesting order, without collecting an intermediate list
     */
    @NonNull
    public static Stream<CtType<?>> streamAllTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream().flatMap(SpoonTypeUtils::streamTypesTree);
    }

    /**
     * Streams top-level types and their explicit, source-positioned members in current declaration-list order.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return types and members, recursively descending into member types
     */
    @NonNull
    public static Stream<CtTypeMember> streamDeclaredHierarchy(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream().flatMap(SpoonTypeUtils::streamTypeAndNestedElements);
    }

    /**
     * Collects types whose ranges reported by Spoon contain the source offset, from inner to outer scopes.
     * @param srcTypes precomputed types from one source file
     * @param srcOffset source offset to locate
     * @return unmodifiable list ordered by descending source start, excluding types without valid positions
     */
    @NonNull
    static List<CtType<?>> collectEnclosingSrcTypes(@NonNull List<CtType<?>> srcTypes, int srcOffset) {
        return srcTypes.stream()
                .filter(srcType -> {
                    SourcePosition position = srcType.getPosition();
                    return position.isValidPosition()
                            && position.getSourceStart() <= srcOffset
                            && srcOffset <= position.getSourceEnd();
                })
                .sorted(Comparator.comparingInt(
                        srcType -> -srcType.getPosition().getSourceStart()))
                .toList();
    }

    /**
     * Collects source type declarations that can enclose a use, including local and anonymous classes.
     * These visibility scopes are separate from the top-level/member trees used for canonical declaration indexes.
     * @param compilationUnit original Spoon model of the source file
     * @return unmodifiable type scopes in Spoon traversal order
     */
    @NonNull
    static List<CtType<?>> collectSrcTypeScopes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream()
                .flatMap(rootType -> rootType.getElements(new TypeFilter<CtType<?>>(CtType.class)).stream())
                .filter(typeScope -> !(typeScope instanceof CtTypeParameter))
                .toList();
    }

    /**
     * Formats a named, non-array reference using dots between its recorded declaring types.
     * Owner references do not require available declarations; this method never calls {@code getDeclaration()}.
     * For example, {@code pkg.Outer$Tag} becomes {@code pkg.Outer.Tag}; a top-level {@code pkg.Dollar$Tag}
     * and the identifiers in {@code pkg.Dollar$Owner.Tag$Kind} retain their '$'.
     * Uses only the nesting recorded by Spoon; does not infer missing owners or resolve type aliases.
     * @param typeReference named, non-array type reference to format
     * @return qualified name with dot-separated declaring types
     */
    @NonNull
    static String formatCanonicalTypeName(@NonNull CtTypeReference<?> typeReference) {
        CtTypeReference<?> declaringTypeReference = typeReference.getDeclaringType();
        // No owner reference means getQualifiedName() adds no nesting separator; any '$' belongs to an identifier.
        // Replacing every '$' would corrupt legal names such as the top-level annotation Dollar$Tag.
        return declaringTypeReference == null
                ? typeReference.getQualifiedName()
                : formatCanonicalTypeName(declaringTypeReference) + '.' + typeReference.getSimpleName();
    }

    /**
     * Requires a selected declaration to be an annotation before using its identity or repeatable metadata.
     * @param typeDeclaration source or classpath declaration selected by lookup
     * @return declaration narrowed to an annotation type
     * @throws IllegalStateException if the selected declaration is another type
     */
    @NonNull
    static CtAnnotationType<?> requireAnnotationTypeDeclaration(@NonNull CtType<?> typeDeclaration) {
        if (typeDeclaration instanceof CtAnnotationType<?> annotationTypeDeclaration) {
            return annotationTypeDeclaration;
        }
        String declarationOrigin = typeDeclaration.isShadow() ? "external" : "source";
        throw new IllegalStateException("Expected an annotation declaration, but resolved " + declarationOrigin
                + " type " + formatCanonicalTypeName(typeDeclaration.getReference()));
    }

    /**
     * Matches a type-name prefix at complete name-segment boundaries, without resolving declarations or inheritance.
     * For prefix {@code Owner}, accepts {@code Owner} and {@code Owner.Tag}, but rejects {@code OwnerOther.Tag}.
     * @param typeNameToCheck tokenized type name to inspect
     * @param expectedTypeNamePrefix complete name segments expected at the beginning
     * @return {@code true} for an exact match or a prefix followed by a dot
     */
    static boolean startsWithTypeName(@NonNull String typeNameToCheck, @NonNull String expectedTypeNamePrefix) {
        return typeNameToCheck.equals(expectedTypeNamePrefix)
                || typeNameToCheck.startsWith(expectedTypeNamePrefix + '.');
    }

    /**
     * Current order for a type: the type, then its members as they are in lists; recurse for nested types.
     */
    @NonNull
    private static Stream<CtTypeMember> streamTypeAndNestedElements(CtType<?> ownerType) {
        Stream<CtTypeMember> self = Stream.of(ownerType);
        Stream<CtTypeMember> membersAndNested = streamExplicitSrcTypeMembers(ownerType)
                .flatMap(member -> {
                    if (member instanceof CtType<?> nestedType) {
                        // include the nested type declaration, then descend into its own members
                        return streamTypeAndNestedElements(nestedType);
                    }
                    return Stream.of(member);
                });
        return Stream.concat(self, membersAndNested);
    }

    @NonNull
    private static Stream<CtType<?>> streamTypesTree(CtType<?> type) {
        return Stream.concat(Stream.of(type), type.getNestedTypes().stream().flatMap(SpoonTypeUtils::streamTypesTree));
    }
}
