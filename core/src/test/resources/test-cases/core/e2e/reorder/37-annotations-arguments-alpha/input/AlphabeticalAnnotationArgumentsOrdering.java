// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Short("zz")
@LongName("aa")
@Marker
class AlphabeticalAnnotationArgumentsOrdering {
    static final String AA = "zz";
    static final String ZZ = "aa";

    @Tag(value = "zz")
    @Tag(
        value = /* kept */ "aa")
    int namedValues;

    @Tag(AlphabeticalAnnotationArgumentsOrdering.ZZ)
    @Tag(AlphabeticalAnnotationArgumentsOrdering.AA)
    int constants;

    @Pair(zeta = "aa", alpha = "zz")
    @Pair(alpha = "zz", zeta = "aa")
    int parameterNames;

    @Tag("aa")
    @Tag("a ")
    int literalWhitespace;

    @Tag("same")
    @Tag( /* stays */ "same")
    int equalArguments;

    @Tag("")
    @Tag()
    @Tag
    int emptyArguments;

    @Tag( /* empty */ )
    @Tag
    @Tag()
    int commentOnlyArguments;

    @Tag()
    @Marker
    int missingArguments;

    @Groups({@Tag("z"), @Tag("a")})
    @Groups({@Tag("a"), @Tag("z")})
    int nestedArrays;

    @Tag("z" + ("z"))
    @Tag("a" + ("a"))
    int expressions;
}

@interface Short {
    String value();
}

@interface LongName {
    String value();
}

@interface Marker {}

@java.lang.annotation.Repeatable(Tags.class)
@interface Tag {
    String value() default "";
}

@interface Tags {
    Tag[] value();
}

@java.lang.annotation.Repeatable(Pairs.class)
@interface Pair {
    String alpha();
    String zeta();
}

@interface Pairs {
    Pair[] value();
}

@java.lang.annotation.Repeatable(GroupList.class)
@interface Groups {
    Tag[] value();
}

@interface GroupList {
    Groups[] value();
}
