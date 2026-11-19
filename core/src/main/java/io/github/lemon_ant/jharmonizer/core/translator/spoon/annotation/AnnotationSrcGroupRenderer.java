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

/** Joins scanner-prepared text pieces in a selected annotation order without changing source models. */
@UtilityClass
public class AnnotationSrcGroupRenderer {

    /**
     * Renders a permutation of existing source fragments using the scanner's prepared gap layouts.
     * Layout selection never reads source text or computes indentation or separator requirements.
     * @param sourceGroup immutable group prepared by the scanner
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
                // Stable source offsets also recognize fragments recreated during dependency-key preparation.
                replacement.append(gap.getSrcCode());
                continue;
            }
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
