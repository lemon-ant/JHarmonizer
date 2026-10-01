// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class TypeUseAnnotationOrdering {

    java.util.List<@Z @A String> names;
    String @Z @A [] @D @C [] matrix;

    <@Z @A Element> @Z @A String execute(@Z @A TypeUseAnnotationOrdering this, String value) {
        return value;
    }
}

@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE, java.lang.annotation.ElementType.TYPE_PARAMETER})
@interface A {}

@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
@interface C {}

@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
@interface D {}

@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE, java.lang.annotation.ElementType.TYPE_PARAMETER})
@interface Z {}
