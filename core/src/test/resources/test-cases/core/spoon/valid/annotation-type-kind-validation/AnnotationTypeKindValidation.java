// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package annotationtypekindvalidation;

class AnnotationTypeKindValidation {
    static class SelfNameBase {
        @interface SelfNameDerived {}
    }

    static class SelfNameDerived extends SelfNameBase {
        @SelfNameDerived
        int selfName;
    }

    static class QualifiedNameBase {
        static class QualifiedNameDerived {
            @interface Tag {}
        }
    }

    static class QualifiedNameDerived extends QualifiedNameBase {
        static class Tag {}

        @QualifiedNameDerived.Tag
        int qualifiedName;
    }

    static class LocalBase {
        @interface Tag {}
    }

    static class LocalOwner {
        static class Tag {}

        void useLocal() {
            class Local extends LocalBase {
                @Tag
                int local;
            }
        }
    }

    static class AnonymousBase {
        @interface Tag {}
    }

    static class AnonymousOwner {
        static class Tag {}

        AnonymousBase anonymous = new AnonymousBase() {
            @Tag
            int anonymous;
        };
    }
}
