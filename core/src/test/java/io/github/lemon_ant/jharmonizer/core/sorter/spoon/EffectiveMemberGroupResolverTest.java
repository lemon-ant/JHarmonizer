package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphTestCreator.createDeclarationDependencyEdge;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphTestCreator.createGraphWithDeclarationDependencyEdges;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Review
class EffectiveMemberGroupResolverTest {

    private static final URL EFFECTIVE_GROUP_FIXTURE_URL = resolveFixtureUrl(
            "/test-cases/core/sorter/spoon/effective-group-resolution/valid/EffectiveGroupResolutionFixture.java");
    private static final CtType<?> PARSED_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(EFFECTIVE_GROUP_FIXTURE_URL);

    private static final CtTypeMember PROVIDER_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "PROVIDER");
    private static final CtTypeMember DIRECT_DEPENDENT_FIELD_MEMBER = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
            PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "DIRECT_DEPENDENT");
    private static final CtTypeMember TRANSITIVE_PROVIDER_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "TRANSITIVE_PROVIDER");
    private static final CtTypeMember TRANSITIVE_DEPENDENT_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "TRANSITIVE_DEPENDENT");
    private static final CtTypeMember UNRELATED_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "UNRELATED");
    private static final CtTypeMember EARLY_ROOT_DEPENDENT_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    PARSED_FIXTURE_MAIN_TYPE.getTypeMembers(), "EARLY_ROOT_DEPENDENT");

    @Test
    void resolveEffectiveGroups_transitiveDependentInEarlierGroup_shouldPullProviderToThatGroup() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup middleGroup = createTrivialMemberGroup("middle", false, 20);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 30);
        CompiledMemberGroup unrelatedEarlierGroup = createTrivialMemberGroup("unrelated", false, 5);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                PROVIDER_FIELD_MEMBER, lateGroup,
                DIRECT_DEPENDENT_FIELD_MEMBER, middleGroup,
                TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                UNRELATED_FIELD_MEMBER, unrelatedEarlierGroup,
                EARLY_ROOT_DEPENDENT_FIELD_MEMBER, lateGroup);
        MemberDependencyGraph memberDependencyGraph = createGraphWithDeclarationDependencyEdges(List.of(
                createDeclarationDependencyEdge(PROVIDER_FIELD_MEMBER, DIRECT_DEPENDENT_FIELD_MEMBER),
                createDeclarationDependencyEdge(PROVIDER_FIELD_MEMBER, TRANSITIVE_PROVIDER_FIELD_MEMBER),
                createDeclarationDependencyEdge(TRANSITIVE_PROVIDER_FIELD_MEMBER, TRANSITIVE_DEPENDENT_FIELD_MEMBER)));

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThat(resolvedEffectiveGroups.get(PROVIDER_FIELD_MEMBER)).isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(DIRECT_DEPENDENT_FIELD_MEMBER)).isSameAs(middleGroup);
        assertThat(resolvedEffectiveGroups.get(TRANSITIVE_PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(TRANSITIVE_DEPENDENT_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(UNRELATED_FIELD_MEMBER)).isSameAs(unrelatedEarlierGroup);
        assertThatThrownBy(() -> resolvedEffectiveGroups.put(PROVIDER_FIELD_MEMBER, lateGroup))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolveEffectiveGroups_dependentInEarlierRootGroup_shouldPullAcrossRootGroups() {
        // Given
        CompiledMemberGroup earlyRootGroup = createTrivialMemberGroup("root-alpha", false, 1);
        CompiledMemberGroup lateRootGroup = createTrivialMemberGroup("root-bravo", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                PROVIDER_FIELD_MEMBER, lateRootGroup,
                EARLY_ROOT_DEPENDENT_FIELD_MEMBER, earlyRootGroup,
                DIRECT_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                TRANSITIVE_PROVIDER_FIELD_MEMBER, lateRootGroup,
                TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                UNRELATED_FIELD_MEMBER, lateRootGroup);
        MemberDependencyGraph memberDependencyGraph = createGraphWithDeclarationDependencyEdges(
                List.of(createDeclarationDependencyEdge(PROVIDER_FIELD_MEMBER, EARLY_ROOT_DEPENDENT_FIELD_MEMBER)));

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThat(resolvedEffectiveGroups.get(PROVIDER_FIELD_MEMBER)).isSameAs(earlyRootGroup);
        assertThat(resolvedEffectiveGroups.get(EARLY_ROOT_DEPENDENT_FIELD_MEMBER))
                .isSameAs(earlyRootGroup);
    }

    @Test
    void resolveEffectiveGroups_inputMappingOrderDifferent_shouldResolveSameGroups() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> firstNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(PROVIDER_FIELD_MEMBER, EARLY_ROOT_DEPENDENT_FIELD_MEMBER),
                Map.of(
                        PROVIDER_FIELD_MEMBER, lateGroup,
                        EARLY_ROOT_DEPENDENT_FIELD_MEMBER, earlyGroup,
                        DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                        TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                        TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateGroup,
                        UNRELATED_FIELD_MEMBER, lateGroup));
        Map<CtTypeMember, CompiledMemberGroup> secondNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(EARLY_ROOT_DEPENDENT_FIELD_MEMBER, PROVIDER_FIELD_MEMBER),
                Map.of(
                        PROVIDER_FIELD_MEMBER, lateGroup,
                        EARLY_ROOT_DEPENDENT_FIELD_MEMBER, earlyGroup,
                        DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                        TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                        TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateGroup,
                        UNRELATED_FIELD_MEMBER, lateGroup));
        MemberDependencyGraph memberDependencyGraph = createGraphWithDeclarationDependencyEdges(
                List.of(createDeclarationDependencyEdge(PROVIDER_FIELD_MEMBER, EARLY_ROOT_DEPENDENT_FIELD_MEMBER)));

        // When
        Map<CtTypeMember, CompiledMemberGroup> firstResolvedMapping =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(firstNaturalGroupMapping, memberDependencyGraph);
        Map<CtTypeMember, CompiledMemberGroup> secondResolvedMapping =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(secondNaturalGroupMapping, memberDependencyGraph);

        // Then
        assertThat(firstResolvedMapping).isEqualTo(secondResolvedMapping);
        assertThat(firstResolvedMapping.get(PROVIDER_FIELD_MEMBER)).isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_missingNaturalGroupForDependent_shouldThrow() {
        // Given
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                PROVIDER_FIELD_MEMBER, lateGroup,
                DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                UNRELATED_FIELD_MEMBER, lateGroup,
                EARLY_ROOT_DEPENDENT_FIELD_MEMBER, lateGroup);
        MemberDependencyGraph memberDependencyGraph = createGraphWithDeclarationDependencyEdges(
                List.of(createDeclarationDependencyEdge(PROVIDER_FIELD_MEMBER, TRANSITIVE_DEPENDENT_FIELD_MEMBER)));

        // When
        org.assertj.core.api.ThrowableAssert.ThrowingCallable resolveAction =
                () -> EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThatThrownBy(resolveAction)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missingMember=")
                .hasMessageContaining("providerMember=");
    }

    private static Map<CtTypeMember, CompiledMemberGroup> createNaturalGroupMappingInOrder(
            List<CtTypeMember> insertionOrder, Map<CtTypeMember, CompiledMemberGroup> naturalGroupsByMember) {
        LinkedHashMap<CtTypeMember, CompiledMemberGroup> orderedMapping = new LinkedHashMap<>();
        insertionOrder.forEach(typeMember -> orderedMapping.put(typeMember, naturalGroupsByMember.get(typeMember)));
        naturalGroupsByMember.forEach(orderedMapping::putIfAbsent);
        return orderedMapping;
    }

    @Deprecated
    private static URL resolveFixtureUrl(String classpathAbsolutePath) {
        URL resolvedUrl = EffectiveMemberGroupResolverTest.class.getResource(classpathAbsolutePath);
        if (resolvedUrl == null) {
            throw new IllegalStateException("Classpath resource not found: " + classpathAbsolutePath);
        }
        return resolvedUrl;
    }
}
