package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.config.ConfigRoot;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonCompilationUnitUtilities;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtTypeMember;

@Slf4j
public final class Sorter {
    private final CtTypeMemberComparator comparator;

    public Sorter(final ConfigRoot config) {
        comparator = new CtTypeMemberComparator(config.getTypeMembersOrdering());
    }

    /**
     * Sorts the given SpoonASTModel.
     *
     * @param spoonAstModel the SpoonASTModel to sort
     * @return a SortingResult containing the sorted SpoonASTModel and statistics
     */
    public SortingResult sort(SpoonAstModel spoonAstModel) {
        log.debug("Sorting");

        StopWatch.TimedResult<SpoonAstModel> formatingResult = StopWatch.measure(() -> {
            SpoonCompilationUnitUtilities.getAllTypes(spoonAstModel.getWorkingCompilationUnit()).stream()
                    .filter(el -> el instanceof CtClass<?>)
                    .forEach((clazz) -> {
                        List<CtTypeMember> typesMembersForSorting = new ArrayList<>(clazz.getTypeMembers());
                        typesMembersForSorting.sort(comparator);
                        clazz.setTypeMembers(typesMembersForSorting);
                    });
            return spoonAstModel;
        });

        return new SortingResult(formatingResult.getResult(), new SortingStatistic(formatingResult.getNanos()));
    }
}
