// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@A @Z
enum EnumAndInterfaceAnnotationOrdering {

    @A @Z
    ZEBRA,
    @A @Z
    ALPHA;
}

@A @Z
interface InterfaceAnnotationOrdering {

    @A @Z
    String resolve(@A @Z String value);
}

@interface A {}

@interface Z {}
