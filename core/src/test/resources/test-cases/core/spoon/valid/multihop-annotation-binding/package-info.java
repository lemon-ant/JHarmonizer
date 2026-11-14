// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@MultiHopOwnerDerived.Inner.Tag
@annotationmultihop.MultiHopOwnerDerived.Inner.Tag
@Derived.Inner.Tag
package annotationmultihop;

import annotationmultihop.MultiHopImportOwner.*;

class MultiHopAnnotationBase {
    @java.lang.annotation.Target({
        java.lang.annotation.ElementType.PACKAGE,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.TYPE_USE
    })
    @java.lang.annotation.Repeatable(Tags.class)
    @interface Tag {}

    @java.lang.annotation.Target({
        java.lang.annotation.ElementType.PACKAGE,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.TYPE_USE
    })
    @interface Tags {
        Tag[] value();
    }
}

class MultiHopAnnotationMiddle extends MultiHopAnnotationBase {}

class MultiHopOwnerBase {
    static class Inner extends MultiHopAnnotationMiddle {}
}

class MultiHopOwnerMiddle extends MultiHopOwnerBase {}

class MultiHopOwnerDerived extends MultiHopOwnerMiddle {}

class MultiHopImportOwner {
    static class Derived extends MultiHopOwnerMiddle {}
}

class MultiHopEnclosingMiddle extends MultiHopAnnotationMiddle {}

class MultiHopEnclosingDerived extends MultiHopEnclosingMiddle {
    @Tag
    int value;
}

class MultiHopArrayConsumer {
    String @MultiHopOwnerDerived.Inner.Tag [] values;
}
