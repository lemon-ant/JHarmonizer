package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;

import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.utils.FileNameUtils;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
// TODO Reconsider methods and remove superfluous
public class SpoonTypeUtils {
    private static final int ONE_ROOT_TYPE = 1;
    private static final TypeFilter<CtTypeMember> TYPE_MEMBER_FILTER = new TypeFilter<>(CtTypeMember.class);

    public static List<CtTypeMember> getAllTypeMembers(CtCompilationUnit compilationUnit) {
        return getAllTypes(compilationUnit).stream()
                .flatMap(type -> type.getElements(TYPE_MEMBER_FILTER).stream())
                .toList();
    }

    public static List<CtType<?>> getAllTypes(CtCompilationUnit compilationUnit) {
        return streamRootTypes(compilationUnit)
                .flatMap(SpoonTypeUtils::streamTypesTree)
                .toList();
    }

    public static List<CtType<?>> getRootTypes(CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes();
    }

    /**
     * Current order for a whole compilation unit: declared types as-is.
     */
    public static Stream<CtElement> streamDeclaredHierarchy(CtCompilationUnit compilationUnit) {
        return streamRootTypes(compilationUnit).flatMap(SpoonTypeUtils::streamTypeAndNestedElements);
    }

    public static CtType<?> findMainType(CtCompilationUnit compilationUnit) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() == ONE_ROOT_TYPE) {
            return declaredTypes.getFirst();
        }

        String baseName = FileNameUtils.getBaseName(compilationUnit.getFile().toPath());
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

    private static Stream<CtType<?>> streamRootTypes(CtCompilationUnit compilationUnit) {
        return getRootTypes(compilationUnit).stream();
    }

    /**
     * Current order for a type: the type, then its members as they are in lists; recurse for nested types.
     */
    private static Stream<CtElement> streamTypeAndNestedElements(CtType<?> ownerType) {
        Stream<CtElement> self = Stream.of(ownerType);
        Stream<CtElement> membersAndNested = streamExplicitSourceTypeMembers(ownerType)
                .flatMap(member -> {
                    if (member instanceof CtType<?> nestedType) {
                        // include the nested type declaration, then descend into its own members
                        return streamTypeAndNestedElements(nestedType);
                    }
                    return Stream.of(member);
                });
        return Stream.concat(self, membersAndNested);
    }

    private static Stream<CtType<?>> streamTypesTree(CtType<?> type) {
        return Stream.concat(Stream.of(type), type.getNestedTypes().stream().flatMap(SpoonTypeUtils::streamTypesTree));
    }
}
