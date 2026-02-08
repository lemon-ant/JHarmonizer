package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PACKAGE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.FIELD;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.METHOD;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMatchMethod.EXACT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedNameMatcher;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class NaturalMemberGroupResolverTest {

    @Test
    void resolveNaturalGroups_memberMatchesParentAndChild_shouldSelectChildGroup() {
        // Given
        CtType<?> parsedMainType = parseMainTypeFromSource(
                "Dummy.java",
                """
                public class Dummy {
                    int alpha;
                    int beta;
                }
                """);
        CtTypeMember alphaFieldMember = findTypeMemberBySimpleNameOrFail(parsedMainType, "alpha");
        MemberDescriptor alphaFieldDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PACKAGE)
                .name("alpha")
                .build();
        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor = Map.of(alphaFieldMember, alphaFieldDescriptor);
        CompiledMemberGroup compiledRootGroup = compileRootGroup(createDeepestMatchWinsRootGroup());

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedNaturalGroups =
                NaturalMemberGroupResolver.resolveNaturalGroups(compiledRootGroup, typeMember2Descriptor);
        CompiledMemberGroup resolvedGroup = resolvedNaturalGroups.get(alphaFieldMember);

        // Then
        assertThat(resolvedNaturalGroups).hasSize(1);
        assertThat(resolvedGroup).isNotNull();
        assertThat(resolvedGroup.getName()).isEqualTo("AlphaFields");
    }

    @Test
    void resolveNaturalGroups_memberDoesNotMatchSpecificGroups_shouldFallbackToDefaultRuleGroup() {
        // Given
        CtType<?> parsedMainType = parseMainTypeFromSource(
                "WorkUnit.java",
                """
                public class WorkUnit {
                    int payload;
                    void doWork() {}
                }
                """);
        CtTypeMember payloadFieldMember = findTypeMemberBySimpleNameOrFail(parsedMainType, "payload");
        CtTypeMember doWorkMethodMember = findTypeMemberBySimpleNameOrFail(parsedMainType, "doWork");
        MemberDescriptor payloadFieldDescriptor = MemberDescriptor.builder()
                .memberKind(FIELD)
                .memberAccess(PACKAGE)
                .name("payload")
                .build();
        MemberDescriptor doWorkMethodDescriptor = MemberDescriptor.builder()
                .memberKind(METHOD)
                .memberAccess(PACKAGE)
                .name("doWork")
                .build();
        Map<CtTypeMember, MemberDescriptor> typeMember2Descriptor =
                Map.of(payloadFieldMember, payloadFieldDescriptor, doWorkMethodMember, doWorkMethodDescriptor);
        CompiledMemberGroup compiledRootGroup = compileRootGroup(createDefaultRuleFallbackRootGroup());

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedNaturalGroups =
                NaturalMemberGroupResolver.resolveNaturalGroups(compiledRootGroup, typeMember2Descriptor);
        CompiledMemberGroup payloadGroup = resolvedNaturalGroups.get(payloadFieldMember);
        CompiledMemberGroup doWorkGroup = resolvedNaturalGroups.get(doWorkMethodMember);

        // Then
        assertThat(resolvedNaturalGroups).hasSize(2);
        assertThat(payloadGroup).isNotNull();
        assertThat(doWorkGroup).isNotNull();
        assertThat(payloadGroup.getName()).isEqualTo("Default Rule");
        assertThat(doWorkGroup.getName()).isEqualTo("Methods");
    }

    private static CtType<?> parseMainTypeFromSource(String virtualFileName, String javaSourceCode) {
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of(virtualFileName), javaSourceCode);
        Optional<CtType<?>> mainType = spoonAstModel.getMainType();
        assertThat(mainType).as("Main type must be detected by Spoon for %s", virtualFileName).isPresent();
        return mainType.orElseThrow();
    }

    private static CtTypeMember findTypeMemberBySimpleNameOrFail(CtType<?> parsedType, String expectedSimpleName) {
        return parsedType.getTypeMembers().stream()
                .filter(typeMember -> expectedSimpleName.equals(typeMember.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Failed to find type member with simple name: " + expectedSimpleName + " in " + parsedType));
    }

    private static CompiledMemberGroup compileRootGroup(UnifiedMemberGroup unifiedRootGroup) {
        UnifiedConfig unifiedConfig = UnifiedConfig.builder()
                .formatting(new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR))
                .backupsEnabled(false)
                .headerLine(new UnifiedHeaderLine('#', 1))
                .topLevelTypesOrdering(createMinimalTopLevelTypesOrdering())
                .rootMemberGroup(unifiedRootGroup)
                .build();
        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(unifiedConfig);
        return compiledConfig.getRootMemberGroups().getFirst();
    }

    private static UnifiedTopLevelTypesOrdering createMinimalTopLevelTypesOrdering() {
        UnifiedTopLevelTypeSelector topLevelTypeSelector =
                new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.CLASS));
        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(true)
                .topLevelTypeSelectors(List.of(topLevelTypeSelector))
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();
    }

    private static UnifiedMemberGroup createDeepestMatchWinsRootGroup() {
        UnifiedMemberGroup alphaFieldsGroup = UnifiedMemberGroup.builder()
                .groupName("AlphaFields")
                .memberSubGroups(List.of())
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(
                        Set.of(),
                        Set.of(UnifiedMemberGroupRuleLine.builder()
                                .memberKind(FIELD)
                                .nameMatcher(new UnifiedNameMatcher(EXACT, "alpha"))
                                .build())))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();

        UnifiedMemberGroup fieldsGroupWithNestedOverride = UnifiedMemberGroup.builder()
                .groupName("Fields")
                .memberSubGroups(List.of(alphaFieldsGroup))
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(
                        Set.of(), Set.of(UnifiedMemberGroupRuleLine.builder().memberKind(FIELD).build())))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();

        UnifiedMemberGroup alsoMatchesFieldsButMustLoseByOrder = UnifiedMemberGroup.builder()
                .groupName("OtherFields")
                .memberSubGroups(List.of())
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(
                        Set.of(), Set.of(UnifiedMemberGroupRuleLine.builder().memberKind(FIELD).build())))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();

        return UnifiedMemberGroup.builder()
                .groupName("Root")
                .memberSubGroups(List.of(fieldsGroupWithNestedOverride, alsoMatchesFieldsButMustLoseByOrder))
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of()))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();
    }

    private static UnifiedMemberGroup createDefaultRuleFallbackRootGroup() {
        UnifiedMemberGroup methodsGroup = UnifiedMemberGroup.builder()
                .groupName("Methods")
                .memberSubGroups(List.of())
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(
                        Set.of(), Set.of(UnifiedMemberGroupRuleLine.builder().memberKind(METHOD).build())))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();

        UnifiedMemberGroup defaultRuleGroup = UnifiedMemberGroup.builder()
                .groupName("Default Rule")
                .memberSubGroups(List.of())
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of()))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();

        return UnifiedMemberGroup.builder()
                .groupName("Root")
                .memberSubGroups(List.of(methodsGroup, defaultRuleGroup))
                .selectorBlock(new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of()))
                .separator(UnifiedSeparator.NONE)
                .sortKeys(List.of(UnifiedSortKey.PRESERVE))
                .build();
    }
}
