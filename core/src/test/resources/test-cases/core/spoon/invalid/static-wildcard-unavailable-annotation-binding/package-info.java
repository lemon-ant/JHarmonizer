// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Derived.Inner.Tag
package annotationstaticwildcard;

import static annotationstaticwildcard.StaticWildcardOwner.*;

import java.lang.annotation.Repeatable;

class StaticWildcardAnnotationBase extends earlymatchlibrary.ExternalAnnotationBase {
    @Repeatable(Tags.class)
    @interface Tag {}

    @interface Tags {
        Tag[] value();
    }
}

class StaticWildcardAnnotationMiddle extends StaticWildcardAnnotationBase {}

class StaticWildcardInnerOwnerBase {
    static class Inner extends StaticWildcardAnnotationMiddle {}
}

class StaticWildcardInnerOwnerMiddle extends StaticWildcardInnerOwnerBase {}

class StaticWildcardOwner {
    static class Derived extends StaticWildcardInnerOwnerMiddle {}
}
