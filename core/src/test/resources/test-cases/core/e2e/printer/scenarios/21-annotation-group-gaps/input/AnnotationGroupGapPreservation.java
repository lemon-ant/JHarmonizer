// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class AnnotationGroupGapPreservation {

    @Ccc
    @Bbb // stays in the middle
    @Aaa
    int unchangedMiddle;

    @Ccc

    /* stays between the first two slots */

    @Bbb

    @Aaa
    int independentComment;

    @Aaa
    // stays below the first annotation

    @Ccc @Bbb
    int unchangedLeadingBlock;

    @Bbb @Aaa @Ccc // stays after the last annotation
    int unchangedTrailingComment;

    @Ccc // escaped terminator\u000a @Aaa int escapedLineTerminator;

    @Ccc
    @Bbb
    // moves with Bbb

    // stays before the annotation type
    @interface CommentedAnnotation {}
}

@interface Aaa {}

@interface Bbb {}

@interface Ccc {}
