// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Comparator;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Renders compact, single-line declaration headers for Spoon type members.
 *
 * <p>Used to produce human-readable diagnostic snippets that show how a member declaration
 * begins, without its body, initializer, or parameter details.
 */
@UtilityClass
class DeclarationHeaderRenderer {

    private static final String BODY_PLACEHOLDER = " { ... }";
    private static final String EMPTY_PARAMS = "()";
    private static final String VARARGS_PARAMS = "(...)";
    private static final String STATIC_INITIALIZER_HEADER = "static { ... }";
    private static final String INSTANCE_INITIALIZER_HEADER = "{ ... }";

    /**
     * Renders a compact declaration header for a type member, omitting bodies and initializers.
     *
     * <p>Format by member kind:
     * <ul>
     *   <li>Field: {@code [modifiers] [type] [name]}</li>
     *   <li>Method: {@code [modifiers] [returnType] [name]() { ... }} or {@code (...) { ... }} when parameters are present</li>
     *   <li>Constructor: {@code [modifiers] [name]() { ... }} or {@code (...) { ... }} when parameters are present</li>
     *   <li>Nested type: {@code [modifiers] class|interface|enum|@interface [name] { ... }}</li>
     *   <li>Enum value: {@code [name]}</li>
     *   <li>Initializer block: {@code { ... }} or {@code static { ... }}</li>
     * </ul>
     *
     * @param element the element to render
     * @return a compact single-line declaration header
     */
    @NonNull
    static String renderDeclarationHeader(@NonNull CtElement element) {
        if (!(element instanceof CtTypeMember member)) {
            return "<nameless>";
        }
        if (member instanceof CtAnonymousExecutable initializerBlock) {
            return renderInitializerHeader(initializerBlock);
        }
        if (member instanceof CtEnumValue<?> enumValue) {
            return enumValue.getSimpleName();
        }
        String modifiers = renderModifiers(member);
        if (member instanceof CtField<?> field) {
            return joinNonBlank(modifiers, field.getType().getSimpleName(), field.getSimpleName());
        }
        if (member instanceof CtMethod<?> method) {
            return renderMethodHeader(modifiers, method);
        }
        if (member instanceof CtConstructor<?> constructor) {
            return renderConstructorHeader(modifiers, constructor);
        }
        if (member instanceof CtType<?> nestedType) {
            return joinNonBlank(modifiers, resolveTypeKeyword(nestedType), nestedType.getSimpleName())
                    + BODY_PLACEHOLDER;
        }
        return isBlank(member.getSimpleName()) ? "<initializer>" : member.getSimpleName();
    }

    @NonNull
    private static String renderInitializerHeader(CtAnonymousExecutable initializerBlock) {
        return initializerBlock.getModifiers().contains(ModifierKind.STATIC)
                ? STATIC_INITIALIZER_HEADER
                : INSTANCE_INITIALIZER_HEADER;
    }

    @NonNull
    private static String renderMethodHeader(@NonNull String modifiers, @NonNull CtMethod<?> method) {
        String params = method.getParameters().isEmpty() ? EMPTY_PARAMS : VARARGS_PARAMS;
        return joinNonBlank(modifiers, method.getType().getSimpleName(), method.getSimpleName() + params)
                + BODY_PLACEHOLDER;
    }

    @NonNull
    private static String renderConstructorHeader(@NonNull String modifiers, @NonNull CtConstructor<?> constructor) {
        String params = constructor.getParameters().isEmpty() ? EMPTY_PARAMS : VARARGS_PARAMS;
        return joinNonBlank(modifiers, constructor.getSimpleName() + params) + BODY_PLACEHOLDER;
    }

    @NonNull
    private static String renderModifiers(@NonNull CtTypeMember member) {
        return member.getModifiers().stream()
                .sorted(Comparator.comparingInt(ModifierKind::ordinal))
                .map(ModifierKind::toString)
                .collect(joining(" "));
    }

    @NonNull
    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(part -> !isBlank(part)).collect(joining(" "));
    }

    @NonNull
    private static String resolveTypeKeyword(@NonNull CtType<?> type) {
        if (type instanceof CtAnnotationType<?>) {
            return "@interface";
        }
        if (type instanceof CtEnum<?>) {
            return "enum";
        }
        if (type instanceof CtInterface<?>) {
            return "interface";
        }
        return "class";
    }
}
