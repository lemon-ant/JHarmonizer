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

/** Immutable annotation fragments, source slots, and placement layouts prepared once by the scanner. */
@Value
public class AnnotationSrcGroup {

    /** Annotation fragments in this group's original source order, retained unchanged by sorting. */
    @NonNull
    List<AnnotationSrcFragment> annotationSrcFragmentsInSrcOrder;

    /** Immutable gap slots and placement profiles in their original source order. */
    @Getter(AccessLevel.PACKAGE)
    @NonNull
    List<AnnotationSrcGap> annotationSrcGapsInSrcOrder;

    /** First source offset after the final gap, before the following Java token. */
    int endExclusive;

    /**
     * Original group text, computed once from source-order fragments.
     * Other annotation orders are rendered from the same immutable pieces without replacing this group.
     */
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
     * Retains read-only views and lazily pairs each annotation with the gap at the same index.
     * Callers must not modify either list after handoff. The final annotation also requires a gap, possibly empty.
     * @param annotationSrcFragmentsInSrcOrder annotations in original source order
     * @param annotationSrcGapsInSrcOrder gap slots in original source order, with all placement layouts prepared
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

    /**
     * Prepared source text and sorting keys for one annotation, including its attached comments.
     * Separators required by trailing comments remain separate so existing destination line breaks can be reused.
     */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class AnnotationSrcFragment extends SrcFragment {

        /**
         * Offset of the opening {@code @}, used for relocation detection and opt-out ranges.
         * The inherited {@link SrcFragment#getStart()} includes attached leading comments and can be earlier.
         * For {@code @A\n// note\n@B}, the second fragment starts at {@code //}, while this offset points to {@code @B}.
         */
        int annotationStart;

        /** Precomputed annotation ordering keys and semantic constraints. */
        @NonNull
        AnnotationDescriptor descriptor;

        /**
         * Minimum physical line separators needed after this fragment's trailing comments.
         * {@code @A @B} needs zero; {@code @A // note\n@B} needs one to terminate the line comment;
         * {@code @A\n// below\n\n@B} needs two so the lower comment remains attached to A when reordered.
         * These separators are not part of the fragment's source text.
         */
        int trailingLineSeparatorCount;

        /**
         * Original separators to insert when the destination gap has too few retained separators.
         * Moving {@code @A // note} into the space before B in {@code @C @B} needs A's line separator;
         * moving {@code @A\n// below} there needs its following blank line to keep the comment attached to A.
         */
        @NonNull
        String trailingLineSeparators;

        // Explicit construction initializes the inherited source range; callers use named builder arguments.
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

    /** Ready gap prefix and whether the annotation's exact separator text precedes it. */
    @Value
    @Getter(AccessLevel.PACKAGE)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class AnnotationGapLayout {
        boolean annotationSeparatorsRequired;

        @NonNull
        String leadingWhitespace;
    }

    /** Original gap text and scanner-prepared whitespace layouts for possible annotation placements. */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class AnnotationSrcGap extends SrcFragment {

        /** Fixed comments and text shared by every placement of this source gap. */
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

    /** Immutable scanner-prepared fragment text and its original source range. */
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
