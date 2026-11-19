// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

/**
 * Source-printing context with independently selected annotation orders.
 * Retains the shared working AST and existing read-only source and ordering views without copying source models.
 */
@Value
public class SpoonPrintingInput {

    /** Shared working AST in its current declaration order, including any in-place sorting. */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit compilationUnit;

    /** Original scanner groups; missing sorted-order entries retain their source order. */
    @NonNull
    List<AnnotationSrcGroup> originalAnnotationSrcGroups;

    @NonNull
    Path path;

    /** Read-only sorted fragment orders for changed groups, indexed by original group start. */
    @NonNull
    Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart;

    @NonNull
    Set<CtType<?>> sortingSkippedTypes;

    @NonNull
    String srcCode;

    /**
     * Extracts the printer's source context and retains already read-only sorting output.
     * Producers must not modify handed-off source groups, order maps, or their fragment lists.
     * @param sortedModel working model after sorting, whose shared AST has the declaration order to print
     * @param sortedAnnotationSrcFragmentsByGroupSrcStart read-only changed orders, or an empty map for source order
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification =
                    "The sorter hands off an unmodifiable map and unmodifiable fragment lists; retain that view.")
    public SpoonPrintingInput(
            @NonNull SpoonAstModel sortedModel,
            @NonNull Map<Integer, List<AnnotationSrcFragment>> sortedAnnotationSrcFragmentsByGroupSrcStart) {
        compilationUnit = sortedModel.getCompilationUnit();
        originalAnnotationSrcGroups = sortedModel.getOriginalAnnotationSrcGroups();
        path = sortedModel.getPath();
        this.sortedAnnotationSrcFragmentsByGroupSrcStart = sortedAnnotationSrcFragmentsByGroupSrcStart;
        sortingSkippedTypes = sortedModel.getOptOuts().getSortingSkippedTypes();
        srcCode = sortedModel.getSrcCode();
    }
}
