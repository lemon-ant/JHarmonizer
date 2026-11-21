// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter.SpoonSortingResult;
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
     * Sorts declarations and annotations and includes member relocations when declarations change.
     *
     * @param spoonAstModel freshly parsed source context
     * @return sorting result with member diagnostics
     * @throws IllegalStateException if the shared AST has already been submitted for sorting
     */
    @NonNull
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel) {
        return sort(spoonAstModel, true);
    }

    /**
     * Sorts declarations in place and selects changed annotation orders separately.
     * @param spoonAstModel freshly parsed source context
     * @param collectMemberRelocations whether to report relocations when declarations change
     * @return sorting result with timing for sorting and any requested diagnostics
     * @throws IllegalStateException if the shared AST has already been submitted for sorting
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public SortingResult sort(@NonNull SpoonAstModel spoonAstModel, boolean collectMemberRelocations) {
        log.trace("Sorting {}", spoonAstModel.getPath());
        StopWatch.TimedResult<SortedContent> sortingResult = StopWatch.measure(() -> {
            SpoonSortingResult spoonSortingResult = spoonSorter.sortCompilationUnitRecursively(spoonAstModel);
            SpoonAstModel sortedSpoonAstModel = spoonSortingResult.getSortedSpoonAstModel();
            List<MemberRelocation> memberRelocations = collectMemberRelocations
                            && spoonSortingResult.isMembersReordered()
                    ? findRelocations(
                            sortedSpoonAstModel.getOriginalMemberOrder(), sortedSpoonAstModel.getCompilationUnit())
                    : List.of();
            return new SortedContent(memberRelocations, spoonSortingResult);
        });
        SortedContent sortedContent = sortingResult.getResult();

        return new SortingResult(
                sortedContent.getMemberRelocations(),
                sortedContent.getSpoonSortingResult().isMembersReordered(),
                sortedContent.getSpoonSortingResult().getSortedAnnotationSrcFragmentsByGroupSrcStart(),
                sortedContent.getSpoonSortingResult().getSortedSpoonAstModel(),
                new SortingStatistic(sortingResult.getNanos()));
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SortedContent {

        @NonNull
        List<MemberRelocation> memberRelocations;

        @NonNull
        SpoonSortingResult spoonSortingResult;
    }
}
