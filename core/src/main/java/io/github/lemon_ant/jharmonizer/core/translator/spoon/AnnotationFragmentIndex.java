// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.countLineSeparators;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/** Indexes reordered annotations with attached comments while preserving surrounding source gaps. */
final class AnnotationFragmentIndex {

    // TODO Annotations: Keep it in the scanner
    private static final int BLANK_LINE_SEPARATOR_COUNT = 2;

    /** Changed source slots, keyed by their original start offsets for range printing. */
    @NonNull
    private final NavigableMap<Integer, Replacement> replacements = new TreeMap<>();

    /**
     * Indexes annotation groups in their requested print order.
     * @param srcCode original source containing the gaps between annotations
     * @param annotationGroupsInPrintOrder annotation groups, each containing fragments in the order to print
     */
    AnnotationFragmentIndex(
            @NonNull String srcCode, @NonNull List<List<AnnotationSrcFragment>> annotationGroupsInPrintOrder) {
        annotationGroupsInPrintOrder.forEach(
                annotationsInPrintOrder -> indexAnnotations(annotationsInPrintOrder, srcCode));
    }

    /**
     * Copies a source slice, replacing annotations and attached comments with their model-ordered source fragments.
     * @param output destination buffer
     * @param srcCode original source
     * @param start first source offset
     * @param endExclusive end of the source slice
     */
    void append(@NonNull StringBuilder output, @NonNull String srcCode, int start, int endExclusive) {
        int cursor = start;
        for (Replacement replacement :
                replacements.subMap(start, true, endExclusive, false).values()) {
            if (replacement.getStart() < cursor || replacement.getEndExclusive() > endExclusive) {
                // TODO Annotations: Can it be in reality??? Can you activate this branch in tests?
                continue;
            }
            output.append(srcCode, cursor, replacement.getStart())
                    .append(srcCode, replacement.getSrcStart(), replacement.getSrcEndExclusive());
            cursor = replacement.getEndExclusive();
            if (replacement.isReplaceTrailingBlankLines()
                    || replacement.getSrcLineSeparatorEndExclusive() > replacement.getSrcEndExclusive()) {
                cursor = appendLineSeparators(output, srcCode, replacement, endExclusive);
            }
        }
        output.append(srcCode, cursor, endExclusive);
    }

    private static int appendLineSeparators(
            StringBuilder output, String srcCode, Replacement replacement, int endExclusive) {
        int nextTokenStart = replacement.getEndExclusive();
        while (nextTokenStart < endExclusive && Character.isWhitespace(srcCode.charAt(nextTokenStart))) {
            nextTokenStart++;
        }
        if (nextTokenStart == endExclusive
                || !replacement.isReplaceTrailingBlankLines()
                        && countLineSeparators(replacement.getEndExclusive(), nextTokenStart, srcCode)
                                >= replacement.getSrcLineSeparatorCount()) {
            return replacement.getEndExclusive();
        }
        int lineStart =
                Math.max(srcCode.lastIndexOf('\r', nextTokenStart - 1), srcCode.lastIndexOf('\n', nextTokenStart - 1))
                        + 1;
        appendReplacementLineSeparators(output, srcCode, replacement, lineStart);
        int indentationEnd = lineStart;
        while (indentationEnd < nextTokenStart
                && (srcCode.charAt(indentationEnd) == ' ' || srcCode.charAt(indentationEnd) == '\t')) {
            indentationEnd++;
        }
        output.append(srcCode, lineStart, indentationEnd);
        return nextTokenStart;
    }

    private static void appendReplacementLineSeparators(
            StringBuilder output, String srcCode, Replacement replacement, int lineStart) {
        if (replacement.getSrcLineSeparatorEndExclusive() > replacement.getSrcEndExclusive()) {
            output.append(srcCode, replacement.getSrcEndExclusive(), replacement.getSrcLineSeparatorEndExclusive());
        } else {
            // The lower comment block takes its blank lines with it; keep only the destination's line break.
            int separatorStart = lineStart - 1;
            if (srcCode.charAt(separatorStart) == '\n'
                    && separatorStart > 0
                    && srcCode.charAt(separatorStart - 1) == '\r') {
                separatorStart--;
            }
            output.append(srcCode, separatorStart, lineStart);
        }
    }

    private void indexAnnotations(List<AnnotationSrcFragment> annotationsInPrintOrder, String srcCode) {
        // Fragment offsets still refer to the original source. Restore the destination slots in source order,
        // then pair each slot with the annotation that should occupy it in the print order.
        // TODO Annotations: Do we need to restore the original order if it exists before sorting
        List<AnnotationSrcFragment> annotationsInSrcOrder = annotationsInPrintOrder.stream()
                .sorted(Comparator.comparingInt(AnnotationSrcFragment::getStart))
                .toList();
        for (int index = 0; index < annotationsInPrintOrder.size(); index++) {
            AnnotationSrcFragment target = annotationsInSrcOrder.get(index);
            AnnotationSrcFragment source = annotationsInPrintOrder.get(index);
            // TODO Annotations: Can we do here target == source because we reordering the same fragment objects?
            if (target.getStart() != source.getStart()) {
                int nextFragmentStart = index + 1 < annotationsInSrcOrder.size()
                        ? annotationsInSrcOrder.get(index + 1).getLeadingCommentStart()
                        : target.getFollowingTokenStart();
                // A gap containing a separate comment block also belongs to that block and must stay separated.
                // TODO Annotations: Can we keep BLANK_LINE_SEPARATOR_COUNT in the scanner and have a boolean method in
                // the AnnotationSrcFragment?
                boolean replaceTrailingBlankLines = target.getTrailingLineSeparatorCount() == BLANK_LINE_SEPARATOR_COUNT
                        && srcCode.substring(target.getTrailingCommentEndExclusive(), nextFragmentStart)
                                .isBlank();
                replacements.put(
                        target.getLeadingCommentStart(),
                        // TODO Annotations: Don't you think that replacement can just contain fragment srcCode
                        // Possibly the tasks is simpier. We have to detect in printer that group of fragments starts
                        // and print srcCode of each fragments in the sorted order instead of the original srcCode
                        Replacement.builder()
                                .start(target.getLeadingCommentStart())
                                .endExclusive(target.getTrailingCommentEndExclusive())
                                .srcStart(source.getLeadingCommentStart())
                                .srcEndExclusive(source.getTrailingCommentEndExclusive())
                                .srcLineSeparatorCount(source.getTrailingLineSeparatorCount())
                                .srcLineSeparatorEndExclusive(source.getTrailingLineSeparatorEndExclusive())
                                .replaceTrailingBlankLines(replaceTrailingBlankLines)
                                .build());
            }
        }
    }

    @Value
    // Named construction stays valid when the build's source reorderer changes field order.
    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Replacement {

        /** Original destination end after its attached comments. */
        int endExclusive;

        /** Whether the destination blank lines belong only to its departing comment block. */
        boolean replaceTrailingBlankLines;

        /** End of the source annotation and attached comments, excluding trailing line separators. */
        int srcEndExclusive;

        /** Minimum separator count required to preserve the copied comment's attachment. */
        int srcLineSeparatorCount;

        /** End of the copied comment's original separator, used when the destination gap is insufficient. */
        int srcLineSeparatorEndExclusive;

        /** Start of the source annotation and comments to copy into this slot. */
        int srcStart;

        /** Original destination start, including attached leading comments. */
        int start;
    }
}
