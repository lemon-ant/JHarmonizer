// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationaliasmatrix;

import static annotationaliasmatrix.OwnerBase.Inner.*;
import static annotationaliasmatrix.OwnerBase.Inner.Tag;

import annotationaliasmatrix.TagOwnerBase.*;
import annotationaliasmatrix.TagOwnerBase.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

public class RepeatableAnnotationAliasMatrix {

    // Ordered pairs use all eight spellings, including the same spelling twice:
    // 0 Tag; 1 TagOwnerBase.Tag; 2 TagOwnerDerived.Tag; 3 OwnerDerived.Inner.Tag;
    // 4 annotationaliasmatrix.TagOwnerBase.Tag; 5 annotationaliasmatrix.TagOwnerDerived.Tag;
    // 6 annotationaliasmatrix.OwnerDerived.Inner.Tag; 7 the Unicode-escaped simple name.

    // Declaration pairs starting with spelling 0.
    @Tag("z") @Tag("a") int fieldPair0_0;
    @Tag("z") @TagOwnerBase.Tag("a") int fieldPair0_1;
    @Tag("z") @TagOwnerDerived.Tag("a") int fieldPair0_2;
    @Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair0_3;
    @Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair0_4;
    @Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair0_5;
    @Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair0_6;
    @Tag("z") @T\u0061g("a") int fieldPair0_7;

    // Declaration pairs starting with spelling 1.
    @TagOwnerBase.Tag("z") @Tag("a") int fieldPair1_0;
    @TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") int fieldPair1_1;
    @TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair1_2;
    @TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair1_3;
    @TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair1_4;
    @TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair1_5;
    @TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair1_6;
    @TagOwnerBase.Tag("z") @T\u0061g("a") int fieldPair1_7;

    // Declaration pairs starting with spelling 2.
    @TagOwnerDerived.Tag("z") @Tag("a") int fieldPair2_0;
    @TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") int fieldPair2_1;
    @TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair2_2;
    @TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair2_3;
    @TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair2_4;
    @TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair2_5;
    @TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair2_6;
    @TagOwnerDerived.Tag("z") @T\u0061g("a") int fieldPair2_7;

    // Declaration pairs starting with spelling 3.
    @OwnerDerived.Inner.Tag("z") @Tag("a") int fieldPair3_0;
    @OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") int fieldPair3_1;
    @OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair3_2;
    @OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair3_3;
    @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair3_4;
    @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair3_5;
    @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair3_6;
    @OwnerDerived.Inner.Tag("z") @T\u0061g("a") int fieldPair3_7;

    // Declaration pairs starting with spelling 4.
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @Tag("a") int fieldPair4_0;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") int fieldPair4_1;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair4_2;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair4_3;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair4_4;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair4_5;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair4_6;
    @annotationaliasmatrix.TagOwnerBase.Tag("z") @T\u0061g("a") int fieldPair4_7;

    // Declaration pairs starting with spelling 5.
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @Tag("a") int fieldPair5_0;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") int fieldPair5_1;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair5_2;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair5_3;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair5_4;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair5_5;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair5_6;
    @annotationaliasmatrix.TagOwnerDerived.Tag("z") @T\u0061g("a") int fieldPair5_7;

