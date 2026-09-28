// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.spoon.MemberRelocation;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
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
     * @return the model for serialization, unmodifiable member diagnostics, and sorting statistics
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel) {
        log.trace("Sorting {}", spoonAstModel.getPath());
        StopWatch.TimedResult<SpoonAstModel> sortingResult =
                StopWatch.measure(() -> spoonSorter.sortCompilationUnitRecursively(spoonAstModel));
        SpoonAstModel sortedSpoonAstModel = sortingResult.getResult();
        // Compute diagnostics before handing off the shared AST, which a later sort can reorder again.
        // TODO The original idea was that the sorting algorithm can nativelly report about relocations
        StopWatch.TimedResult<List<MemberRelocation>> relocationResult = StopWatch.measure(() -> findRelocations(
                sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit()));

        return new SortingResult(
                relocationResult.getResult(),
                sortedSpoonAstModel,
                new SortingStatistic(sortingResult.getNanos() + relocationResult.getNanos()));
    }
}
