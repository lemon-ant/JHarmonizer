// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

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

/** A source replacement initialized in source order and finalized in sorted order before printing. */
@Value
public class AnnotationSrcGroup {

    /** Current annotation order: source order after scanning, comparator order after sorting. */
    @NonNull
    List<AnnotationSrcFragment> annotationSrcFragments;

    /** Gap slots retain source order and ranges; sorting can replace their text for new preceding annotations. */
    @NonNull
    List<AnnotationSrcGap> annotationSrcGapsInSrcOrder;

    /** First source offset after the final gap, before the following Java token. */
    int endExclusive;

    /**
     * Concatenated fragment text, computed once on first access.
     * Requests the lazy fragment sequence only when this group's replacement is needed.
     */
    @Getter(lazy = true)
    @NonNull
    String replacementCode =
            getSrcFragments().stream().map(SrcFragment::getSrcCode).collect(Collectors.joining());

    /** Internal annotation/gap sequence, assembled and cached when replacement code is requested. */
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @NonNull
    List<SrcFragment> srcFragments = interleaveFragments(annotationSrcFragments, annotationSrcGapsInSrcOrder);

    /** First attached leading comment, or the first annotation's opening {@code @}. */
    int start;

    @NonNull
    private static List<SrcFragment> interleaveFragments(
            List<AnnotationSrcFragment> annotationSrcFragments, List<AnnotationSrcGap> annotationSrcGapsInSrcOrder) {
        List<SrcFragment> fragmentsInCurrentOrder =
                new ArrayList<>(annotationSrcFragments.size() + annotationSrcGapsInSrcOrder.size());
        for (int annotationIndex = 0; annotationIndex < annotationSrcFragments.size(); annotationIndex++) {
            fragmentsInCurrentOrder.add(annotationSrcFragments.get(annotationIndex));
            fragmentsInCurrentOrder.add(annotationSrcGapsInSrcOrder.get(annotationIndex));
        }
        return Collections.unmodifiableList(fragmentsInCurrentOrder);
    }

    /**
     * Retains read-only views and lazily pairs each annotation with the gap at the same index.
     * Callers must not modify either list after handoff. The final annotation also requires a gap, possibly empty.
     * @param annotationSrcFragments annotations in this group's current order
     * @param annotationSrcGapsInSrcOrder gap slots in original source order, with text prepared for the current annotations
     * @param start first offset of the original group
     * @param endExclusive first source offset after the original group
     * @throws IllegalArgumentException if annotations are empty or the two lists have different sizes
     */
    public AnnotationSrcGroup(
            @NonNull List<AnnotationSrcFragment> annotationSrcFragments,
            @NonNull List<AnnotationSrcGap> annotationSrcGapsInSrcOrder,
            int start,
            int endExclusive) {
        this.annotationSrcFragments = Collections.unmodifiableList(annotationSrcFragments);
        this.annotationSrcGapsInSrcOrder = Collections.unmodifiableList(annotationSrcGapsInSrcOrder);
        if (this.annotationSrcFragments.isEmpty()
                || this.annotationSrcFragments.size() != this.annotationSrcGapsInSrcOrder.size()) {
            throw new IllegalArgumentException(
                    "An annotation group requires at least one annotation and exactly one gap per annotation");
        }
        this.start = start;
        this.endExclusive = endExclusive;
    }

    /** Scanner-prepared gap components; sorting selects a whitespace prefix without inspecting source text. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class AnnotationGapLayout {

        /** Independent comments and all following text, excluding leading whitespace. */
        @Getter(AccessLevel.PRIVATE)
        @NonNull
        String content;

        /** Destination line indentation to append after separators supplied by an annotation. */
        @NonNull
        String indentation;

        /** Exact original whitespace prefix, restored when the original preceding annotation returns. */
        @NonNull
        String leadingWhitespace;

        /** Prefix for a moved annotation: original whitespace, or one line break after a departing lower block. */
        @NonNull
        String relocatedLeadingWhitespace;

        /** Available physical separators; zero when an attached lower block takes its blank lines with it. */
        int retainedLineSeparatorCount;
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

        /** Sorting keys computed from the annotation's name, argument tokens, and internal comments. */
        @NonNull
        AnnotationDescriptor descriptor;

        /**
         * Minimum physical separators needed outside this fragment: zero normally, one after {@code //},
         * two after an attached lower comment block.
         */
        int trailingLineSeparatorCount;

        /** Original separators needed when a destination cannot preserve the trailing comment's attachment. */
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
        boolean requiresTrailingBlankLine() {
            return trailingLineSeparatorCount > 1;
        }
    }

    /** Ready-to-emit gap text with scanner-prepared components for subsequent sorting. */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class AnnotationSrcGap extends SrcFragment {

        /** Fixed content and whitespace choices prepared once by the scanner, shared by all placements of this gap. */
        @NonNull
        AnnotationGapLayout layout;

        /**
         * Combines the selected whitespace prefix with the gap's fixed content.
         * @param endExclusive first source offset after the original gap
         * @param layout scanner-prepared content and whitespace choices
         * @param leadingWhitespace selected prefix for the current preceding annotation
         * @param start first source offset of the original gap
         */
        public AnnotationSrcGap(
                int endExclusive, @NonNull AnnotationGapLayout layout, @NonNull String leadingWhitespace, int start) {
            super(endExclusive, leadingWhitespace + layout.getContent(), start);
            this.layout = layout;
        }
    }

    /** Immutable fragment text and its original source range, retained even when sorting changes its position or text. */
    @Value
    @NonFinal
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static sealed class SrcFragment permits AnnotationSrcFragment, AnnotationSrcGap {

        /** First offset after the original fragment; replacement text may have a different length. */
        int endExclusive;

        /** Text to emit for this fragment, including any gap adjustment already made by the sorter. */
        @NonNull
        String srcCode;

        /** Original fragment start, including attached leading comments for an annotation. */
        int start;
    }
}
