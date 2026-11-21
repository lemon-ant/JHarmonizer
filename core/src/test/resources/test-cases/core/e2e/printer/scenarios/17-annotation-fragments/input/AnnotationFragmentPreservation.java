// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Zed("class")
// class annotation
@Able
class AnnotationFragmentPreservation {

    @Zed(value = "field") /* gap */
    @Able
    int value;

    @Zed("both") /* zed */ @Able /** able */
    int blockComments;

    @Zed("multiline") /* first
            continuation */ /* second */
    @Able
    int multilineComment;

    @Zed("line") // zed
    @Able // able
    int lineComments;

    @Zed("detached") /* attached */
    // separate line
    /* separate block */
    @Able
    int detachedComments;

    // leading line
    /* leading block */
    @Zed("leading")
    @Able
    int leadingComments;

    @Zed("separated")
    // separated comment

    @Able
    int separatedComments;

    @Zed("split") // trailing
    // below zed

    /* adjacent block */
    // adjacent line
    @Able
    int splitComments;

    @Zed("detached block")

    // detached block

    @Able
    int detachedBlock;

    @Zed("below blocks")
    /* first

       continuation */
    /* second */

    @Able
    int belowBlockComments;

    @Zed("last") @Able
    // below able

    int belowLastAnnotation;

    @Zed("multiple blocks")
    // below zed

    /* detached middle */

    // above able
    @Able
    int multipleCommentBlocks;

    @Zed("annotation gap")

    @Able
    int separatedAnnotations;

    @Zed("line after below")
    /* below zed */

    @Able // able
    int separatedLineComment;

    @Zed("detached after")
    @Able
    // below able

    /* detached declaration */

    int detachedAfterAnnotations;

    @Zed("multiline leading")
    /* first
       continuation */ /* second */
    @Able
    int multilineLeadingComments;

    /** Documents the field. */
    @Zed("documented")
    @Able
    int documented;

    @Zed("inline") @Able // able
    int inlineComment;

    @Zed("inline declaration") // zed
    @Able int inlineDeclaration;

    @Zed("constructor") @Able
    AnnotationFragmentPreservation(@Zed("parameter") @Able int value) {
        this.value = value;
    }

    @Zed(
            value = "method")
    @Able
    void execute(@Zed("argument") @Able String input) {
        @Zed("local") @Able String local = input;
        java.util.List<@Zed("type use") @Able String> values = java.util.List.of(local);
    }

    @Zed("nested") @Able
    static class Nested {}

    @java.lang.Deprecated @SuppressWarnings("unused")
    void legacy() {}
}

@java.lang.annotation.Target({
        java.lang.annotation.ElementType.TYPE,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.CONSTRUCTOR,
        java.lang.annotation.ElementType.METHOD,
        java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.LOCAL_VARIABLE,
        java.lang.annotation.ElementType.TYPE_USE})
@interface Able {}

@java.lang.annotation.Target({
        java.lang.annotation.ElementType.TYPE,
        java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.CONSTRUCTOR,
        java.lang.annotation.ElementType.METHOD,
        java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.LOCAL_VARIABLE,
        java.lang.annotation.ElementType.TYPE_USE})
@interface Zed {
    String value();
}
