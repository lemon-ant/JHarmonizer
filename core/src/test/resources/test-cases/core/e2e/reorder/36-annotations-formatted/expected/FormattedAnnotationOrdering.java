// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@A
@B(value = {"class", "annotation"})
class FormattedAnnotationOrdering {

    @B("zz")
    @B(/* argument */ "aa")
    int repeated;

    @B("aa")
    @B("zz")
    int whitespaceTie;

    @A
    @B(value = {"field", "annotation"})
    int value;

    @A
    @B(value = {"method", "annotation"})
    String execute(@A @B({"parameter", "annotation"}) String input) {
        return input;
    }
}

@interface A {}

@java.lang.annotation.Repeatable(Bs.class)
@interface B {
    String[] value();
}

@interface Bs {
    B[] value();
}
