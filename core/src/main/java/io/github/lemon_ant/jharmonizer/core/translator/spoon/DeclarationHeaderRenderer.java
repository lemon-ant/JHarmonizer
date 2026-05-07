// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.github.lemon_ant.jharmonizer.core.diff.WhitespaceVisualizationStyle;
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
    private static final String EMPTY_PARAMS = "()";

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
     * @param style the visualization style used to render body and parameter placeholders
     * @return a compact single-line declaration header
     */
    @NonNull
    static String renderDeclarationHeader(@NonNull CtElement element, @NonNull WhitespaceVisualizationStyle style) {
        if (!(element instanceof CtTypeMember member)) {
            return "<nameless>";
        }
        if (member instanceof CtAnonymousExecutable initializerBlock) {
            return renderInitializerHeader(initializerBlock, style);
        }
        if (member instanceof CtEnumValue<?> enumValue) {
            return enumValue.getSimpleName();
        }
        String modifiers = renderModifiers(member);
        if (member instanceof CtField<?> field) {
            return joinNonBlank(modifiers, field.getType().getSimpleName(), field.getSimpleName());
        }
        if (member instanceof CtMethod<?> method) {
            return renderMethodHeader(modifiers, method, style);
        }
        if (member instanceof CtConstructor<?> constructor) {
            return renderConstructorHeader(modifiers, constructor, style);
        }
        if (member instanceof CtType<?> nestedType) {
            return joinNonBlank(modifiers, resolveTypeKeyword(nestedType), nestedType.getSimpleName())
                    + buildBody(style);
        }
        return isBlank(member.getSimpleName()) ? "<initializer>" : member.getSimpleName();
    }

    @NonNull
    private static String buildBody(WhitespaceVisualizationStyle style) {
        return " { " + style.getEllipsisMark() + " }";
    }

    @NonNull
    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(part -> !isBlank(part)).collect(joining(" "));
    }

    @NonNull
    private static String renderConstructorHeader(
            String modifiers, CtConstructor<?> constructor, WhitespaceVisualizationStyle style) {
        String params = constructor.getParameters().isEmpty() ? EMPTY_PARAMS : "(" + style.getEllipsisMark() + ")";
        String name = constructor.getDeclaringType().getSimpleName();
        return joinNonBlank(modifiers, name + params) + buildBody(style);
    }

    @NonNull
    private static String renderInitializerHeader(
            CtAnonymousExecutable initializerBlock, WhitespaceVisualizationStyle style) {
        // buildBody() returns " { ... }" with a leading space; strip it for the instance case
        // so the result is "{ ... }" rather than " { ... }". The static case prepends "static" which
        // consumes the leading space naturally: "static { ... }".
        String body = buildBody(style);
        return initializerBlock.getModifiers().contains(ModifierKind.STATIC) ? "static" + body : body.stripLeading();
    }

    @NonNull
    private static String renderMethodHeader(String modifiers, CtMethod<?> method, WhitespaceVisualizationStyle style) {
        String params = method.getParameters().isEmpty() ? EMPTY_PARAMS : "(" + style.getEllipsisMark() + ")";
        return joinNonBlank(modifiers, method.getType().getSimpleName(), method.getSimpleName() + params)
                + buildBody(style);
    }

    @NonNull
    private static String renderModifiers(CtTypeMember member) {
        return member.getModifiers().stream()
                .sorted(Comparator.comparingInt(ModifierKind::ordinal))
                .map(ModifierKind::toString)
                .collect(joining(" "));
    }

    @NonNull
    private static String resolveTypeKeyword(CtType<?> type) {
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
