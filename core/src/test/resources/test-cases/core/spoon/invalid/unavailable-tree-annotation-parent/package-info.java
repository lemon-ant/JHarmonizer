// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@UnavailableTreeAnnotationDerived.Tag
@UnavailableTreeAnnotationOwner.Derived.Tag
@UnavailableTreeAnnotationOwner.Direct.Tag
package annotationtreeunavailable;

class UnavailableTreeAnnotationBase extends earlymatchlibrary.ExternalAnnotationBase {
    @interface Tag {}
}

class UnavailableTreeAnnotationDerived extends UnavailableTreeAnnotationBase {}

class UnavailableTreeAnnotationOwner {
    static class Base extends earlymatchlibrary.ExternalAnnotationBase {
        @interface Tag {}
    }

    static class Derived extends Base {}

    static class Direct extends earlymatchlibrary.ExternalAnnotationBase {
        @interface Tag {}
    }
}
