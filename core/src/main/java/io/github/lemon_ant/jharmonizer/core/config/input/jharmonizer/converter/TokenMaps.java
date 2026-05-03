// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static java.util.Map.entry;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.Map;

/**
 * Centralized token dictionaries (immutable).
 */
final class TokenMaps {

    static final Map<String, MemberAccess> ACCESS_BY_TOKEN = Map.ofEntries(
            entry("public", MemberAccess.PUBLIC),
            entry("protected", MemberAccess.PROTECTED),
            entry("package", MemberAccess.PACKAGE),
            entry("package-private", MemberAccess.PACKAGE),
            entry("private", MemberAccess.PRIVATE));

    static final Map<String, MemberKind> KIND_BY_TOKEN = Map.ofEntries(
            entry("field", MemberKind.FIELD),
            entry("method", MemberKind.METHOD),
            entry("constructor", MemberKind.CONSTRUCTOR),
            entry("init", MemberKind.INIT_BLOCK),
            entry("initializer", MemberKind.INIT_BLOCK),
            entry("class", MemberKind.TYPE_CLASS),
            entry("interface", MemberKind.TYPE_INTERFACE),
            entry("enum", MemberKind.TYPE_ENUM),
            entry("record", MemberKind.TYPE_RECORD),
            entry("annotation", MemberKind.TYPE_ANNOTATION),
            entry("enum-constant", MemberKind.ENUM_CONSTANT),
            entry("record-component", MemberKind.RECORD_COMPONENT));

    static final Map<String, DeclarationModifier> MODIFIER_BY_TOKEN = Map.ofEntries(
            entry("static", DeclarationModifier.STATIC),
            entry("final", DeclarationModifier.FINAL),
            entry("abstract", DeclarationModifier.ABSTRACT),
            entry("synchronized", DeclarationModifier.SYNCHRONIZED),
            entry("native", DeclarationModifier.NATIVE),
            entry("transient", DeclarationModifier.TRANSIENT),
            entry("volatile", DeclarationModifier.VOLATILE),
            entry("strictfp", DeclarationModifier.STRICTFP),
            entry("default", DeclarationModifier.DEFAULT),
            entry("sealed", DeclarationModifier.SEALED),
            entry("non-sealed", DeclarationModifier.NON_SEALED),
            entry("nonsealed", DeclarationModifier.NON_SEALED));

    private TokenMaps() {}
}
