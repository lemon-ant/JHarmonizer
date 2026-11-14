// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class UnicodeAnnotationDeclarationLength {

    @BB
    \u0040A
    int escapedAt;

    @BB
    @\u0041
    int escapedIdentifier;

    @CCC
    @A\u0028\u0029
    int escapedParentheses;

    @LongMarker
    @Nest\u002eA
    int escapedQualification;

    @LongMarkerName
    @AString("\u0061")
    int escapedStringValue;

    @interface A {}

    @interface BB {}

    @interface CCC {}

    @interface LongMarker {}

    @interface LongMarkerName {}

    @interface AString {
        String value();
    }

    static class Nest {

        @interface A {}
    }
}
