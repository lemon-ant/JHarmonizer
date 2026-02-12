package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

// TODO Review
class EffectiveMemberGroupResolverTest {

    @Test
    void resolveEffectiveGroups_transitiveDependentInEarlierGroup_shouldPullProviderToThatGroup() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup middleGroup = createTrivialMemberGroup("middle", false, 20);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 30);
        CompiledMemberGroup unrelatedEarlierGroup = createTrivialMemberGroup("unrelated", false, 5);
        CompiledMemberGroup initializerBlockLateGroup = createTrivialMemberGroup("initializer-late", false, 1_000);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                Constants.PROVIDER_FIELD_MEMBER, lateGroup,
                Constants.DIRECT_DEPENDENT_FIELD_MEMBER, middleGroup,
                Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                Constants.UNRELATED_FIELD_MEMBER, unrelatedEarlierGroup,
                Constants.STATIC_INITIALIZER_BLOCK_MEMBER, initializerBlockLateGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalMemberGroup);

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThat(resolvedEffectiveGroups.get(Constants.PROVIDER_FIELD_MEMBER)).isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.DIRECT_DEPENDENT_FIELD_MEMBER))
                .isSameAs(middleGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.UNRELATED_FIELD_MEMBER))
                .isSameAs(unrelatedEarlierGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.STATIC_INITIALIZER_BLOCK_MEMBER))
                .isSameAs(initializerBlockLateGroup);
        assertThatThrownBy(() -> resolvedEffectiveGroups.put(Constants.PROVIDER_FIELD_MEMBER, lateGroup))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolveEffectiveGroups_dependentInEarlierRootGroup_shouldPullAcrossRootGroups() {
        // Given
        CompiledMemberGroup earlyRootGroup = createTrivialMemberGroup("root-alpha", false, 1);
        CompiledMemberGroup lateRootGroup = createTrivialMemberGroup("root-bravo", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                Constants.PROVIDER_FIELD_MEMBER, lateRootGroup,
                Constants.DIRECT_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateRootGroup,
                Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                Constants.UNRELATED_FIELD_MEMBER, lateRootGroup,
                Constants.STATIC_INITIALIZER_BLOCK_MEMBER, earlyRootGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalMemberGroup);

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThat(resolvedEffectiveGroups.get(Constants.PROVIDER_FIELD_MEMBER)).isSameAs(earlyRootGroup);
        assertThat(resolvedEffectiveGroups.get(Constants.STATIC_INITIALIZER_BLOCK_MEMBER))
                .isSameAs(earlyRootGroup);
    }

    @Test
    void resolveEffectiveGroups_inputMappingOrderDifferent_shouldResolveSameGroups() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> firstNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(Constants.PROVIDER_FIELD_MEMBER, Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER),
                Map.of(
                        Constants.PROVIDER_FIELD_MEMBER, lateGroup,
                        Constants.DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                        Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                        Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                        Constants.UNRELATED_FIELD_MEMBER, lateGroup,
                        Constants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup));
        Map<CtTypeMember, CompiledMemberGroup> secondNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, Constants.PROVIDER_FIELD_MEMBER),
                Map.of(
                        Constants.PROVIDER_FIELD_MEMBER, lateGroup,
                        Constants.DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                        Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                        Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                        Constants.UNRELATED_FIELD_MEMBER, lateGroup,
                        Constants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup));
        MemberDependencyGraph firstDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(firstNaturalGroupMapping);
        MemberDependencyGraph secondDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(secondNaturalGroupMapping);

        // When
        Map<CtTypeMember, CompiledMemberGroup> firstResolvedMapping =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(firstNaturalGroupMapping, firstDependencyGraph);
        Map<CtTypeMember, CompiledMemberGroup> secondResolvedMapping =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(secondNaturalGroupMapping, secondDependencyGraph);

        // Then
        assertThat(firstResolvedMapping).isEqualTo(secondResolvedMapping);
        assertThat(firstResolvedMapping.get(Constants.PROVIDER_FIELD_MEMBER)).isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_missingNaturalGroupForTransitiveDependent_shouldThrow() {
        // Given
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> completeNaturalGroupMapping = Map.of(
                Constants.PROVIDER_FIELD_MEMBER, lateGroup,
                Constants.DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                Constants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateGroup,
                Constants.UNRELATED_FIELD_MEMBER, lateGroup,
                Constants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(completeNaturalGroupMapping);
        Map<CtTypeMember, CompiledMemberGroup> incompleteNaturalGroupMapping =
                new HashMap<>(completeNaturalGroupMapping);
        incompleteNaturalGroupMapping.remove(Constants.TRANSITIVE_DEPENDENT_FIELD_MEMBER);

        // When
        ThrowingCallable resolveAction = () -> EffectiveMemberGroupResolver.resolveEffectiveGroups(
                Map.copyOf(incompleteNaturalGroupMapping), memberDependencyGraph);

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

    private static CtTypeMember requireFixtureMember(String simpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.FIXTURE_MAIN_TYPE.getTypeMembers(), simpleName);
    }

    private static URL requireFixtureUrl(String classpathAbsolutePath) {
        URL resolvedUrl = EffectiveMemberGroupResolverTest.class.getResource(classpathAbsolutePath);
        if (resolvedUrl == null) {
            throw new IllegalStateException("Classpath resource not found: " + classpathAbsolutePath);
        }
        return resolvedUrl;
    }

    private static CtTypeMember requireUniqueInitializerBlockMember(
            CtType<?> declaringType, boolean requiredStaticness) {
        List<CtAnonymousExecutable> initializerBlocks = declaringType.getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtAnonymousExecutable)
                .map(typeMember -> (CtAnonymousExecutable) typeMember)
                .filter(initializerBlock ->
                        initializerBlock.getModifiers().contains(ModifierKind.STATIC) == requiredStaticness)
                .toList();

        if (initializerBlocks.size() != 1) {
            throw new IllegalStateException("Expected exactly one initializer block. requiredStaticness="
                    + requiredStaticness + ", found=" + initializerBlocks.size());
        }

        return initializerBlocks.getFirst();
    }

    private static final class Constants {

        private static final String FIXTURE_RESOURCE_PATH =
                "/test-cases/core/sorter/spoon/effective-group-resolution/valid/EffectiveGroupResolutionFixture.java";
        private static final URL FIXTURE_URL = requireFixtureUrl(FIXTURE_RESOURCE_PATH);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        private static final CtTypeMember STATIC_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(FIXTURE_MAIN_TYPE, true);

        private static final String PROVIDER_FIELD_NAME = "PROVIDER";
        private static final CtTypeMember PROVIDER_FIELD_MEMBER = requireFixtureMember(PROVIDER_FIELD_NAME);
        private static final String DIRECT_DEPENDENT_FIELD_NAME = "DIRECT_DEPENDENT";
        private static final CtTypeMember DIRECT_DEPENDENT_FIELD_MEMBER =
                requireFixtureMember(DIRECT_DEPENDENT_FIELD_NAME);
        private static final String TRANSITIVE_PROVIDER_FIELD_NAME = "TRANSITIVE_PROVIDER";
        private static final CtTypeMember TRANSITIVE_PROVIDER_FIELD_MEMBER =
                requireFixtureMember(TRANSITIVE_PROVIDER_FIELD_NAME);
        private static final String TRANSITIVE_DEPENDENT_FIELD_NAME = "TRANSITIVE_DEPENDENT";
        private static final CtTypeMember TRANSITIVE_DEPENDENT_FIELD_MEMBER =
                requireFixtureMember(TRANSITIVE_DEPENDENT_FIELD_NAME);
        private static final String UNRELATED_FIELD_NAME = "UNRELATED";
        private static final CtTypeMember UNRELATED_FIELD_MEMBER = requireFixtureMember(UNRELATED_FIELD_NAME);

        private Constants() {}
    }
}
