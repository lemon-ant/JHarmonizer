package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

class GroupMembersOrdererSortKeysTest {

    @Test
    void orderMembersInsideGroups_sortKeyPreserve_keepOriginalSourceOrder() {
        // Given
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("preserve", false, List.of(SortKey.PRESERVE));
        CtTypeMember publicFieldFirstMember = requireFixtureMemberBySimpleName("publicFieldFirst");
        CtTypeMember protectedFieldMember = requireFixtureMemberBySimpleName("protectedField");
        CtTypeMember privateFieldMember = requireFixtureMemberBySimpleName("privateField");
        CtTypeMember alphaFieldMember = requireFixtureMemberBySimpleName("alphaField");
        CtTypeMember calculateNoArgsMethodMember =
                requireFixtureMethodByNameAndParameterQualifiedNames("calculate", List.of());
        List<CtTypeMember> inputMembers = List.of(
                calculateNoArgsMethodMember,
                alphaFieldMember,
                privateFieldMember,
                protectedFieldMember,
                publicFieldFirstMember);
        MemberGroupBlock inputBlock = new MemberGroupBlock(compiledMemberGroup, inputMembers);
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                publicFieldFirstMember, compiledMemberGroup,
                protectedFieldMember, compiledMemberGroup,
                privateFieldMember, compiledMemberGroup,
                alphaFieldMember, compiledMemberGroup,
                calculateNoArgsMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks).hasSize(1);
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        publicFieldFirstMember,
                        protectedFieldMember,
                        privateFieldMember,
                        alphaFieldMember,
                        calculateNoArgsMethodMember);
    }

    @Test
    void orderMembersInsideGroups_sortKeyAlphaWithFields_orderAlphabetically() {
        // Given
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("alpha-fields", false, List.of(SortKey.ALPHA));
        CtTypeMember zuluFieldMember = requireFixtureMemberBySimpleName("zuluField");
        CtTypeMember alphaFieldMember = requireFixtureMemberBySimpleName("alphaField");
        CtTypeMember bravoFieldMember = requireFixtureMemberBySimpleName("bravoField");
        MemberGroupBlock inputBlock =
                new MemberGroupBlock(compiledMemberGroup, List.of(zuluFieldMember, bravoFieldMember, alphaFieldMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                zuluFieldMember, compiledMemberGroup,
                alphaFieldMember, compiledMemberGroup,
                bravoFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(alphaFieldMember, bravoFieldMember, zuluFieldMember);
    }

    @Test
    void orderMembersInsideGroups_sortKeyAlphaWithOverloads_orderDeterministically() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-overloads", false, List.of(SortKey.ALPHA));
        CtMethod<?> calculateStringMethodMember =
                requireFixtureMethodByNameAndParameterQualifiedNames("calculate", List.of("java.lang.String"));
        CtMethod<?> calculateNoArgsMethodMember =
                requireFixtureMethodByNameAndParameterQualifiedNames("calculate", List.of());
        CtMethod<?> calculateIntMethodMember =
                requireFixtureMethodByNameAndParameterQualifiedNames("calculate", List.of("int"));
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(calculateStringMethodMember, calculateIntMethodMember, calculateNoArgsMethodMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                calculateStringMethodMember, compiledMemberGroup,
                calculateNoArgsMethodMember, compiledMemberGroup,
                calculateIntMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers().stream()
                        .map(member -> (CtMethod<?>) member)
                        .map(GroupMembersOrdererSortKeysTest::extractParameterTypeQualifiedNames)
                        .toList())
                .containsExactly(List.of(), List.of("int"), List.of("java.lang.String"));
    }

    @Test
    void orderMembersInsideGroups_sortKeyVisibilityAsc_orderByVisibilityRank() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "visibility-asc", false, List.of(SortKey.VISIBILITY_ASC));
        CtTypeMember publicFieldFirstMember = requireFixtureMemberBySimpleName("publicFieldFirst");
        CtTypeMember publicFieldSecondMember = requireFixtureMemberBySimpleName("publicFieldSecond");
        CtTypeMember protectedFieldMember = requireFixtureMemberBySimpleName("protectedField");
        CtTypeMember packagePrivateFieldMember = requireFixtureMemberBySimpleName("packagePrivateField");
        CtTypeMember privateFieldMember = requireFixtureMemberBySimpleName("privateField");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(
                        privateFieldMember,
                        protectedFieldMember,
                        publicFieldSecondMember,
                        packagePrivateFieldMember,
                        publicFieldFirstMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                publicFieldFirstMember, compiledMemberGroup,
                publicFieldSecondMember, compiledMemberGroup,
                protectedFieldMember, compiledMemberGroup,
                packagePrivateFieldMember, compiledMemberGroup,
                privateFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        publicFieldFirstMember,
                        publicFieldSecondMember,
                        protectedFieldMember,
                        packagePrivateFieldMember,
                        privateFieldMember);
    }

    @Test
    void orderMembersInsideGroups_sortKeyVisibilityDesc_orderByVisibilityRankDescending() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "visibility-desc", false, List.of(SortKey.VISIBILITY_DESC));
        CtTypeMember publicFieldFirstMember = requireFixtureMemberBySimpleName("publicFieldFirst");
        CtTypeMember publicFieldSecondMember = requireFixtureMemberBySimpleName("publicFieldSecond");
        CtTypeMember protectedFieldMember = requireFixtureMemberBySimpleName("protectedField");
        CtTypeMember packagePrivateFieldMember = requireFixtureMemberBySimpleName("packagePrivateField");
        CtTypeMember privateFieldMember = requireFixtureMemberBySimpleName("privateField");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(
                        protectedFieldMember,
                        publicFieldSecondMember,
                        packagePrivateFieldMember,
                        privateFieldMember,
                        publicFieldFirstMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                publicFieldFirstMember, compiledMemberGroup,
                publicFieldSecondMember, compiledMemberGroup,
                protectedFieldMember, compiledMemberGroup,
                packagePrivateFieldMember, compiledMemberGroup,
                privateFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        privateFieldMember,
                        packagePrivateFieldMember,
                        protectedFieldMember,
                        publicFieldFirstMember,
                        publicFieldSecondMember);
    }

    @Test
    void orderMembersInsideGroups_keepAccessorsTogetherEnabled_keepAccessorPairContiguous() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-enabled", true, List.of(SortKey.ALPHA));
        CtTypeMember getValueMethodMember = requireFixtureMemberBySimpleName("getValue");
        CtTypeMember middleMethodMember = requireFixtureMemberBySimpleName("middleMethod");
        CtTypeMember setValueMethodMember = requireFixtureMemberBySimpleName("setValue");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(middleMethodMember, setValueMethodMember, getValueMethodMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                getValueMethodMember, compiledMemberGroup,
                setValueMethodMember, compiledMemberGroup,
                middleMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(getValueMethodMember, setValueMethodMember, middleMethodMember);
    }

    @Test
    void orderMembersInsideGroups_keepAccessorsTogetherDisabled_allowAlphaInterleaving() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-disabled", false, List.of(SortKey.ALPHA));
        CtTypeMember getValueMethodMember = requireFixtureMemberBySimpleName("getValue");
        CtTypeMember middleMethodMember = requireFixtureMemberBySimpleName("middleMethod");
        CtTypeMember setValueMethodMember = requireFixtureMemberBySimpleName("setValue");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(middleMethodMember, setValueMethodMember, getValueMethodMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                getValueMethodMember, compiledMemberGroup,
                setValueMethodMember, compiledMemberGroup,
                middleMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(getValueMethodMember, middleMethodMember, setValueMethodMember);
    }


    @Test
    void orderMembersInsideGroups_sameRepresentativeFieldAndInitializer_fieldComesFirst() {
        // Given
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("alpha-static-tie", false, List.of(SortKey.ALPHA));

        CtTypeMember readFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS, "READ");
        CtTypeMember valueFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS, "VALUE");
        CtTypeMember staticInitializerMember = Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS.stream()
                .filter(CtAnonymousExecutable.class::isInstance)
                .filter(typeMember -> typeMember.getModifiers().contains(ModifierKind.STATIC))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Static initializer was not found in tie fixture"));

        MemberGroupBlock inputBlock =
                new MemberGroupBlock(compiledMemberGroup, List.of(staticInitializerMember, readFieldMember, valueFieldMember));

        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                readFieldMember, compiledMemberGroup,
                valueFieldMember, compiledMemberGroup,
                staticInitializerMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(valueFieldMember, staticInitializerMember, readFieldMember);
    }

    private static CtTypeMember requireFixtureMemberBySimpleName(String expectedSimpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.FIXTURE_MEMBERS, expectedSimpleName);
    }

    private static CtMethod<?> requireFixtureMethodByNameAndParameterQualifiedNames(
            String expectedMethodName, List<String> expectedParameterTypeQualifiedNames) {
        return Constants.FIXTURE_MEMBERS.stream()
                .filter(typeMember -> typeMember instanceof CtMethod<?>)
                .map(typeMember -> (CtMethod<?>) typeMember)
                .filter(method -> expectedMethodName.equals(method.getSimpleName()))
                .filter(method ->
                        extractParameterTypeQualifiedNames(method).equals(expectedParameterTypeQualifiedNames))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No CtMethod found for methodName=%s, parameterTypes=%s. Available overloads: %s"
                                .formatted(
                                        expectedMethodName,
                                        expectedParameterTypeQualifiedNames,
                                        Constants.FIXTURE_MEMBERS.stream()
                                                .filter(typeMember -> typeMember instanceof CtMethod<?>)
                                                .map(typeMember -> (CtMethod<?>) typeMember)
                                                .filter(method -> expectedMethodName.equals(method.getSimpleName()))
                                                .map(method -> extractParameterTypeQualifiedNames(method)
                                                        .toString())
                                                .sorted()
                                                .toList())));
    }

    private static List<String> extractParameterTypeQualifiedNames(CtMethod<?> method) {
        return method.getParameters().stream()
                .map(parameter -> parameter.getType().getQualifiedName())
                .toList();
    }

    private static final class Constants {

        private static final String FIELD_INITIALIZER_TIE_FIXTURE_CLASSPATH_RESOURCE =
                "/test-cases/core/sorter/spoon/group-member-ordering/valid/GroupMemberOrderingFieldInitializerTieFixture.java";
        private static final URL FIELD_INITIALIZER_TIE_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererSortKeysTest.class.getResource(FIELD_INITIALIZER_TIE_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> FIELD_INITIALIZER_TIE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIELD_INITIALIZER_TIE_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS =
                streamExplicitSourceTypeMembers(FIELD_INITIALIZER_TIE_FIXTURE_MAIN_TYPE).toList();

        private static final String FIXTURE_CLASSPATH_RESOURCE =
                "/test-cases/core/sorter/spoon/group-member-ordering/valid/GroupMemberOrderingFixture.java";
        private static final URL FIXTURE_RESOURCE_URL =
                GroupMembersOrdererSortKeysTest.class.getResource(FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> FIXTURE_MEMBERS =
                streamExplicitSourceTypeMembers(FIXTURE_MAIN_TYPE).toList();

        private Constants() {}
    }
}
