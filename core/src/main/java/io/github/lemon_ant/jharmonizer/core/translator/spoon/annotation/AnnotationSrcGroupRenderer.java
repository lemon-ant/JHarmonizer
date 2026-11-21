// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationGapLayout;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/** Renders annotation groups from their source fragments and prepared whitespace layouts. */
@UtilityClass
public class AnnotationSrcGroupRenderer {

    /**
     * Writes the selected fragments into the group's original gap slots. Each gap retains its exact text when
     * its original annotation precedes it. A moved annotation selects a layout by its separator requirement.
     * @param sourceGroup group prepared by the scanner
     * @param annotationOrder selected fragments, or {@code null} to retain the original source text
     * @return complete replacement text for the group's original source range
     * @throws IllegalArgumentException if the selected order has a different number of annotations
     */
    @NonNull
    public static String renderReplacement(
            @NonNull AnnotationSrcGroup sourceGroup, @Nullable List<AnnotationSrcFragment> annotationOrder) {
        if (annotationOrder == null) {
            return sourceGroup.getReplacementCode();
        }
        List<AnnotationSrcGap> sourceGaps = sourceGroup.getAnnotationSrcGapsInSrcOrder();
        if (annotationOrder.size() != sourceGaps.size()) {
            throw new IllegalArgumentException("An annotation order requires exactly one fragment per source gap");
        }
        StringBuilder replacement = new StringBuilder();
        for (int slotIndex = 0; slotIndex < annotationOrder.size(); slotIndex++) {
            AnnotationSrcFragment annotation = annotationOrder.get(slotIndex);
            AnnotationSrcGap gap = sourceGaps.get(slotIndex);
            replacement.append(annotation.getSrcCode());
            if (annotation.getEndExclusive() == gap.getStart()) {
                // TODO What does it mean???
                // Stable source offsets also recognize fragments recreated during dependency-key preparation.
                replacement.append(gap.getSrcCode());
                continue;
            }
            // TODO Add understandable src examples
            // Sorting chooses the annotation/gap pair after scanning: in @A // note\r\n@B\n@C class Sample {},
            // moving A to B's slot reuses its LF gap; moving A to C's inline slot needs A's CRLF.
            // One fixed separator string on A cannot represent both placements.
            AnnotationGapLayout layout =
                    gap.getMovedLayoutsByRequiredSeparatorCount().get(annotation.getTrailingLineSeparatorCount());
            if (layout.isAnnotationSeparatorsRequired()) {
                replacement.append(annotation.getTrailingLineSeparators());
            }
            replacement.append(layout.getLeadingWhitespace()).append(gap.getContent());
        }
        return replacement.toString();
    }
}
