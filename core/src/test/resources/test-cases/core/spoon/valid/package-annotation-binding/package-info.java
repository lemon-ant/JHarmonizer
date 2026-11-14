// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@annotationbindings.PackageAnnotationDerived.Tag
package annotationbindings;

class PackageAnnotationBase {
    @java.lang.annotation.Repeatable(Tags.class)
    @interface Tag {}

    @interface Tags {
        Tag[] value();
    }
}

class PackageAnnotationDerived extends PackageAnnotationBase {}
