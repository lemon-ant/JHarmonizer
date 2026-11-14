// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Derived.Inner.Tag
package annotationordinarywildcard;

import annotationordinarywildcard.WildcardOwner.*;
import java.lang.annotation.Repeatable;

class WildcardAnnotationBase extends earlymatchlibrary.ExternalAnnotationBase {
    @Repeatable(Tags.class)
    @interface Tag {}

    @interface Tags {
        Tag[] value();
    }
}

class WildcardAnnotationMiddle extends WildcardAnnotationBase {}

class WildcardInnerOwnerBase {
    static class Inner extends WildcardAnnotationMiddle {}
}

class WildcardInnerOwnerMiddle extends WildcardInnerOwnerBase {}

class WildcardOwner {
    static class Derived extends WildcardInnerOwnerMiddle {}
}
