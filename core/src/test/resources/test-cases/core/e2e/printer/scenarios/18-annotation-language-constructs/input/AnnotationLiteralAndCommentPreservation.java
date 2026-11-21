// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

// @Z @A are comment text.
@Z(value = "parentheses: ( ) and annotations: @Z @A") /* @Z @A */ @A
class AnnotationLiteralAndCommentPreservation {

    @Z("""
            @Z @A
            unmatched: ( ) )
            """)
    @A
    String value = "@Z @A";

    @Z(value = "escaped quote: \" ) @Z @A") @A
    String other;
}

@interface A {}

@interface Z {
    String value();
}
