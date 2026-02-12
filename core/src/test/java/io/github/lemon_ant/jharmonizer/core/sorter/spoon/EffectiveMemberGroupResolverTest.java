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
import org.assertj.core.api.ThrowableAssert;
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
                FixtureConstants.PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.DIRECT_DEPENDENT_FIELD_MEMBER, middleGroup,
                FixtureConstants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                FixtureConstants.UNRELATED_FIELD_MEMBER, unrelatedEarlierGroup,
                FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER, initializerBlockLateGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalMemberGroup);
        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);
        // Then
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.DIRECT_DEPENDENT_FIELD_MEMBER))
                .isSameAs(middleGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.TRANSITIVE_PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.UNRELATED_FIELD_MEMBER))
                .isSameAs(unrelatedEarlierGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER))
                .isSameAs(initializerBlockLateGroup);
        assertThatThrownBy(() -> resolvedEffectiveGroups.put(FixtureConstants.PROVIDER_FIELD_MEMBER, lateGroup))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolveEffectiveGroups_dependentInEarlierRootGroup_shouldPullAcrossRootGroups() {
        // Given
        CompiledMemberGroup earlyRootGroup = createTrivialMemberGroup("root-alpha", false, 1);
        CompiledMemberGroup lateRootGroup = createTrivialMemberGroup("root-bravo", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup = Map.of(
                FixtureConstants.PROVIDER_FIELD_MEMBER, lateRootGroup,
                FixtureConstants.DIRECT_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                FixtureConstants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateRootGroup,
                FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateRootGroup,
                FixtureConstants.UNRELATED_FIELD_MEMBER, lateRootGroup,
                FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER, earlyRootGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalMemberGroup);

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedEffectiveGroups =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(
                        typeMember2NaturalMemberGroup, memberDependencyGraph);

        // Then
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyRootGroup);
        assertThat(resolvedEffectiveGroups.get(FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER))
                .isSameAs(earlyRootGroup);
    }

    @Test
    void resolveEffectiveGroups_inputMappingOrderDifferent_shouldResolveSameGroups() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> baseNaturalGroupMapping = Map.of(
                FixtureConstants.PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                FixtureConstants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, earlyGroup,
                FixtureConstants.UNRELATED_FIELD_MEMBER, lateGroup,
                FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup);
        Map<CtTypeMember, CompiledMemberGroup> firstNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(FixtureConstants.PROVIDER_FIELD_MEMBER, FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER),
                baseNaturalGroupMapping);
        Map<CtTypeMember, CompiledMemberGroup> secondNaturalGroupMapping = createNaturalGroupMappingInOrder(
                List.of(FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, FixtureConstants.PROVIDER_FIELD_MEMBER),
                baseNaturalGroupMapping);
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
        assertThat(firstResolvedMapping.get(FixtureConstants.PROVIDER_FIELD_MEMBER))
                .isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_accessorBundlingEnabled_shouldPullPairedAccessorsToEarliestGroup() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", true, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", true, 100);
        Map<CtTypeMember, CompiledMemberGroup> naturalGroupsByMember = Map.of(
                AccessorFixtureConstants.GET_VALUE_METHOD_MEMBER, lateGroup,
                AccessorFixtureConstants.SET_VALUE_METHOD_MEMBER, earlyGroup,
                AccessorFixtureConstants.IS_ENABLED_METHOD_MEMBER, lateGroup,
                AccessorFixtureConstants.SET_ENABLED_METHOD_MEMBER, earlyGroup);
        MemberDependencyGraph dependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(naturalGroupsByMember);

        // When
        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupsByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupsByMember, dependencyGraph);

        // Then
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.GET_VALUE_METHOD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.SET_VALUE_METHOD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.IS_ENABLED_METHOD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.SET_ENABLED_METHOD_MEMBER))
                .isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_accessorBundlingDisabled_shouldKeepNaturalGroups() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> naturalGroupsByMember = Map.of(
                AccessorFixtureConstants.GET_VALUE_METHOD_MEMBER, lateGroup,
                AccessorFixtureConstants.SET_VALUE_METHOD_MEMBER, earlyGroup,
                AccessorFixtureConstants.IS_ENABLED_METHOD_MEMBER, lateGroup,
                AccessorFixtureConstants.SET_ENABLED_METHOD_MEMBER, earlyGroup);
        MemberDependencyGraph dependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(naturalGroupsByMember);

        // When
        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupsByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupsByMember, dependencyGraph);

        // Then
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.GET_VALUE_METHOD_MEMBER))
                .isSameAs(lateGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.SET_VALUE_METHOD_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.IS_ENABLED_METHOD_MEMBER))
                .isSameAs(lateGroup);
        assertThat(effectiveGroupsByMember.get(AccessorFixtureConstants.SET_ENABLED_METHOD_MEMBER))
                .isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_blankFinalRead_shouldPullAssignmentProviderToDependentGroup() {
        // Given
        CompiledMemberGroup earlyGroup = createTrivialMemberGroup("early", false, 10);
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> naturalGroupsByMember = Map.of(
                BlankFinalFixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup,
                BlankFinalFixtureConstants.READS_BLANK_FINAL_FIELD_MEMBER, earlyGroup);
        MemberDependencyGraph dependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(naturalGroupsByMember);

        // When
        Map<CtTypeMember, CompiledMemberGroup> effectiveGroupsByMember =
                EffectiveMemberGroupResolver.resolveEffectiveGroups(naturalGroupsByMember, dependencyGraph);

        // Then
        assertThat(effectiveGroupsByMember.get(BlankFinalFixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER))
                .isSameAs(earlyGroup);
        assertThat(effectiveGroupsByMember.get(BlankFinalFixtureConstants.READS_BLANK_FINAL_FIELD_MEMBER))
                .isSameAs(earlyGroup);
    }

    @Test
    void resolveEffectiveGroups_missingNaturalGroupForTransitiveDependent_shouldThrow() {
        // Given
        CompiledMemberGroup lateGroup = createTrivialMemberGroup("late", false, 100);
        Map<CtTypeMember, CompiledMemberGroup> completeNaturalGroupMapping = Map.of(
                FixtureConstants.PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.DIRECT_DEPENDENT_FIELD_MEMBER, lateGroup,
                FixtureConstants.TRANSITIVE_PROVIDER_FIELD_MEMBER, lateGroup,
                FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER, lateGroup,
                FixtureConstants.UNRELATED_FIELD_MEMBER, lateGroup,
                FixtureConstants.STATIC_INITIALIZER_BLOCK_MEMBER, lateGroup);
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(completeNaturalGroupMapping);
        Map<CtTypeMember, CompiledMemberGroup> incompleteNaturalGroupMapping =
                new HashMap<>(completeNaturalGroupMapping);
        incompleteNaturalGroupMapping.remove(FixtureConstants.TRANSITIVE_DEPENDENT_FIELD_MEMBER);

        // When
        ThrowableAssert.ThrowingCallable resolveAction = () -> EffectiveMemberGroupResolver.resolveEffectiveGroups(
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

    private static CtTypeMember requireFixtureMember(CtType<?> fixtureMainType, String simpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(fixtureMainType.getTypeMembers(), simpleName);
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

    private static final class FixtureConstants {
        private static final String FIXTURE_RESOURCE_PATH =
                "/test-cases/core/sorter/spoon/effective-group-resolution/valid/EffectiveGroupResolutionFixture.java";
        private static final URL FIXTURE_URL = requireFixtureUrl(FIXTURE_RESOURCE_PATH);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        private static final CtTypeMember STATIC_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(FIXTURE_MAIN_TYPE, true);
        private static final CtTypeMember PROVIDER_FIELD_MEMBER = requireFixtureMember(FIXTURE_MAIN_TYPE, "PROVIDER");
        private static final CtTypeMember DIRECT_DEPENDENT_FIELD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "DIRECT_DEPENDENT");
        private static final CtTypeMember TRANSITIVE_PROVIDER_FIELD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "TRANSITIVE_PROVIDER");
        private static final CtTypeMember TRANSITIVE_DEPENDENT_FIELD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "TRANSITIVE_DEPENDENT");
        private static final CtTypeMember UNRELATED_FIELD_MEMBER = requireFixtureMember(FIXTURE_MAIN_TYPE, "UNRELATED");

        private FixtureConstants() {}
    }

    private static final class AccessorFixtureConstants {
        private static final String FIXTURE_RESOURCE_PATH =
                "/test-cases/core/sorter/spoon/effective-group-resolution/valid/EffectiveGroupResolutionAccessorBundleFixture.java";
        private static final URL FIXTURE_URL = requireFixtureUrl(FIXTURE_RESOURCE_PATH);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        private static final CtTypeMember GET_VALUE_METHOD_MEMBER = requireFixtureMember(FIXTURE_MAIN_TYPE, "getValue");
        private static final CtTypeMember SET_VALUE_METHOD_MEMBER = requireFixtureMember(FIXTURE_MAIN_TYPE, "setValue");
        private static final CtTypeMember IS_ENABLED_METHOD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "isEnabled");
        private static final CtTypeMember SET_ENABLED_METHOD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "setEnabled");

        private AccessorFixtureConstants() {}
    }

    private static final class BlankFinalFixtureConstants {
        private static final String FIXTURE_RESOURCE_PATH =
                "/test-cases/core/sorter/spoon/effective-group-resolution/valid/EffectiveGroupResolutionBlankFinalFixture.java";
        private static final URL FIXTURE_URL = requireFixtureUrl(FIXTURE_RESOURCE_PATH);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_URL);
        private static final CtTypeMember STATIC_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(FIXTURE_MAIN_TYPE, true);
        private static final CtTypeMember READS_BLANK_FINAL_FIELD_MEMBER =
                requireFixtureMember(FIXTURE_MAIN_TYPE, "READS_BLANK_FINAL");

        private BlankFinalFixtureConstants() {}
    }
}
