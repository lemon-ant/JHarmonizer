// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Z @A
record RecordComponentAnnotationOrdering(@Z @A String value) {

    @Z @A
    public String value() {
        return value;
    }
}

@java.lang.annotation.Target({
        java.lang.annotation.ElementType.TYPE,
        java.lang.annotation.ElementType.RECORD_COMPONENT,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.METHOD,
        java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.TYPE_USE})
@interface A {}

@java.lang.annotation.Target({
        java.lang.annotation.ElementType.TYPE,
        java.lang.annotation.ElementType.RECORD_COMPONENT,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.METHOD,
        java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.TYPE_USE})
@interface Z {}
