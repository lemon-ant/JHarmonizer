// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@B(value = {"class", "annotation"}) @A
class FormattedAnnotationOrdering {
@B(/* argument */ "aa") @B( "zz" ) int repeated;
@B(             "aa"             ) @B("zz") int whitespaceTie;
@B(
    value = {"field", "annotation"}) @A int value;
@B(value = {"method", "annotation"}) @A
String execute(@B({"parameter", "annotation"}) @A String input){return input;}
}
@interface A{}
@java.lang.annotation.Repeatable(Bs.class)
@interface B{String[] value();}
@interface Bs{B[] value();}
