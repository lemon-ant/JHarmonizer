// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@InheritedAnnotationOwnerDerived.Inner.Tag
package annotationtreeinheritedowner;

class InheritedAnnotationOwnerBase {
    static class Inner {
        @interface Tag {}
    }
}

class InheritedAnnotationOwnerDerived extends InheritedAnnotationOwnerBase {}
