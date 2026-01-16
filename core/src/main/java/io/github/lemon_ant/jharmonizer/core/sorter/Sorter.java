package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Sorter {
    private final CompiledConfig config;

    public Sorter(final CompiledConfig config) {
        this.config = config;
    }

    /**
     * Sorts the given SpoonASTModel.
     *
     * @param spoonAstModel the SpoonASTModel to sort
     * @return a SortingResult containing the sorted SpoonASTModel and statistics
     */
    public SortingResult sort(SpoonAstModel spoonAstModel) {
        StopWatch.TimedResult<SpoonAstModel> sortingResult = StopWatch.measure(() -> {
            SpoonSorter.sortCompilationUnitRecursively(config, spoonAstModel.getCompilationUnit());
            return spoonAstModel;
        });

        return new SortingResult(sortingResult.getResult(), new SortingStatistic(sortingResult.getNanos()));
    }
}
