// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class AnnotationGroupGapPreservation {

    @Aaa
    @Bbb // stays in the middle
    @Ccc
    int unchangedMiddle;

    @Aaa

    /* stays between the first two slots */

    @Bbb

    @Ccc
    int independentComment;

    @Aaa
    // stays below the first annotation

    @Bbb @Ccc
    int unchangedLeadingBlock;

    @Aaa @Bbb @Ccc // stays after the last annotation
    int unchangedTrailingComment;

    @Aaa @Ccc // escaped terminator\u000a int escapedLineTerminator;

    @Bbb
    // moves with Bbb

    @Ccc

    // stays before the annotation type
    @interface CommentedAnnotation {}
}

@interface Aaa {}

@interface Bbb {}

@interface Ccc {}
