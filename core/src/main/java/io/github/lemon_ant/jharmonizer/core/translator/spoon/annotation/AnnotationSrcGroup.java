// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

/** One group of adjacent annotation uses with an annotation fragment and a following gap at each source slot. */
@Value
public class AnnotationSrcGroup {

    /** Annotation fragments in this group's original source order, retained unchanged by sorting. */
    @NonNull
    List<AnnotationSrcFragment> annotationSrcFragmentsInSrcOrder;

    /** Immutable gap slots and placement profiles in their original source order. */
    @Getter(AccessLevel.PACKAGE)
    @NonNull
    List<AnnotationSrcGap> annotationSrcGapsInSrcOrder;

    /** Exclusive end of the original group, including the final gap. */
    int endExclusive;

    /** Original group text, including all gaps. */
    @Getter(lazy = true)
    @NonNull
    String replacementCode =
            getSrcFragments().stream().map(SrcFragment::getSrcCode).collect(Collectors.joining());

    /** Original annotation/gap sequence, assembled and cached when original group text is requested. */
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @NonNull
    List<SrcFragment> srcFragments = interleaveFragments(annotationSrcFragmentsInSrcOrder, annotationSrcGapsInSrcOrder);

    /** First attached leading comment, or the first annotation's opening {@code @}. */
    int start;

    @NonNull
    private static List<SrcFragment> interleaveFragments(
            List<AnnotationSrcFragment> annotationSrcFragmentsInSrcOrder,
            List<AnnotationSrcGap> annotationSrcGapsInSrcOrder) {
        List<SrcFragment> fragmentsInSrcOrder =
                new ArrayList<>(annotationSrcFragmentsInSrcOrder.size() + annotationSrcGapsInSrcOrder.size());
        for (int annotationIndex = 0; annotationIndex < annotationSrcFragmentsInSrcOrder.size(); annotationIndex++) {
            fragmentsInSrcOrder.add(annotationSrcFragmentsInSrcOrder.get(annotationIndex));
            fragmentsInSrcOrder.add(annotationSrcGapsInSrcOrder.get(annotationIndex));
        }
        return Collections.unmodifiableList(fragmentsInSrcOrder);
    }

    /**
     * Retains read-only views of the lists. Callers must leave their backing lists unchanged after handoff.
     * The final annotation also requires a gap, which may be empty.
     * @param annotationSrcFragmentsInSrcOrder annotations in original source order
     * @param annotationSrcGapsInSrcOrder gaps in original source order, with prepared layouts
     * @param start first offset of the original group
     * @param endExclusive first source offset after the original group
     * @throws IllegalArgumentException if annotations are empty or the two lists have different sizes
     */
    AnnotationSrcGroup(
            @NonNull List<AnnotationSrcFragment> annotationSrcFragmentsInSrcOrder,
            @NonNull List<AnnotationSrcGap> annotationSrcGapsInSrcOrder,
            int start,
            int endExclusive) {
        this.annotationSrcFragmentsInSrcOrder = Collections.unmodifiableList(annotationSrcFragmentsInSrcOrder);
        this.annotationSrcGapsInSrcOrder = Collections.unmodifiableList(annotationSrcGapsInSrcOrder);
        if (this.annotationSrcFragmentsInSrcOrder.isEmpty()
                || this.annotationSrcFragmentsInSrcOrder.size() != this.annotationSrcGapsInSrcOrder.size()) {
            throw new IllegalArgumentException(
                    "An annotation group requires at least one annotation and exactly one gap per annotation");
        }
        this.start = start;
        this.endExclusive = endExclusive;
    }

    /** Source text, attached comments, and ordering metadata for one annotation use. */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class AnnotationSrcFragment extends SrcFragment {

        /**
         * Offset of the opening {@code @}, used for opt-out ranges. The fragment itself starts at any attached
         * leading comment.
         */
        int annotationStart;

        /** Precomputed annotation ordering keys and semantic constraints. */
        @NonNull
        AnnotationDescriptor descriptor;

        /**
         * Minimum number of physical separators after the fragment. Use one for a line comment whose terminator
         * lies outside the fragment, or two to retain the blank line after an attached lower comment block.
         */
        int trailingLineSeparatorCount;

        /** Original separators to insert if the destination gap supplies too few. */
        @NonNull
        String trailingLineSeparators;

        @Builder(access = AccessLevel.PACKAGE)
        private AnnotationSrcFragment(
                int annotationStart,
                @NonNull AnnotationDescriptor descriptor,
                int endExclusive,
                String srcCode,
                int start,
                int trailingLineSeparatorCount,
                @NonNull String trailingLineSeparators) {
            super(endExclusive, srcCode, start);
            this.annotationStart = annotationStart;
            this.descriptor = descriptor;
            this.trailingLineSeparatorCount = trailingLineSeparatorCount;
            this.trailingLineSeparators = trailingLineSeparators;
        }

        /**
         * Indicates whether trailing comments require a blank line before the next token.
         * @return true when more than one physical line separator is required
         */
        boolean requiresBlankLineAfterTrailingComments() {
            return trailingLineSeparatorCount > 1;
        }
    }

    /** Leading whitespace to use after a moved annotation, with a flag for inserting its original separators first. */
    @Value
    @Getter(AccessLevel.PACKAGE)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class AnnotationGapLayout {
        boolean annotationSeparatorsRequired;

        @NonNull
        String leadingWhitespace;
    }

    /** Original gap text and whitespace layouts for a moved annotation. */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class AnnotationSrcGap extends SrcFragment {

        /** Gap text after its leading whitespace, shared by all layouts. */
        @Getter(AccessLevel.PACKAGE)
        @NonNull
        String content;

        /** Layouts indexed by the preceding annotation's required line-separator count. */
        @Getter(AccessLevel.PACKAGE)
        @NonNull
        List<AnnotationGapLayout> movedLayoutsByRequiredSeparatorCount;

        /**
         * Retains original gap code and immutable layouts prepared while scanning.
         * @param content fixed gap content without leading whitespace
         * @param endExclusive original gap end offset
         * @param movedLayoutsByRequiredSeparatorCount ready layouts for requirements zero, one, and two
         * @param srcCode complete original gap text
         * @param start original gap start offset
         */
        AnnotationSrcGap(
                @NonNull String content,
                int endExclusive,
                @NonNull List<AnnotationGapLayout> movedLayoutsByRequiredSeparatorCount,
                @NonNull String srcCode,
                int start) {
            super(endExclusive, srcCode, start);
            this.content = content;
            this.movedLayoutsByRequiredSeparatorCount = List.copyOf(movedLayoutsByRequiredSeparatorCount);
        }
    }

    /** A source fragment's text and half-open range in the original file. */
    @Value
    @NonFinal
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SrcFragment {

        /** First offset after the original fragment; replacement text may have a different length. */
        int endExclusive;

        /** Original annotation or gap text, before selecting a different annotation order. */
        @NonNull
        String srcCode;

        /** Original fragment start, including attached leading comments for an annotation. */
        int start;
    }
}
