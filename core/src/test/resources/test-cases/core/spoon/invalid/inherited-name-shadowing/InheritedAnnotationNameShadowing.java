// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class InheritedAnnotationNameShadowing extends earlymatchlibrary.ExternalAnnotationBase {
    @Tag("inherited")
    int shortName;

    @java.lang.Deprecated
    int qualifiedName;
}

@interface Tag {}
