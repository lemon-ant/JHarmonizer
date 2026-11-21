// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@InheritedTreeAnnotationDerived.Tag
@InheritedTreeAnnotationOwner.NestedDerived.Tag
package annotationtreebindings;

class InheritedTreeAnnotationBase {
    @java.lang.annotation.Repeatable(Tags.class)
    @interface Tag {}

    @interface Tags {
        Tag[] value();
    }
}

class InheritedTreeAnnotationDerived extends InheritedTreeAnnotationBase {}

class InheritedTreeAnnotationOwner {
    static class NestedDerived extends InheritedTreeAnnotationBase {}
}