    // Declaration pairs starting with spelling 6.
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @Tag("a") int fieldPair6_0;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") int fieldPair6_1;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") int fieldPair6_2;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") int fieldPair6_3;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair6_4;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair6_5;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair6_6;
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @T\u0061g("a") int fieldPair6_7;

    // Declaration pairs starting with spelling 7.
    @T\u0061g("z") @Tag("a") int fieldPair7_0;
    @T\u0061g("z") @TagOwnerBase.Tag("a") int fieldPair7_1;
    @T\u0061g("z") @TagOwnerDerived.Tag("a") int fieldPair7_2;
    @T\u0061g("z") @OwnerDerived.Inner.Tag("a") int fieldPair7_3;
    @T\u0061g("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") int fieldPair7_4;
    @T\u0061g("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") int fieldPair7_5;
    @T\u0061g("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") int fieldPair7_6;
    @T\u0061g("z") @T\u0061g("a") int fieldPair7_7;

    // Array-dimension pairs starting with spelling 0.
    String @Tag("z") @Tag("a") [] arrayPair0_0;
    String @Tag("z") @TagOwnerBase.Tag("a") [] arrayPair0_1;
    String @Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair0_2;
    String @Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair0_3;
    String @Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair0_4;
    String @Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair0_5;
    String @Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair0_6;
    String @Tag("z") @T\u0061g("a") [] arrayPair0_7;

    // Array-dimension pairs starting with spelling 1.
    String @TagOwnerBase.Tag("z") @Tag("a") [] arrayPair1_0;
    String @TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair1_1;
    String @TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair1_2;
    String @TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair1_3;
    String @TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair1_4;
    String @TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair1_5;
    String @TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair1_6;
    String @TagOwnerBase.Tag("z") @T\u0061g("a") [] arrayPair1_7;

    // Array-dimension pairs starting with spelling 2.
    String @TagOwnerDerived.Tag("z") @Tag("a") [] arrayPair2_0;
    String @TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair2_1;
    String @TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair2_2;
    String @TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair2_3;
    String @TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair2_4;
    String @TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair2_5;
    String @TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair2_6;
    String @TagOwnerDerived.Tag("z") @T\u0061g("a") [] arrayPair2_7;

    // Array-dimension pairs starting with spelling 3.
    String @OwnerDerived.Inner.Tag("z") @Tag("a") [] arrayPair3_0;
    String @OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair3_1;
    String @OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair3_2;
    String @OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair3_3;
    String @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair3_4;
    String @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair3_5;
    String @OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair3_6;
    String @OwnerDerived.Inner.Tag("z") @T\u0061g("a") [] arrayPair3_7;

    // Array-dimension pairs starting with spelling 4.
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @Tag("a") [] arrayPair4_0;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair4_1;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair4_2;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair4_3;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair4_4;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair4_5;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair4_6;
    String @annotationaliasmatrix.TagOwnerBase.Tag("z") @T\u0061g("a") [] arrayPair4_7;

    // Array-dimension pairs starting with spelling 5.
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @Tag("a") [] arrayPair5_0;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair5_1;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair5_2;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair5_3;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair5_4;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair5_5;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair5_6;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("z") @T\u0061g("a") [] arrayPair5_7;

    // Array-dimension pairs starting with spelling 6.
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @Tag("a") [] arrayPair6_0;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") [] arrayPair6_1;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") [] arrayPair6_2;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") [] arrayPair6_3;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair6_4;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair6_5;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair6_6;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @T\u0061g("a") [] arrayPair6_7;

    // Array-dimension pairs starting with spelling 7.
    String @T\u0061g("z") @Tag("a") [] arrayPair7_0;
    String @T\u0061g("z") @TagOwnerBase.Tag("a") [] arrayPair7_1;
    String @T\u0061g("z") @TagOwnerDerived.Tag("a") [] arrayPair7_2;
    String @T\u0061g("z") @OwnerDerived.Inner.Tag("a") [] arrayPair7_3;
    String @T\u0061g("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") [] arrayPair7_4;
    String @T\u0061g("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") [] arrayPair7_5;
    String @T\u0061g("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") [] arrayPair7_6;
    String @T\u0061g("z") @T\u0061g("a") [] arrayPair7_7;

    // Receiver pairs starting with spelling 0.
    void receiverPair0_0(@Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_1(@Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_2(@Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_3(@Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_4(@Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_5(@Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_6(@Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair0_7(@Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 1.
    void receiverPair1_0(@TagOwnerBase.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_1(@TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_2(@TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_3(@TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_4(@TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_5(@TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_6(@TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair1_7(@TagOwnerBase.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 2.
    void receiverPair2_0(@TagOwnerDerived.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_1(@TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_2(@TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_3(@TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_4(@TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_5(@TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_6(@TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair2_7(@TagOwnerDerived.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 3.
    void receiverPair3_0(@OwnerDerived.Inner.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_1(@OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_2(@OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_3(@OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_4(@OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_5(@OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_6(@OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair3_7(@OwnerDerived.Inner.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 4.
    void receiverPair4_0(@annotationaliasmatrix.TagOwnerBase.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_1(@annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_2(@annotationaliasmatrix.TagOwnerBase.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_3(@annotationaliasmatrix.TagOwnerBase.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_4(@annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_5(@annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_6(@annotationaliasmatrix.TagOwnerBase.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair4_7(@annotationaliasmatrix.TagOwnerBase.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 5.
    void receiverPair5_0(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_1(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_2(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_3(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_4(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_5(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_6(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair5_7(@annotationaliasmatrix.TagOwnerDerived.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 6.
    void receiverPair6_0(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_1(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_2(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_3(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_4(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_5(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_6(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair6_7(@annotationaliasmatrix.OwnerDerived.Inner.Tag("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    // Receiver pairs starting with spelling 7.
    void receiverPair7_0(@T\u0061g("z") @Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_1(@T\u0061g("z") @TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_2(@T\u0061g("z") @TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_3(@T\u0061g("z") @OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_4(@T\u0061g("z") @annotationaliasmatrix.TagOwnerBase.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_5(@T\u0061g("z") @annotationaliasmatrix.TagOwnerDerived.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_6(@T\u0061g("z") @annotationaliasmatrix.OwnerDerived.Inner.Tag("a") RepeatableAnnotationAliasMatrix this) {}
    void receiverPair7_7(@T\u0061g("z") @T\u0061g("a") RepeatableAnnotationAliasMatrix this) {}

    @Able @Zed
    @Tag("7")
    @TagOwnerBase.Tag("6")
    @TagOwnerDerived.Tag("5")
    @OwnerDerived.Inner.Tag("4")
    @annotationaliasmatrix.TagOwnerBase.Tag("3")
    @annotationaliasmatrix.TagOwnerDerived.Tag("2")
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("1")
    @T\u0061g("0")
    int allDeclarationSpellings;

    String @Able @Zed @Tag("7") @TagOwnerBase.Tag("6") @TagOwnerDerived.Tag("5") @OwnerDerived.Inner.Tag("4") @annotationaliasmatrix.TagOwnerBase.Tag("3") @annotationaliasmatrix.TagOwnerDerived.Tag("2") @annotationaliasmatrix.OwnerDerived.Inner.Tag("1") @T\u0061g("0") [] allArraySpellings;

    void allReceiverSpellings(@Able @Zed @Tag("7") @TagOwnerBase.Tag("6") @TagOwnerDerived.Tag("5") @OwnerDerived.Inner.Tag("4") @annotationaliasmatrix.TagOwnerBase.Tag("3") @annotationaliasmatrix.TagOwnerDerived.Tag("2") @annotationaliasmatrix.OwnerDerived.Inner.Tag("1") @T\u0061g("0") RepeatableAnnotationAliasMatrix this) {}

    @OwnerDerived /* inherited owner */ . Inner . Tag("comment-z")
    @annotationaliasmatrix . TagOwnerBase . Tag("comment-a")
    int commentedQualification;

    @Tags
    @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct")
    int defaultContainerFirst;

    @annotationaliasmatrix.TagOwnerDerived.Tag("direct")
    @Tags
    int defaultContainerLast;

    @Tags({@Tag("inner-z"), @annotationaliasmatrix.TagOwnerBase.Tag("inner-a")})
    @OwnerDerived.Inner.Tag("direct")
    int explicitContainerFirst;

    @OwnerDerived.Inner.Tag("direct")
    @annotationaliasmatrix.TagOwnerDerived.Tags({@Tag("inner-z"), @TagOwnerBase.Tag("inner-a")})
    int explicitContainerLast;

    String @Tags @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") [] arrayDefaultContainerFirst;
    String @annotationaliasmatrix.TagOwnerDerived.Tag("direct") @Tags [] arrayDefaultContainerLast;
    String @annotationaliasmatrix.TagOwnerDerived.Tags({@Tag("inner-z"), @TagOwnerBase.Tag("inner-a")}) @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") [] arrayExplicitContainerFirst;
    String @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") @Tags({@Tag("inner-z"), @TagOwnerBase.Tag("inner-a")}) [] arrayExplicitContainerLast;

    void receiverDefaultContainerFirst(@Tags @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") RepeatableAnnotationAliasMatrix this) {}
    void receiverDefaultContainerLast(@annotationaliasmatrix.TagOwnerDerived.Tag("direct") @Tags RepeatableAnnotationAliasMatrix this) {}
    void receiverExplicitContainerFirst(@annotationaliasmatrix.TagOwnerDerived.Tags({@Tag("inner-z"), @TagOwnerBase.Tag("inner-a")}) @annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") RepeatableAnnotationAliasMatrix this) {}
    void receiverExplicitContainerLast(@annotationaliasmatrix.OwnerDerived.Inner.Tag("direct") @Tags({@Tag("inner-z"), @TagOwnerBase.Tag("inner-a")}) RepeatableAnnotationAliasMatrix this) {}

    @TagGroups
    @OwnerDerived.Inner.Tags(label = "direct")
    int defaultOuterContainerFirst;

    @OwnerDerived.Inner.Tags(label = "direct")
    @annotationaliasmatrix.TagOwnerDerived.TagGroups
    int defaultOuterContainerLast;

    String @TagGroups @annotationaliasmatrix.TagOwnerDerived.Tags(label = "direct") [] arrayDefaultOuterContainerFirst;
    String @annotationaliasmatrix.TagOwnerDerived.Tags(label = "direct") @TagGroups [] arrayDefaultOuterContainerLast;

    void receiverDefaultOuterContainerFirst(@TagGroups @annotationaliasmatrix.TagOwnerDerived.Tags(label = "direct") RepeatableAnnotationAliasMatrix this) {}
    void receiverDefaultOuterContainerLast(@annotationaliasmatrix.TagOwnerDerived.Tags(label = "direct") @TagGroups RepeatableAnnotationAliasMatrix this) {}

    @Dollar$Owner.Tag$Kind("dollar-z") @annotationaliasmatrix.Dollar$Owner.Tag$Kind("dollar-a")
    int dollarDeclarations;

    @Dollar\u0024Owner.Tag\u0024Kind("dollar-z") @annotationaliasmatrix.Dollar$Owner.Tag$Kind("dollar-a")
    int escapedDollarDeclarations;

    String @Dollar$Owner.Tag$Kind("dollar-z") @annotationaliasmatrix.Dollar$Owner.Tag$Kind("dollar-a") [] dollarArray;

    void dollarReceiver(@Dollar\u0024Owner.Tag\u0024Kind("dollar-z") @annotationaliasmatrix.Dollar$Owner.Tag$Kind("dollar-a") RepeatableAnnotationAliasMatrix this) {}

    @Dollar$Owner.Tags$Kinds @annotationaliasmatrix.Dollar$Owner.Tag$Kind("dollar-a")
    int dollarContainerFirst;

    @Tag("primary-z")
    @Other.Tag("other-z")
    @TagOwnerDerived.Tag("primary-a")
    @Other.Tag("other-a")
    int independentHomonyms;

    public static void main(String[] arguments) throws ReflectiveOperationException {
        Class<RepeatableAnnotationAliasMatrix> matrixType = RepeatableAnnotationAliasMatrix.class;
        assertCount("declaration pairs", Arrays.stream(matrixType.getDeclaredFields())
                .filter(field -> field.getName().startsWith("fieldPair")).count());
        assertCount("array pairs", Arrays.stream(matrixType.getDeclaredFields())
                .filter(field -> field.getName().startsWith("arrayPair")).count());
        assertCount("receiver pairs", Arrays.stream(matrixType.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("receiverPair")).count());
        for (int firstSpelling = 0; firstSpelling < 8; firstSpelling++) {
            for (int secondSpelling = 0; secondSpelling < 8; secondSpelling++) {
                String pairSuffix = firstSpelling + "_" + secondSpelling;
                assertTagValues("fieldPair" + pairSuffix, matrixType.getDeclaredField("fieldPair" + pairSuffix), "z", "a");
                assertTagValues("arrayPair" + pairSuffix, matrixType.getDeclaredField("arrayPair" + pairSuffix)
                        .getAnnotatedType(), "z", "a");
                assertTagValues("receiverPair" + pairSuffix, matrixType.getDeclaredMethod("receiverPair" + pairSuffix)
                        .getAnnotatedReceiverType(), "z", "a");
            }
        }
        assertTagValues("allDeclarationSpellings", matrixType.getDeclaredField("allDeclarationSpellings"),
                "7", "6", "5", "4", "3", "2", "1", "0");
        assertTagValues("allArraySpellings", matrixType.getDeclaredField("allArraySpellings").getAnnotatedType(),
                "7", "6", "5", "4", "3", "2", "1", "0");
        assertTagValues("allReceiverSpellings", matrixType.getDeclaredMethod("allReceiverSpellings")
                .getAnnotatedReceiverType(), "7", "6", "5", "4", "3", "2", "1", "0");
        assertTagValues("commentedQualification", matrixType.getDeclaredField("commentedQualification"), "comment-z", "comment-a");
        assertTagValues("defaultContainerFirst", matrixType.getDeclaredField("defaultContainerFirst"), "default", "direct");
        assertTagValues("defaultContainerLast", matrixType.getDeclaredField("defaultContainerLast"), "direct", "default");
        assertTagValues("explicitContainerFirst", matrixType.getDeclaredField("explicitContainerFirst"),
                "inner-z", "inner-a", "direct");
        assertTagValues("explicitContainerLast", matrixType.getDeclaredField("explicitContainerLast"),
                "direct", "inner-z", "inner-a");
        assertTagValues("arrayDefaultContainerFirst", matrixType.getDeclaredField("arrayDefaultContainerFirst")
                .getAnnotatedType(), "default", "direct");
        assertTagValues("arrayDefaultContainerLast", matrixType.getDeclaredField("arrayDefaultContainerLast")
                .getAnnotatedType(), "direct", "default");
        assertTagValues("arrayExplicitContainerFirst", matrixType.getDeclaredField("arrayExplicitContainerFirst")
                .getAnnotatedType(), "inner-z", "inner-a", "direct");
        assertTagValues("arrayExplicitContainerLast", matrixType.getDeclaredField("arrayExplicitContainerLast")
                .getAnnotatedType(), "direct", "inner-z", "inner-a");
        assertTagValues("receiverDefaultContainerFirst", matrixType.getDeclaredMethod("receiverDefaultContainerFirst")
                .getAnnotatedReceiverType(), "default", "direct");
        assertTagValues("receiverDefaultContainerLast", matrixType.getDeclaredMethod("receiverDefaultContainerLast")
                .getAnnotatedReceiverType(), "direct", "default");
        assertTagValues("receiverExplicitContainerFirst", matrixType.getDeclaredMethod("receiverExplicitContainerFirst")
                .getAnnotatedReceiverType(), "inner-z", "inner-a", "direct");
        assertTagValues("receiverExplicitContainerLast", matrixType.getDeclaredMethod("receiverExplicitContainerLast")
                .getAnnotatedReceiverType(), "direct", "inner-z", "inner-a");
        assertContainerLabels("defaultOuterContainerFirst", matrixType.getDeclaredField("defaultOuterContainerFirst"),
                "outer-default", "direct");
        assertContainerLabels("defaultOuterContainerLast", matrixType.getDeclaredField("defaultOuterContainerLast"),
                "direct", "outer-default");
        assertContainerLabels("arrayDefaultOuterContainerFirst", matrixType.getDeclaredField("arrayDefaultOuterContainerFirst")
                .getAnnotatedType(), "outer-default", "direct");
        assertContainerLabels("arrayDefaultOuterContainerLast", matrixType.getDeclaredField("arrayDefaultOuterContainerLast")
                .getAnnotatedType(), "direct", "outer-default");
        assertContainerLabels("receiverDefaultOuterContainerFirst", matrixType.getDeclaredMethod("receiverDefaultOuterContainerFirst")
                .getAnnotatedReceiverType(), "outer-default", "direct");
        assertContainerLabels("receiverDefaultOuterContainerLast", matrixType.getDeclaredMethod("receiverDefaultOuterContainerLast")
                .getAnnotatedReceiverType(), "direct", "outer-default");
        assertDollarValues("dollarDeclarations", matrixType.getDeclaredField("dollarDeclarations"), "dollar-z", "dollar-a");
        assertDollarValues("escapedDollarDeclarations", matrixType.getDeclaredField("escapedDollarDeclarations"), "dollar-z", "dollar-a");
        assertDollarValues("dollarArray", matrixType.getDeclaredField("dollarArray").getAnnotatedType(), "dollar-z", "dollar-a");
        assertDollarValues("dollarReceiver", matrixType.getDeclaredMethod("dollarReceiver")
                .getAnnotatedReceiverType(), "dollar-z", "dollar-a");
        assertDollarValues("dollarContainerFirst", matrixType.getDeclaredField("dollarContainerFirst"), "dollar-z", "dollar-a");
        assertTagValues("independentHomonyms", matrixType.getDeclaredField("independentHomonyms"),
                "primary-z", "primary-a");
        assertValues("independentHomonyms/Other.Tag", Arrays.stream(matrixType.getDeclaredField("independentHomonyms")
                .getAnnotationsByType(Other.Tag.class)).map(Other.Tag::value).toList(), "other-z", "other-a");
    }

    private static void assertCount(String label, long actualCount) {
        if (actualCount != 64) {
            throw new IllegalStateException(label + ": expected 64, actual " + actualCount);
        }
    }

    private static void assertTagValues(String label, AnnotatedElement element, String... expectedValues) {
        assertValues(label, Arrays.stream(element.getAnnotationsByType(Tag.class)).map(Tag::value).toList(), expectedValues);
    }

    private static void assertContainerLabels(String label, AnnotatedElement element, String... expectedLabels) {
        assertValues(label, Arrays.stream(element.getAnnotationsByType(TagOwnerBase.Tags.class))
                .map(TagOwnerBase.Tags::label).toList(), expectedLabels);
    }

    private static void assertDollarValues(String label, AnnotatedElement element, String... expectedValues) {
        assertValues(label, Arrays.stream(element.getAnnotationsByType(Dollar$Owner.Tag$Kind.class))
                .map(Dollar$Owner.Tag$Kind::value).toList(), expectedValues);
    }

    private static void assertValues(String label, List<String> actualValues, String... expectedValues) {
        if (!actualValues.equals(List.of(expectedValues))) {
            throw new IllegalStateException(label + ": expected " + List.of(expectedValues) + ", actual " + actualValues);
        }
    }
}

class TagOwnerBase {
    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Tags.class)
    public @interface Tag {
        String value();
    }

    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(TagGroups.class)
    public @interface Tags {
        Tag[] value() default {@Tag("default")};
        String label() default "short";
    }

    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TagGroups {
        Tags[] value() default {@Tags(label = "outer-default")};
    }
}

class TagOwnerMid extends TagOwnerBase {}

class TagOwnerDerived extends TagOwnerMid {}

class OwnerBase {
    static class Inner extends TagOwnerDerived {}
}

class OwnerMid extends OwnerBase {}

class OwnerDerived extends OwnerMid {}

class Dollar$Owner {
    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Tags$Kinds.class)
    public @interface Tag$Kind {
        String value();
    }

    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Tags$Kinds {
        Tag$Kind[] value() default {@Tag$Kind("dollar-z")};
    }
}

class Other {
    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Tags.class)
    public @interface Tag {
        String value();
    }

    @Target({ElementType.FIELD, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Tags {
        Tag[] value();
    }
}

@Target({ElementType.FIELD, ElementType.TYPE_USE})
@interface Able {}

@Target({ElementType.FIELD, ElementType.TYPE_USE})
@interface Zed {}
