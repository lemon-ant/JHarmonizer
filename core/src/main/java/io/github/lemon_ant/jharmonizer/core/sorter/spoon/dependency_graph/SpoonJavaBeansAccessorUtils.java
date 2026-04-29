package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static java.beans.Introspector.decapitalize;
import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Detects JavaBeans-like accessors in Spoon ({@code get/set/is/has}) and matches them by property name + normalized type
 * (type erasure + boxing). Used for {@code keepAccessorsTogether}.
 */
@UtilityClass
public class SpoonJavaBeansAccessorUtils {

    private static final List<AccessorMethodContract> ACCESSOR_METHOD_CONTRACTS = List.of(
            new AccessorMethodContract("get", AccessorKind.GET, 0, AccessorMethodReturnType.NON_VOID),
            new AccessorMethodContract("set", AccessorKind.SET, 1, AccessorMethodReturnType.VOID),
            new AccessorMethodContract("is", AccessorKind.IS, 0, AccessorMethodReturnType.BOOLEAN),
            new AccessorMethodContract("has", AccessorKind.HAS, 0, AccessorMethodReturnType.BOOLEAN));

    /**
     * Extracts the JavaBeans property name from a method using the full accessor contract
     * (prefix match, return type, and parameter count). Returns empty when the method does
     * not match any recognized accessor contract.
     *
     * @param method the method to inspect
     * @return the decapitalized property name, or empty if the method is not a recognized accessor
     */
    @NonNull
    public static Optional<String> findAccessorPropertyName(@NonNull CtMethod<?> method) {
        return tryParseAccessorMethodDescriptor(method).map(AccessorMethodDescriptor::getPropertyName);
    }

