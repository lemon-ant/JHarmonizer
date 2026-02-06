package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

@UtilityClass
public class SpoonTypeMemberUtils {
    @NonNull
    public static CtTypeReference<?> normalizeTypeReferenceByErasureAndBoxing(
            @NonNull CtTypeReference<?> typeReference) {
        // Use type erasure + boxing to match boolean/Boolean and to ignore generic parameters.
        CtTypeReference<?> erasedTypeReference = typeReference.getTypeErasure();
        return erasedTypeReference.isPrimitive() ? erasedTypeReference.box() : erasedTypeReference;
    }

    static int deriveVisibilityRank(@NonNull CtTypeMember typeMember) {
        // Ascending: public (0) -> protected (1) -> package-private (2) -> private (3)
        Set<ModifierKind> modifiers = typeMember.getModifiers();
        if (modifiers.contains(ModifierKind.PUBLIC)) {
            return 0;
        }
        if (modifiers.contains(ModifierKind.PROTECTED)) {
            return 1;
        }
        if (modifiers.contains(ModifierKind.PRIVATE)) {
            return 3;
        }
        return 2; // package-private
    }

    static int extractSourceStart(@NonNull CtTypeMember typeMember) {
        if (typeMember.getPosition() == null || !typeMember.getPosition().isValidPosition()) {
            return Integer.MAX_VALUE;
        }
        return typeMember.getPosition().getSourceStart();
    }

    @NonNull
    static String deriveAlphaKey(@NonNull CtTypeMember typeMember) {
        switch (typeMember) {
            case CtMethod<?> method -> {
                String methodName = method.getSimpleName();
                String parameters = deriveParameterTypeList(method.getParameters());
                String returnType =
                        method.getType() == null ? "" : method.getType().getQualifiedName();
                return methodName + "(" + parameters + "):" + returnType;
            }
            case CtConstructor<?> constructor -> {
                String parameters = deriveParameterTypeList(constructor.getParameters());
                return "<init>(" + parameters + ")";
            }
            case CtField<?> field -> {
                String fieldName = field.getSimpleName();
                String fieldType =
                        field.getType() == null ? "" : field.getType().getQualifiedName();
                return fieldName + ":" + fieldType;
            }
            case CtAnonymousExecutable anonymousExecutable -> {
                boolean isStaticInitializer = anonymousExecutable.getModifiers().contains(ModifierKind.STATIC);
                return isStaticInitializer ? "<clinit>" : "<init>";
            }
            case CtRecordComponent recordComponent -> {
                String componentName = recordComponent.getSimpleName();
                String componentType = recordComponent.getType() == null
                        ? ""
                        : recordComponent.getType().getQualifiedName();
                return componentName + ":" + componentType;
            }
            case CtType<?> nestedType -> {
                return nestedType.getQualifiedName();
            }
            default -> {
                // Defensive fallback for any unexpected Spoon CtTypeMember implementation.
                return typeMember.getClass().getSimpleName() + ":" + extractSourceStart(typeMember);
            }
        }
    }

    @NonNull
    private static String deriveParameterTypeList(List<CtParameter<?>> parameters) {
        // Spoon's parameter list is typed, but we keep this helper generic to avoid a large type signature here.
        return parameters.stream()
                .map(parameter -> {
                    CtTypeReference<?> parameterType = parameter.getType();
                    return parameterType.getQualifiedName();
                })
                .collect(Collectors.joining(","));
    }

    @NonNull
    public static Stream<@NonNull CtTypeMember> streamExplicitSourceTypeMembers(@NonNull CtType<?> declaringType) {
        return declaringType.getTypeMembers().stream()
                .filter(typeMember -> typeMember.getPosition() != null
                        && typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard to start processing record implicit fields/components.
                Disabled until the source printer can correctly print record headers/components. */
                .filter(typeMember -> !typeMember.isImplicit());
    }
}
