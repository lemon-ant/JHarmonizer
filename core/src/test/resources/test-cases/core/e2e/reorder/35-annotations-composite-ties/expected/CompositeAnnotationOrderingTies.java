// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@A("yz")
@B("zy")
@Bb("x")
@CC("w")
@Tag("aa")
@Tag("zz")
@Container({@Tag("z"), @Tag("a")})
class CompositeAnnotationOrderingTies {}

@interface A {
    String value();
}

@interface B {
    String value();
}

@interface Bb {
    String value();
}

@interface CC {
    String value();
}

@java.lang.annotation.Repeatable(Tags.class)
@interface Tag {
    String value();
}

@interface Tags {
    Tag[] value();
}

@interface Container {
    Tag[] value();
}
