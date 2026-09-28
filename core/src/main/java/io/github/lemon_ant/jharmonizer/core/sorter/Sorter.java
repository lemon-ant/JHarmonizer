// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.findRelocations;
import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.hasReorderedAnnotations;
import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.hasReorderedDeclarations;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public final class Sorter {

    // TODO Try to remove this field and make the class static util
    private final SpoonSorter spoonSorter;

    /**
     * Creates a new Sorter.
     * @param config the compiled configuration to use
     */
    public Sorter(CompiledConfig config) {
        this.spoonSorter = new SpoonSorter(config);
    }

    /**
     * Reorders declarations in the working AST and returns a model with immutable sorted annotation groups.
     *
     * @param spoonAstModel the SpoonASTModel to sort
     * @return the model for serialization, member diagnostics, change flags, and sorting statistics
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel) {
        log.trace("Sorting {}", spoonAstModel.getPath());
        StopWatch.TimedResult<SortedContent> sortingResult = StopWatch.measure(() -> {
            SpoonAstModel sortedSpoonAstModel = spoonSorter.sortCompilationUnitRecursively(spoonAstModel);
            // Compute diagnostics before handing off the shared AST, which a later sort can reorder again.
            // TODO Annotations: The original idea was that the sorting algorithm can natively report about relocations
            List<MemberRelocation> memberRelocations = findRelocations(
                    sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit());
            boolean annotationsReordered = hasReorderedAnnotations(sortedSpoonAstModel);
            // The report omits untracked members and invalid source positions, so it cannot replace this order check.
            boolean membersReordered = annotationsReordered
                    || hasReorderedDeclarations(
                            sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit());
            return new SortedContent(annotationsReordered, memberRelocations, membersReordered, sortedSpoonAstModel);
        });
        SortedContent sortedContent = sortingResult.getResult();

        return new SortingResult(
                sortedContent.isAnnotationsReordered(),
                sortedContent.getMemberRelocations(),
                sortedContent.isMembersReordered(),
                sortedContent.getSortedSpoonAstModel(),
                new SortingStatistic(sortingResult.getNanos()));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SortedContent {
        boolean annotationsReordered;

        @NonNull
        List<MemberRelocation> memberRelocations;

        boolean membersReordered;

        @NonNull
        SpoonAstModel sortedSpoonAstModel;
    }
}
