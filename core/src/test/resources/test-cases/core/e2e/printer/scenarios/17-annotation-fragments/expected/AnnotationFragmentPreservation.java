// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

// class annotation
@Able
@Zed("class")
class AnnotationFragmentPreservation {

    @Able
    @Zed(value = "field") /* gap */
    int value;

    @Able /** able */ @Zed("both") /* zed */
    int blockComments;

    @Able
    @Zed("multiline") /* first
            continuation */ /* second */
    int multilineComment;

    @Able // able
    @Zed("line") // zed
    int lineComments;

    // separate line
    /* separate block */
    @Able
    @Zed("detached") /* attached */
    int detachedComments;

    @Able
    // leading line
    /* leading block */
    @Zed("leading")
    int leadingComments;

    @Able
    @Zed("separated")
    // separated comment

    int separatedComments;

    /* adjacent block */
    // adjacent line
    @Able
    @Zed("split") // trailing
    // below zed

    int splitComments;

    @Able

    // detached block

    @Zed("detached block")
    int detachedBlock;

    @Able
    @Zed("below blocks")
    /* first

       continuation */
    /* second */

    int belowBlockComments;

    @Able
    // below able

    @Zed("last")
    int belowLastAnnotation;

    // above able
    @Able

    /* detached middle */

    @Zed("multiple blocks")
    // below zed

    int multipleCommentBlocks;

    @Able

    @Zed("annotation gap")
    int separatedAnnotations;

    @Able // able
    @Zed("line after below")
    /* below zed */

    int separatedLineComment;

    @Able
    // below able

    @Zed("detached after")

    /* detached declaration */

    int detachedAfterAnnotations;

    /* first
       continuation */ /* second */
    @Able
    @Zed("multiline leading")
    int multilineLeadingComments;

    /** Documents the field. */
    @Able
    @Zed("documented")
    int documented;

    @Able // able
    @Zed("inline")
    int inlineComment;

    @Able
    @Zed("inline declaration") // zed
    int inlineDeclaration;

    @Able @Zed("constructor")
    AnnotationFragmentPreservation(@Able @Zed("parameter") int value) {
        this.value = value;
    }

    @Able
    @Zed(
            value = "method")
    void execute(@Able @Zed("argument") String input) {
        @Able @Zed("local") String local = input;
        java.util.List<@Able @Zed("type use") String> values = java.util.List.of(local);
    }

    @Able @Zed("nested")
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
