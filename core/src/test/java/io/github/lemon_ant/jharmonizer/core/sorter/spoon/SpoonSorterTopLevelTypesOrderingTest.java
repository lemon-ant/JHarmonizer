package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpoonSorterTopLevelTypesOrderingTest {

    @Test
    void sortCompilationUnitRecursively_mainTypeFirstAndGroupedAlpha_reorderTopLevelTypes() {
        // Given
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("MainTypeFirstFixture.java"), """
                enum ZebraKind {
                    ONE
                }

                @interface Marker {}

                interface AlphaContract {}

                record BravoRecord(int value) {}

                class AlphaHelper {
                    static String message() {
                        return "ok";
                    }
                }

                public class MainTypeFirstFixture {
                    public static void main(String[] args) {
                        System.out.println(new BravoRecord(1).value()
                                + AlphaHelper.message()
                                + AlphaContract.class.getSimpleName()
                                + ZebraKind.ONE.name()
                                + Marker.class.getSimpleName());
                    }
                }
                """);
        SpoonSorter spoonSorter = new SpoonSorter(createCompiledConfig(UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(true)
                .topLevelTypeSelectors(List.of(
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.CLASS, UnifiedTypeKind.RECORD)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.INTERFACE)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.ENUM)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.ANNOTATION))))
                .orderingRules(List.of(UnifiedOrderingRule.VISIBILITY_DESC, UnifiedOrderingRule.ALPHA))
                .build()));

        // When
        spoonSorter.sortCompilationUnitRecursively(spoonAstModel.getCompilationUnit());

        // Then
        assertThat(spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                        .map(type -> type.getSimpleName())
                        .toList())
                .containsExactly(
                        "MainTypeFirstFixture", "AlphaHelper", "BravoRecord", "AlphaContract", "ZebraKind", "Marker");
    }

    @Test
    void sortCompilationUnitRecursively_groupedPreserveWithoutMainTypeFirst_keepOriginalOrderInsideGroups() {
        // Given
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("PreserveGroupedFixture.java"), """
                class DeltaClass {}

                interface FirstInterface {}

                @interface FirstAnnotation {}

                enum BetaEnum {
                    ONE
                }

                record GammaRecord(int value) {}

                class AlphaClass {}

                interface SecondInterface {}

                @interface SecondAnnotation {}
                """);
        SpoonSorter spoonSorter = new SpoonSorter(createCompiledConfig(UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(false)
                .topLevelTypeSelectors(List.of(
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.INTERFACE, UnifiedTypeKind.ANNOTATION)),
                        new UnifiedTopLevelTypeSelector(
                                Set.of(UnifiedTypeKind.CLASS, UnifiedTypeKind.RECORD, UnifiedTypeKind.ENUM))))
                .orderingRules(List.of(UnifiedOrderingRule.PRESERVE))
                .build()));

        // When
        spoonSorter.sortCompilationUnitRecursively(spoonAstModel.getCompilationUnit());

        // Then
        assertThat(spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                        .map(type -> type.getSimpleName())
                        .toList())
                .containsExactly(
                        "FirstInterface",
                        "FirstAnnotation",
                        "SecondInterface",
                        "SecondAnnotation",
                        "DeltaClass",
                        "BetaEnum",
                        "GammaRecord",
                        "AlphaClass");
    }

    private static CompiledConfig createCompiledConfig(UnifiedTopLevelTypesOrdering topLevelTypesOrdering) {
        UnifiedMemberGroup rootMemberGroup = UnifiedMemberGroup.builder()
                .groupName("Root")
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRule(UnifiedOrderingRule.PRESERVE)
                .build();
        UnifiedConfig unifiedConfig = UnifiedConfig.builder()
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .formatting(new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR))
                .backupsEnabled(false)
                .headerLine(new UnifiedHeaderLine('-', 0))
                .rootMemberGroup(rootMemberGroup)
                .build();
        return Unified2CompiledModelCompiler.compile(unifiedConfig);
    }
}
