// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class UnicodeAnnotationDeclarationLength {

    \u0040A
    @BB
    int escapedAt;

    @\u0041
    @BB
    int escapedIdentifier;

    @A\u0028\u0029
    @CCC
    int escapedParentheses;

    @Nest\u002eA
    @LongMarker
    int escapedQualification;

    @AString("\u0061")
    @LongMarkerName
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
