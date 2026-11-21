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

/** Working AST, original source, and selected annotation orders used by the source printer. */
@Value
public class SpoonPrintingInput {

    /** Shared working AST in the declaration order to print. */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit compilationUnit;

    /** Original annotation groups. A group without a selected order prints in source order. */
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
     * Retains the supplied model's source context and read-only annotation orders. Producers must keep the
     * source groups, order map, and fragment lists unchanged.
     * @param sortedModel working model with the declaration order to print
     * @param sortedAnnotationSrcFragmentsByGroupSrcStart read-only map of changed annotation orders
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