    /**
     * Returns other accessor methods from the same declaring type that target the same property.
     * Empty if the input method is not an accessor or if no matching pair exists in the type.
     * Throws if the type contains a duplicate accessor kind for the same property.
     */
    @NonNull
    static Set<@NonNull CtMethod<?>> findPairedAccessorMethods(@NonNull CtMethod<?> accessorMethod) {
        Optional<AccessorMethodDescriptor> accessorMethodDescriptor = tryParseAccessorMethodDescriptor(accessorMethod);
        if (accessorMethodDescriptor.isEmpty()) {
            return Set.of();
        }

        CtType<?> declaringType = requireNonNull(
                accessorMethod.getDeclaringType(),
                "Expected CtMethod to have a declaring type, but it is detached from the Spoon model. methodName="
                        + accessorMethod.getSimpleName());

        return streamExplicitSrcTypeMembers(declaringType)
                .filter(typeMember -> typeMember instanceof CtMethod<?>)
                .map(typeMember -> (CtMethod<?>) typeMember)
                .filter(candidateMethod -> candidateMethod != accessorMethod)
                .flatMap(
                        candidateMethod -> tryParseAccessorMethodDescriptor(candidateMethod)
                                .map(candidateDescriptor -> Pair.of(candidateMethod, candidateDescriptor))
                                .stream())
                .filter(pair -> isPairedAccessor(accessorMethodDescriptor.get(), pair.getRight()))
                .map(Pair::getLeft)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isPairedAccessor(
            AccessorMethodDescriptor leftDescriptor, AccessorMethodDescriptor rightDescriptor) {
        boolean samePropertyName = leftDescriptor.getPropertyName().equals(rightDescriptor.getPropertyName());
        boolean samePropertyType = leftDescriptor
                .getPropertyType()
                .getQualifiedName()
                .equals(rightDescriptor.getPropertyType().getQualifiedName());

        if (!samePropertyName || !samePropertyType) {
            return false;
        }

        if (leftDescriptor.getAccessorKind() == rightDescriptor.getAccessorKind()) {
            throw new IllegalStateException("Duplicate accessor kind for the same property. propertyName="
                    + leftDescriptor.getPropertyName()
                    + ", propertyType=" + leftDescriptor.getPropertyType().getQualifiedName()
                    + ", accessorKind=" + leftDescriptor.getAccessorKind());
        }

        return true;
    }

    @NonNull
    private static Optional<AccessorMethodDescriptor> tryParseAccessorMethodDescriptor(CtMethod<?> candidateMethod) {
        return ACCESSOR_METHOD_CONTRACTS.stream()
                .filter(accessorMethodContract ->
                        matchesAccessorMethodContract(candidateMethod, accessorMethodContract))
                .map(accessorMethodContract -> tryBuildDescriptor(candidateMethod, accessorMethodContract))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static boolean matchesAccessorMethodContract(
            CtMethod<?> candidateMethod, AccessorMethodContract accessorMethodContract) {
        String candidateMethodName = candidateMethod.getSimpleName();
        String methodNamePrefix = accessorMethodContract.getMethodNamePrefix();

        if (!candidateMethodName.startsWith(methodNamePrefix)) {
            return false;
        }

        if (candidateMethod.getParameters().size() != accessorMethodContract.getExpectedParameterCount()) {
            return false;
        }

        int propertyNameStartIndex = methodNamePrefix.length();

        return candidateMethodName.length() > propertyNameStartIndex
                && Character.isUpperCase(candidateMethodName.charAt(propertyNameStartIndex))
                && matchesReturnTypeContract(candidateMethod, accessorMethodContract.getAccessorMethodReturnType());
    }

    private static boolean matchesReturnTypeContract(
            CtMethod<?> candidateMethod, AccessorMethodReturnType accessorMethodReturnType) {
        CtTypeReference<?> returnTypeReference = candidateMethod.getType();
        if (returnTypeReference == null) {
            return false;
        }

        String returnTypeQualifiedName = returnTypeReference.getQualifiedName();

        return switch (accessorMethodReturnType) {
            case VOID -> "void".equals(returnTypeQualifiedName);
            case NON_VOID -> !"void".equals(returnTypeQualifiedName);
            case BOOLEAN ->
                "java.lang.Boolean"
                        .equals(SpoonTypeMemberUtils.normalizeTypeReferenceByErasureAndBoxing(returnTypeReference)
                                .getQualifiedName());
        };
    }

    @Nullable
    private static AccessorMethodDescriptor tryBuildDescriptor(
            CtMethod<?> candidateMethod, AccessorMethodContract accessorMethodContract) {

        CtTypeReference<?> propertyTypeReference =
                extractPropertyTypeReference(candidateMethod, accessorMethodContract.getAccessorKind());
        if (propertyTypeReference == null) {
            return null;
        }

        CtTypeReference<?> propertyType =
                SpoonTypeMemberUtils.normalizeTypeReferenceByErasureAndBoxing(propertyTypeReference);

        int propertyNameStartIndex =
                accessorMethodContract.getMethodNamePrefix().length();
        String propertyName = candidateMethod.getSimpleName().substring(propertyNameStartIndex);
        String normalizedPropertyName = decapitalize(propertyName);

        return new AccessorMethodDescriptor(
                accessorMethodContract.getAccessorKind(), normalizedPropertyName, propertyType);
    }

    @Nullable
    private static CtTypeReference<?> extractPropertyTypeReference(
            CtMethod<?> candidateMethod, AccessorKind accessorKind) {
        // For getters/is/has the property type is the return type; for setters it is the single parameter type.
        return switch (accessorKind) {
            case GET, IS, HAS -> candidateMethod.getType();
            case SET ->
                candidateMethod.getParameters().isEmpty()
                        ? null
                        : candidateMethod.getParameters().getFirst().getType();
        };
    }

    private enum AccessorKind {
        GET,
        SET,
        IS,
        HAS
    }

    private enum AccessorMethodReturnType {
        VOID,
        NON_VOID,
        BOOLEAN
    }

    @Value
    private static class AccessorMethodContract {

        @NonNull
        String methodNamePrefix;

        @NonNull
        AccessorKind accessorKind;

        int expectedParameterCount;

        @NonNull
        AccessorMethodReturnType accessorMethodReturnType;
    }

    @Value
    private static class AccessorMethodDescriptor {

        @NonNull
        AccessorKind accessorKind;

        @NonNull
        String propertyName;

        @NonNull
        CtTypeReference<?> propertyType;
    }
}
