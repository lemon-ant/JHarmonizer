// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class AnnotationDeclarationLengthOrdering {

    @M
    @LongMarkerName
    int names;

    @LongMarkerName
    @java.lang.Deprecated
    int qualifiedNames;

    @N
    @M()
    int parentheses;

    @Tag("zz")
    @Tag(value = "a")
    int parameterNames;

    @Tag("z")
    @Tag("longer")
    int values;

    @Tag("zz")
    @Tag(/* argument */ "aa")
    int blockComments;

    @Tag(value = {"aa", "bb"})
    @Tag(value /* name */ = {"a", /* value */ "b"} /* trailing */)
    int arrayComments;

    @Tag("zz")
    @Tag(// argument with unmatched ) and @Tag("annotation")
            "aa")
    int lineComments;

    @Tag("a") /* Trailing comments do not contribute to declaration length. */
    @Tag("zz")
    int externalComments;
}

@interface LongMarkerName {}

@interface M {}

@interface N {}

@java.lang.annotation.Repeatable(Tags.class)
@interface Tag {
    String[] value();
}

@interface Tags {
    Tag[] value();
}
