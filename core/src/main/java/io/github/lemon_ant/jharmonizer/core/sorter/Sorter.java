// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
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
     * @return the model to use for subsequent serialization, together with sorting statistics
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel) {
        log.trace("Sorting {}", spoonAstModel.getPath());
        StopWatch.TimedResult<SpoonAstModel> sortingResult = StopWatch.measure(
                () -> spoonAstModel.withAnnotationSrcGroups(spoonSorter.sortCompilationUnitRecursively(
                        spoonAstModel.getCompilationUnit(),
                        spoonAstModel.getOptOuts().getSortingSkippedTypes(),
                        spoonAstModel.getAnnotationSrcGroups())));

        return new SortingResult(sortingResult.getResult(), new SortingStatistic(sortingResult.getNanos()));
    }
}
