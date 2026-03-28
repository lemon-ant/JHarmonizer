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

/**
 * Utility methods for extracting ordering-relevant attributes from Spoon {@code CtTypeMember} instances,
 * such as alpha keys, visibility ranks, and source-position starts.
 */
@UtilityClass
public class SpoonTypeMemberUtils {

    /**
     * Normalizes the type reference by erasure and boxing.
     * @param typeReference the type reference
     * @return the result
     */
    @NonNull
    public static CtTypeReference<?> normalizeTypeReferenceByErasureAndBoxing(
            @NonNull CtTypeReference<?> typeReference) {
        // Use type erasure + boxing to match boolean/Boolean and to ignore generic parameters.
        CtTypeReference<?> erasedTypeReference = typeReference.getTypeErasure();
        return erasedTypeReference.isPrimitive() ? erasedTypeReference.box() : erasedTypeReference;
    }

    /**
     * Performs the derive alpha sorting rank.
     * @param typeMember the type member
     * @return the result
     */
    static int deriveAlphaSortingRank(@NonNull CtTypeMember typeMember) {
        if (typeMember instanceof CtAnonymousExecutable) {
            return 1;
        }
        return 0;
    }

    /**
     * Performs the derive visibility rank.
     * @param typeMember the type member
     * @return the result
     */
    static int deriveVisibilityRank(@NonNull CtTypeMember typeMember) {
        if (typeMember instanceof CtAnonymousExecutable) {
            // Initializer blocks do not declare an explicit visibility modifier;
            // treat them as private for visibility ordering purposes.
            return 3;
        }

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

    /**
     * Performs the extract source start.
     * @param typeMember the type member
     * @return the result
     */
    static int extractSrcStart(@NonNull CtTypeMember typeMember) {
        if (typeMember.getPosition() == null || !typeMember.getPosition().isValidPosition()) {
            return Integer.MAX_VALUE;
        }
        return typeMember.getPosition().getSourceStart();
    }

    /**
     * Performs the derive alpha key.
     * @param typeMember the type member
     * @return the result
     */
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
                return typeMember.getClass().getSimpleName() + ":" + extractSrcStart(typeMember);
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

    /**
     * Streams the explicit source type members.
     * @param declaringType the declaring type
     * @return the stream of explicit source type members
     */
    @NonNull
    public static Stream<@NonNull CtTypeMember> streamExplicitSrcTypeMembers(@NonNull CtType<?> declaringType) {
        return declaringType.getTypeMembers().stream()
                .filter(typeMember -> typeMember.getPosition() != null
                        && typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard to start processing record implicit fields/components.
                Disabled until the source printer can correctly print record headers/components. */
                .filter(typeMember -> !typeMember.isImplicit());
    }
}
