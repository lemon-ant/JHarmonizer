package io.github.lemon_ant.jharmonizer.core.spoon;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.utils.FileNameUtils;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
public class SpoonCompilationUnitUtilities {
    private static final int ONE_ROOT_TYPE = 1;
    private static final TypeFilter<CtTypeMember> TYPE_MEMBER_FILTER = new TypeFilter<>(CtTypeMember.class);

    public static List<CtType<?>> getRootTypes(CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes();
    }

    public static List<CtTypeMember> getAllTypeMembers(CtCompilationUnit compilationUnit) {
        return getAllTypes(compilationUnit).stream()
                .flatMap(type -> type.getElements(TYPE_MEMBER_FILTER).stream())
                .toList();
    }

    public static List<CtType<?>> getAllTypes(CtCompilationUnit compilationUnit) {
        return getRootTypes(compilationUnit).stream()
                .flatMap(SpoonCompilationUnitUtilities::findTypesTree)
                .toList();
    }

    private static Stream<CtType<?>> findTypesTree(CtType<?> type) {
        return Streams.concat(
                Stream.of(type), type.getNestedTypes().stream().flatMap(SpoonCompilationUnitUtilities::findTypesTree));
    }

    static CtType<?> findMainType(CtCompilationUnit compilationUnit) {
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
}
