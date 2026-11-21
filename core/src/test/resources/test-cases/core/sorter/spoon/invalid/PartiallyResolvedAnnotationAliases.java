// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import java.lang.annotation.Repeatable;

class PartiallyResolvedAnnotationAliases {

    @Zed
    @FirstOwner.Tag("z")
    @SecondOwner.Tag("a")
    @unavailable.Owner.Tag("0")
    @Able
    int ambiguousThirdOwner;

    @Zed
    @FirstOwner.Tags
    @MissingOwner.Tag("direct")
    @Able
    int defaultedContainerFirst;

    static class FirstOwner {

        @Repeatable(Tags.class)
        @interface Tag {
            String value();
        }

        @interface Tags {
            Tag[] value() default {@Tag("default")};
        }

        static class Consumer extends unavailable.Base {

            @Zed
            @Tags
            @Tag("a")
            @Able
            int lexicalContainerFirst;

            @Zed
            @Tag("z")
            @Tags
            @Able
            int lexicalContainerLast;
        }
    }

    static class SecondOwner {

        @interface Tag {
            String value();
        }
    }

    static class MissingOwner extends unavailable.Base {}

    static class ExternalContainerOwner {

        @Repeatable(unavailable.containers.Stamps.class)
        @interface Stamp {
            String value();
        }

        static class Consumer extends unavailable.Base {

            @Zed
            @Stamps
            @Stamp("a")
            @Able
            int lexicalExternalContainerFirst;
        }
    }

    @interface Able {}

    @interface Zed {}
}
