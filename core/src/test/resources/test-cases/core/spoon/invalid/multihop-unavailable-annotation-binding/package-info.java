// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@UnavailableChainDerived.Tag
@annotationchainunavailable.UnavailableChainDerived.Tag
@UnavailableChainOwnerDerived.Inner.Tag
@annotationchainunavailable.UnavailableChainOwnerDerived.Inner.Tag
package annotationchainunavailable;

import java.lang.annotation.Repeatable;

class UnavailableChainAnnotationBase extends earlymatchlibrary.ExternalAnnotationBase {
    @Repeatable(Tags.class)
    @interface Tag {}

    @interface Tags {
        Tag[] value();
    }
}

class UnavailableChainAnnotationMiddle extends UnavailableChainAnnotationBase {}

class UnavailableChainDerived extends UnavailableChainAnnotationMiddle {}

class UnavailableChainOwnerBase extends earlymatchlibrary.ExternalAnnotationBase {
    static class Inner extends UnavailableChainAnnotationMiddle {}
}

class UnavailableChainOwnerMiddle extends UnavailableChainOwnerBase {}

class UnavailableChainOwnerDerived extends UnavailableChainOwnerMiddle {}

class UnavailableChainEnclosingDerived extends UnavailableChainAnnotationMiddle {
    @Tag
    int inherited;
}

class UnavailableChainConsumer {
    @UnavailableChainOwnerDerived.Inner.Tag
    int qualified;
}
