// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

// @Z @A are comment text.
@A @Z(value = "parentheses: ( ) and annotations: @Z @A") /* @Z @A */
class AnnotationLiteralAndCommentPreservation {

    @A
    @Z("""
            @Z @A
            unmatched: ( ) )
            """)
    String value = "@Z @A";

    @A @Z(value = "escaped quote: \" ) @Z @A")
    String other;
}

@interface A {}

@interface Z {
    String value();
}
