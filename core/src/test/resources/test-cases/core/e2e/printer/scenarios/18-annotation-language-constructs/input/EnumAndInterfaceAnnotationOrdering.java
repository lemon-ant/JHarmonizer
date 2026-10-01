// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Z @A
enum EnumAndInterfaceAnnotationOrdering {

    @Z @A
    ZEBRA,
    @Z @A
    ALPHA;
}

@Z @A
interface InterfaceAnnotationOrdering {

    @Z @A
    String resolve(@Z @A String value);
}

@interface A {}

@interface Z {}
