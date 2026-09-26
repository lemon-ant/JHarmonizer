// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class AnnotationDeclarationLengthOrdering {

    @LongMarkerName
    @M
    int names;

    @java.lang.Deprecated
    @LongMarkerName
    int qualifiedNames;

    @M()
    @N
    int parentheses;

    @Tag(value = "a")
    @Tag("zz")
    int parameterNames;

    @Tag("longer")
    @Tag("z")
    int values;

    @Tag(/* argument */ "aa")
    @Tag("zz")
    int blockComments;

    @Tag(value /* name */ = {"a", /* value */ "b"} /* trailing */)
    @Tag(value = {"aa", "bb"})
    int arrayComments;

    @Tag(// argument with unmatched ) and @Tag("annotation")
            "aa")
    @Tag("zz")
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
