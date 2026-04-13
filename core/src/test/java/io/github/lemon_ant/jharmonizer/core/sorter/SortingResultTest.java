package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SortingResultTest {

    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true, false);

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        SortingResult sortingResult = buildSortingResult("class A { int a; }");

        // When / Then
        assertThat(sortingResult).isEqualTo(sortingResult);
    }

    @Test
    void equals_nullObject_returnsFalse() {
        // Given
        SortingResult sortingResult = buildSortingResult("class A { int a; }");

        // When / Then
        assertThat(sortingResult).isNotEqualTo(null);
    }

    @Test
    void equals_differentClass_returnsFalse() {
        // Given
        SortingResult sortingResult = buildSortingResult("class A { int a; }");

        // When / Then
        assertThat(sortingResult).isNotEqualTo("not a sorting result");
    }

    @Test
    void equals_sameModelAndStatistic_returnsTrue() {
        // Given
        SpoonAstModel model = parseModel("class B { int b; }");
        SortingStatistic statistic = new SortingStatistic(1000L);
        SortingResult first = new SortingResult(model, statistic);
        SortingResult second = new SortingResult(model, statistic);

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentStatistic_returnsFalse() {
        // Given
        SpoonAstModel model = parseModel("class C { int c; }");
        SortingResult first = new SortingResult(model, new SortingStatistic(1000L));
        SortingResult second = new SortingResult(model, new SortingStatistic(9999L));

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        SpoonAstModel model = parseModel("class D { int d; }");
        SortingStatistic statistic = new SortingStatistic(500L);
        SortingResult first = new SortingResult(model, statistic);
        SortingResult second = new SortingResult(model, statistic);

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private static SortingResult buildSortingResult(String srcCode) {
        SpoonAstModel model = parseModel(srcCode);
        return new SortingResult(model, new SortingStatistic(100L));
    }

    private static SpoonAstModel parseModel(String srcCode) {
        return SrcAstTranslator.parse(createSrcFile(srcCode, Path.of("Sample.java")), DEFAULT_PRINTER_CONFIG)
                .getSpoonAstModel();
    }
}
