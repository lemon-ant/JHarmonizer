// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@RequiredParentDerived.Tag
@annotationrequiredparents.RequiredParentDerived.Tag
@RequiredParentOwnerDerived.Inner.Tag
@annotationrequiredparents.RequiredParentOwnerDerived.Inner.Tag
package annotationrequiredparents;

class RequiredAnnotationBase extends requiredparentlibrary.RequiredExternalAnnotationBase {}

class RequiredAnnotationMiddle extends RequiredAnnotationBase {}

class RequiredParentDerived extends RequiredAnnotationMiddle {}

class RequiredParentOwnerBase {
    static class Inner extends RequiredAnnotationMiddle {}
}

class RequiredParentOwnerMiddle extends RequiredParentOwnerBase {}

class RequiredParentOwnerDerived extends RequiredParentOwnerMiddle {}

class RequiredParentFieldOwner extends RequiredAnnotationMiddle {
    @Tag
    int inherited;
}

class RequiredParentFieldConsumer {
    @RequiredParentOwnerDerived.Inner.Tag
    int qualified;
}
