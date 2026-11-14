// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package annotationmemberbranches;

interface MemberDiamondRoot {
    @interface Tag {}
}

interface MemberDiamondLeft extends MemberDiamondRoot {}

interface MemberDiamondRight extends MemberDiamondRoot {}

class MemberDiamondOwner implements MemberDiamondLeft, MemberDiamondRight {}

class MemberHidingAncestor {
    static class Inner {
        @interface Tag {}
    }
}

class MemberHidingParent extends MemberHidingAncestor {
    private static class Inner {}
}

interface MemberVisibleBranch {
    class Inner {
        @interface Tag {}
    }
}

class MemberHidingOwner extends MemberHidingParent implements MemberVisibleBranch {}

interface FirstInheritedAnnotationOwner {
    @interface Tag {}
}

interface SecondInheritedAnnotationOwner {
    @interface Tag {}
}

class AmbiguousInheritedAnnotationOwner implements FirstInheritedAnnotationOwner, SecondInheritedAnnotationOwner {}

class InheritedMemberTypeBranches {
    @MemberDiamondOwner.Tag
    int diamond;

    @MemberHidingOwner.Inner.Tag
    int hiddenAncestor;
}
