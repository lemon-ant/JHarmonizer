// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;

import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
// TODO Reconsider methods and remove superfluous
public class SpoonTypeUtils {
    private static final int ONE_ROOT_TYPE = 1;
    private static final TypeFilter<CtTypeMember> TYPE_MEMBER_FILTER = new TypeFilter<>(CtTypeMember.class);

    /**
     * Returns the all type members.
     * @param compilationUnit the compilation unit to inspect
     * @return the all type members
     */
    @NonNull
    public static List<CtTypeMember> getAllTypeMembers(@NonNull CtCompilationUnit compilationUnit) {
        return getAllTypes(compilationUnit).stream()
                .flatMap(type -> type.getElements(TYPE_MEMBER_FILTER).stream())
                .toList();
    }

    /**
     * Returns the all types.
     * @param compilationUnit the compilation unit to inspect
     * @return the all types
     */
    @NonNull
    public static List<CtType<?>> getAllTypes(@NonNull CtCompilationUnit compilationUnit) {
        return streamRootTypes(compilationUnit)
                .flatMap(SpoonTypeUtils::streamTypesTree)
                .toList();
    }

    /**
     * Returns the root types.
     * @param compilationUnit the compilation unit to inspect
     * @return the root types
     */
    @NonNull
    public static List<CtType<?>> getRootTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes();
    }

    /**
     * Checks whether the compilation unit has no declared types to sort.
     * @param compilationUnit the compilation unit to inspect
     * @return {@code true} when the unit is not TYPE_DECLARATION or has no declared types
     */
    public static boolean hasNoDeclaredTypes(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getUnitType() != CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION
                || compilationUnit.getDeclaredTypes().isEmpty();
    }

    /**
     * Current order for a whole compilation unit: declared types as-is.
     */
    @NonNull
    public static Stream<CtTypeMember> streamDeclaredHierarchy(@NonNull CtCompilationUnit compilationUnit) {
        return streamRootTypes(compilationUnit).flatMap(SpoonTypeUtils::streamTypeAndNestedElements);
    }

    /**
     * Finds the main type.
     * @param compilationUnit the compilation unit to inspect
     * @return the matching main type
     */
    @Nullable
    public static CtType<?> findMainType(@NonNull CtCompilationUnit compilationUnit) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() == ONE_ROOT_TYPE) {
            return declaredTypes.get(0);
        }

        String baseName = stripExtension(compilationUnit.getFile().getName());
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

    @NonNull
    private static Stream<CtType<?>> streamRootTypes(CtCompilationUnit compilationUnit) {
        return getRootTypes(compilationUnit).stream();
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

    /**
     * Returns the file name without its extension (the part before the last dot).
     *
     * @param fileName the file name to strip the extension from
     * @return the base name without the extension, or the original name when there is no dot
     */
    @NonNull
    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }

    @NonNull
    private static Stream<CtType<?>> streamTypesTree(CtType<?> type) {
        return Stream.concat(Stream.of(type), type.getNestedTypes().stream().flatMap(SpoonTypeUtils::streamTypesTree));
    }
}
