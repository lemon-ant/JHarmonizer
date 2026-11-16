// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamExplicitSrcTypeMembers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.visitor.filter.TypeFilter;

/** Traverses source type trees and reported ranges while the translator prepares the processing model. */
@UtilityClass
public class SpoonTypeTreeUtils {

    /**
     * Collects types whose ranges reported by Spoon contain the source offset, from inner to outer scopes.
     * @param srcTypes precomputed types from one source file
     * @param srcOffset source offset to locate
     * @return unmodifiable list ordered by descending source start, excluding types without valid positions
     */
    @NonNull
    public static List<CtType<?>> collectEnclosingSrcTypes(@NonNull List<CtType<?>> srcTypes, int srcOffset) {
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
    public static List<CtType<?>> collectSrcTypeScopes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream()
                .flatMap(rootType -> rootType.getElements(new TypeFilter<CtType<?>>(CtType.class)).stream())
                .filter(typeScope -> !(typeScope instanceof CtTypeParameter))
                .toList();
    }

    /**
     * Streams top-level and member types through direct declaration nesting, excluding local and anonymous classes.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return top-level/member declarations in nesting order, without collecting an intermediate list
     */
    @NonNull
    public static Stream<CtType<?>> streamAllTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream().flatMap(SpoonTypeTreeUtils::streamTypesTree);
    }

    /**
     * Checks whether the source-file model has no sortable type declarations.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return {@code true} when the unit is not TYPE_DECLARATION or has no declared types
     */
    static boolean hasNoDeclaredTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getUnitType() != CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION
                || compilationUnit.getDeclaredTypes().isEmpty();
    }

    /**
     * Streams top-level types and their explicit, source-positioned members in current declaration-list order.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return types and members, recursively descending into member types
     */
    @NonNull
    static Stream<CtTypeMember> streamDeclaredHierarchy(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream().flatMap(SpoonTypeTreeUtils::streamTypeAndNestedElements);
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
        return Stream.concat(
                Stream.of(type), type.getNestedTypes().stream().flatMap(SpoonTypeTreeUtils::streamTypesTree));
    }
}
