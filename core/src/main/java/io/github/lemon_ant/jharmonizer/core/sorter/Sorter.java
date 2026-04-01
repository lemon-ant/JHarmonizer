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
     * Sorts the given SpoonASTModel.
     *
     * @param spoonAstModel the SpoonASTModel to sort
     * @return a SortingResult containing the sorted SpoonASTModel and statistics
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel) {
        log.debug("Sorting {}", spoonAstModel.getPath());
        StopWatch.TimedResult<SpoonAstModel> sortingResult = StopWatch.measure(() -> {
            spoonSorter.sortCompilationUnitRecursively(
                    spoonAstModel.getCompilationUnit(),
                    spoonAstModel.getOptOuts().getSortingSkippedTypes());
            return spoonAstModel;
        });

        return new SortingResult(sortingResult.getResult(), new SortingStatistic(sortingResult.getNanos()));
    }
}
