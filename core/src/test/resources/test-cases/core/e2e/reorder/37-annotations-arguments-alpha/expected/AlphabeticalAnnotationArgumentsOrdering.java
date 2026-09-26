// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Marker
@LongName("aa")
@Short("zz")
class AlphabeticalAnnotationArgumentsOrdering {
    static final String AA = "zz";
    static final String ZZ = "aa";

    @Tag(
        value = /* kept */ "aa")
    @Tag(value = "zz")
    int namedValues;

    @Tag(AlphabeticalAnnotationArgumentsOrdering.AA)
    @Tag(AlphabeticalAnnotationArgumentsOrdering.ZZ)
    int constants;

    @Pair(alpha = "zz", zeta = "aa")
    @Pair(zeta = "aa", alpha = "zz")
    int parameterNames;

    @Tag("a ")
    @Tag("aa")
    int literalWhitespace;

    @Tag("same")
    @Tag( /* stays */ "same")
    int equalArguments;

    @Tag
    @Tag()
    @Tag("")
    int emptyArguments;

    @Tag
    @Tag( /* empty */ )
    @Tag()
    int commentOnlyArguments;

    @Marker
    @Tag()
    int missingArguments;

    @Groups({@Tag("a"), @Tag("z")})
    @Groups({@Tag("z"), @Tag("a")})
    int nestedArrays;

    @Tag("a" + ("a"))
    @Tag("z" + ("z"))
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
