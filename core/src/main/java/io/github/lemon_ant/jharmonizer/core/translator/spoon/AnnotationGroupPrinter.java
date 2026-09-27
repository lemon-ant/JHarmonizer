// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcGap;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcGroup;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import lombok.NonNull;

/** Copies source slices, printing each encountered annotation group from its prepared fragments. */
final class AnnotationGroupPrinter {

    @NonNull
    private final NavigableMap<Integer, AnnotationSrcGroup> annotationSrcGroupsBySrcStart = new TreeMap<>();

    /**
     * Indexes group boundaries without reconstructing annotation order or individual replacement ranges.
     * @param annotationSrcGroups groups containing prepared fragments in the requested print order
     */
    AnnotationGroupPrinter(@NonNull List<AnnotationSrcGroup> annotationSrcGroups) {
        annotationSrcGroups.forEach(group -> annotationSrcGroupsBySrcStart.put(group.getStart(), group));
    }

    /**
     * Copies a source slice, emitting each complete annotation group in its requested print order.
     * @param output destination buffer
     * @param srcCode original source
     * @param start first source offset
     * @param endExclusive end of the source slice
     */
    void append(@NonNull StringBuilder output, @NonNull String srcCode, int start, int endExclusive) {
        int cursor = start;
        for (AnnotationSrcGroup group : annotationSrcGroupsBySrcStart
                .subMap(start, true, endExclusive, false)
                .values()) {
            if (group.getEndExclusive() > endExclusive) {
                // A slice ending inside a group cannot safely reorder that group independently.
                continue;
            }
            output.append(srcCode, cursor, group.getStart());
            appendGroup(output, group);
            cursor = group.getEndExclusive();
        }
        output.append(srcCode, cursor, endExclusive);
    }

    private static void appendGroup(StringBuilder output, AnnotationSrcGroup group) {
        List<AnnotationSrcFragment> fragmentsInPrintOrder = group.getFragmentsInPrintOrder();
        for (int index = 0; index < fragmentsInPrintOrder.size(); index++) {
            AnnotationSrcFragment fragment = fragmentsInPrintOrder.get(index);
            AnnotationSrcGap gap = group.getGapsInSrcOrder().get(index);
            output.append(fragment.getSrcCode());
            if (fragment.getStart() == gap.getAnnotationStart()) {
                // A stationary annotation retains the entire gap, including whitespace such as form feeds.
                output.append(gap.getOriginalSrcCode());
            } else if (!fragment.getTrailingLineSeparators().isEmpty()
                    && fragment.getTrailingLineSeparatorCount() > gap.getRetainedLineSeparatorCount()) {
                // An escaped line terminator is already inside srcCode and needs no physical separator or reindent.
                output.append(fragment.getTrailingLineSeparators()).append(gap.getIndentedRemainder());
            } else {
                output.append(gap.getSrcCode());
            }
        }
    }
}
