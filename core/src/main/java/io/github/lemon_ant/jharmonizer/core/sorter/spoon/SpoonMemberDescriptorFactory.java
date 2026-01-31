package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Spoon-based descriptor factory.
 * Note: mapping rules are intentionally explicit and conservative.
 */
@UtilityClass
class SpoonMemberDescriptorFactory {

    private static final String INIT_NAME = "<init>";
    private static final List<Map.Entry<ModifierKind, MemberAccess>> ACCESS_BY_MODIFIER = List.of(
            Map.entry(ModifierKind.PUBLIC, MemberAccess.PUBLIC),
            Map.entry(ModifierKind.PROTECTED, MemberAccess.PROTECTED),
            Map.entry(ModifierKind.PRIVATE, MemberAccess.PRIVATE));
    private static final Map<ModifierKind, DeclarationModifier> DECLARATION_MODIFIER_BY_SPOON_MODIFIER_KIND = Map.of(
            ModifierKind.STATIC, DeclarationModifier.STATIC,
            ModifierKind.FINAL, DeclarationModifier.FINAL,
            ModifierKind.ABSTRACT, DeclarationModifier.ABSTRACT
            // TODO Research: map ModifierKind.DEFAULT once semantics are confirmed for your model.
            // TODO Map SEALED / NON_SEALED once Spoon exposes them (Java 17+ features).
            );

    @NonNull
    Map<@NonNull CtTypeMember, @NonNull MemberDescriptor> describeMembers(@NonNull CtType<?> type) {
        return describeMembers(type.getTypeMembers());
    }

    private Map<CtTypeMember, MemberDescriptor> describeMembers(List<CtTypeMember> typeMembers) {
        return typeMembers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(), SpoonMemberDescriptorFactory::describeMember));
    }

    static MemberDescriptor describeMember(CtTypeMember typeMember) {
        MemberKind memberKind = resolveMemberKind(typeMember);
        MemberAccess memberAccess = resolveMemberAccessIfApplicable(typeMember);
        Set<DeclarationModifier> declarationModifiers = resolveDeclarationModifiers(typeMember);
        Set<String> annotationQualifiedNames = resolveAnnotationQualifiedNames(typeMember);
        String memberName = resolveMemberName(typeMember);

        return MemberDescriptor.builder()
                .name(memberName)
                .memberKind(memberKind)
                .memberAccess(memberAccess)
                .declarationModifiers(declarationModifiers)
                .annotationQualifiedNames(annotationQualifiedNames)
                .build();
    }

    private static MemberKind resolveMemberKind(CtTypeMember typeMember) {
        return switch (typeMember) {
            case CtField<?> ctField -> MemberKind.FIELD;
            case CtMethod<?> ctMethod -> MemberKind.METHOD;
            case CtConstructor<?> ctConstructor -> MemberKind.CONSTRUCTOR;

            // Initializer blocks (Spoon: CtAnonymousExecutable; static init has ModifierKind.STATIC)
            case CtAnonymousExecutable ctAnonymousExecutable ->
                ctAnonymousExecutable.getModifiers().contains(ModifierKind.STATIC)
                        ? MemberKind.STATIC_INIT_BLOCK
                        : MemberKind.INSTANCE_INIT_BLOCK;

            // Nested types:
            case CtType<?> ctType -> resolveNestedTypeKind(ctType);

            default ->
                throw new IllegalArgumentException(
                        "Unsupported CtTypeMember kind. " + composeDebugExceptionMessage(typeMember));
        };
    }

    private static String composeDebugExceptionMessage(CtTypeMember typeMember) {
        Class<?> runtimeClass = typeMember.getClass();

        String implementedInterfaces =
                Stream.of(runtimeClass.getInterfaces()).map(Class::getName).collect(Collectors.joining(", "));

        String declaringTypeQualifiedName = typeMember.getDeclaringType() == null
                ? "<null>"
                : String.valueOf(typeMember.getDeclaringType().getQualifiedName());

        String positionText =
                typeMember.getPosition().isValidPosition() ? String.valueOf(typeMember.getPosition()) : "<invalid>";

        String modifiersText = String.valueOf(typeMember.getModifiers());

        String simpleNameText = String.valueOf(typeMember.getSimpleName());

        String shortRepresentationText = String.valueOf(typeMember.getShortRepresentation());

        return "context{"
                + "\nruntimeClass=" + runtimeClass.getName()
                + ",\ndirectInterfaces=[" + implementedInterfaces + "]"
                + ",\ndeclaringType=" + declaringTypeQualifiedName
                + ",\nsimpleName=" + simpleNameText
                + ",\nshortRepresentation=" + shortRepresentationText
                + ",\nmodifiers=" + modifiersText
                + ",\nposition=" + positionText
                + "}";
    }

    private static MemberKind resolveNestedTypeKind(CtType<?> nestedType) {
        // In Spoon annotation types may also appear as interfaces, so handle this first.
        if (nestedType.isAnnotationType()) {
            return MemberKind.TYPE_ANNOTATION;
        }

        return switch (nestedType) {
            case CtEnum<?> unusedEnum -> MemberKind.TYPE_ENUM;
            case CtRecord unusedRecord -> MemberKind.TYPE_RECORD;
            case CtClass<?> unusedClass -> MemberKind.TYPE_CLASS;
            case CtInterface<?> unusedInterface -> MemberKind.TYPE_INTERFACE;
            default ->
                throw new IllegalArgumentException("Unsupported nested CtType: "
                        + nestedType.getClass().getName() + ", qualifiedName=" + nestedType.getQualifiedName());
        };
    }

    @Nullable
    private static MemberAccess resolveMemberAccessIfApplicable(CtTypeMember typeMember) {
        // Init blocks are not expected to have access level by MemberDescriptor invariants.
        if (typeMember instanceof CtAnonymousExecutable) {
            return null;
        }

        Set<ModifierKind> modifierKinds = typeMember.getModifiers();
        for (Map.Entry<ModifierKind, MemberAccess> modifierToAccessMapping : ACCESS_BY_MODIFIER) {
            if (modifierKinds.contains(modifierToAccessMapping.getKey())) {
                return modifierToAccessMapping.getValue();
            }
        }

        // No explicit access modifier means package-private.
        return MemberAccess.PACKAGE;
    }

    private static Set<DeclarationModifier> resolveDeclarationModifiers(CtTypeMember typeMember) {
        return typeMember.getModifiers().stream()
                .map(DECLARATION_MODIFIER_BY_SPOON_MODIFIER_KIND::get)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> EnumSet.noneOf(DeclarationModifier.class)), Set::copyOf));
    }

    private static Set<String> resolveAnnotationQualifiedNames(CtTypeMember typeMember) {
        return typeMember.getAnnotations().stream()
                .map(CtAnnotation::getAnnotationType)
                .flatMap(annotationTypeReference ->
                        Stream.of(annotationTypeReference.getQualifiedName(), annotationTypeReference.getSimpleName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    private static String resolveMemberName(CtTypeMember typeMember) {
        String name = trimToNull(typeMember.getSimpleName());
        if (INIT_NAME.equals(name)) {
            return null;
        }
        return name;
    }
}
