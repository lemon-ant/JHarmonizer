package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.GroupMembersOrderer.orderMembersInsideGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Review
class GroupMembersOrdererComplexDependenciesTest {

    @Test
    // @Disabled("TODO Flaky test! Debug it!!!")
    void orderMembersInsideGroups_alphaDepsAndAccessors_expectedStableOrder() {
        // Given
        URL fixtureResourceUrl =
                requireClasspathResourceUrl(Constants.GROUP_MEMBER_ORDERING_COMPLEX_FIXTURE_CLASSPATH_PATH);
        CtType<?> mainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(fixtureResourceUrl);
        List<CtTypeMember> explicitSourceTypeMembers =
                SpoonTypeMemberUtils.streamExplicitSourceTypeMembers(mainType).toList();
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("complex", true, List.of(SortKey.ALPHA));
        Map<CtTypeMember, CompiledMemberGroup> memberToNaturalGroup = explicitSourceTypeMembers.stream()
                .collect(Collectors.toUnmodifiableMap(Function.identity(), typeMember -> compiledMemberGroup));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(memberToNaturalGroup);
        List<MemberGroupBlock> groupBlocks = TypeMemberGrouper.groupMembersByEffectiveGroups(memberToNaturalGroup);

        // When
        List<MemberGroupBlock> orderedGroupBlocks = orderMembersInsideGroups(groupBlocks, dependencyGraph);

        // Then
        assertThat(orderedGroupBlocks).hasSize(1);
        MemberGroupBlock orderedGroupBlock = orderedGroupBlocks.getFirst();
        List<String> sourceAlphaKeys = deriveAlphaKeys(explicitSourceTypeMembers);
        List<String> orderedAlphaKeys = deriveAlphaKeys(orderedGroupBlock.getTypeMembers());
        assertProvidersAreReorderedAlphabeticallyButStillBeforeDependents(sourceAlphaKeys, orderedAlphaKeys);
        assertTransitiveDependencyChainIsRespected(orderedAlphaKeys);
        assertInitializerBlockIsAfterTheDeepestDependent(orderedAlphaKeys);
        assertAccessorBundlingPreventsInterleaving(sourceAlphaKeys, orderedAlphaKeys);
    }

    private static void assertProvidersAreReorderedAlphabeticallyButStillBeforeDependents(
            @NonNull List<String> sourceAlphaKeys, @NonNull List<String> orderedAlphaKeys) {
        List<String> providerKeysInSourceOrder = sourceAlphaKeys.stream()
                .filter(Constants.PROVIDER_ALPHA_KEYS::contains)
                .toList();
        assertThat(providerKeysInSourceOrder)
                .containsExactly(
                        Constants.Y_PROVIDER_ALPHA_KEY,
                        Constants.W_PROVIDER_ALPHA_KEY,
                        Constants.Z_PROVIDER_ALPHA_KEY,
                        Constants.X_PROVIDER_ALPHA_KEY);
        List<String> providerKeysInOrderedResult = orderedAlphaKeys.stream()
                .filter(Constants.PROVIDER_ALPHA_KEYS::contains)
                .toList();
        assertThat(providerKeysInOrderedResult)
                .containsExactly(
                        Constants.W_PROVIDER_ALPHA_KEY,
                        Constants.X_PROVIDER_ALPHA_KEY,
                        Constants.Y_PROVIDER_ALPHA_KEY,
                        Constants.Z_PROVIDER_ALPHA_KEY);
        int dependentIndex = requireIndex(orderedAlphaKeys, Constants.A_DEPENDENT_ALPHA_KEY);
        providerKeysInOrderedResult.forEach(providerAlphaKey -> {
            int providerIndex = requireIndex(orderedAlphaKeys, providerAlphaKey);
            assertThat(providerIndex).isLessThan(dependentIndex);
        });
    }

    private static void assertTransitiveDependencyChainIsRespected(@NonNull List<String> orderedAlphaKeys) {
        int aDependentIndex = requireIndex(orderedAlphaKeys, Constants.A_DEPENDENT_ALPHA_KEY);
        int cDependentIndex = requireIndex(orderedAlphaKeys, Constants.C_DEPENDENT_ALPHA_KEY);
        int bDependentIndex = requireIndex(orderedAlphaKeys, Constants.B_DEPENDENT_ALPHA_KEY);
        int dDependentIndex = requireIndex(orderedAlphaKeys, Constants.D_DEPENDENT_ALPHA_KEY);
        // TODO Explore flaky tests
        assertThat(aDependentIndex).isLessThan(cDependentIndex);
        assertThat(cDependentIndex).isLessThan(bDependentIndex);
        assertThat(bDependentIndex).isLessThan(dDependentIndex);
    }

    private static void assertInitializerBlockIsAfterTheDeepestDependent(@NonNull List<String> orderedAlphaKeys) {
        int deepestDependentIndex = requireIndex(orderedAlphaKeys, Constants.C_DEPENDENT_ALPHA_KEY);
        int initializerBlockIndex = requireIndex(orderedAlphaKeys, Constants.INSTANCE_INITIALIZER_BLOCK_ALPHA_KEY);
        assertThat(initializerBlockIndex).isGreaterThan(deepestDependentIndex);
    }

    private static void assertAccessorBundlingPreventsInterleaving(
            @NonNull List<String> sourceAlphaKeys, @NonNull List<String> orderedAlphaKeys) {
        List<String> methodKeysInSourceOrder = sourceAlphaKeys.stream()
                .filter(Constants.METHOD_ALPHA_KEYS::contains)
                .toList();
        assertThat(methodKeysInSourceOrder)
                .containsExactly(
                        Constants.SET_ENABLED_FLAG_ALPHA_KEY,
                        Constants.IS_ENABLED_FLAG_ALPHA_KEY,
                        Constants.HELLO_ENABLED_FLAG_ALPHA_KEY,
                        Constants.GET_ENABLED_FLAG_ALPHA_KEY,
                        Constants.HAS_ENABLED_FLAG_ALPHA_KEY);
        List<String> accessorKeysInOrderedResult = orderedAlphaKeys.stream()
                .filter(Constants.ACCESSOR_ALPHA_KEYS::contains)
                .toList();
        assertThat(accessorKeysInOrderedResult)
                .containsExactly(
                        Constants.GET_ENABLED_FLAG_ALPHA_KEY,
                        Constants.HAS_ENABLED_FLAG_ALPHA_KEY,
                        Constants.IS_ENABLED_FLAG_ALPHA_KEY,
                        Constants.SET_ENABLED_FLAG_ALPHA_KEY);
        int firstAccessorIndex = accessorKeysInOrderedResult.stream()
                .mapToInt(accessorAlphaKey -> requireIndex(orderedAlphaKeys, accessorAlphaKey))
                .min()
                .orElseThrow();
        int lastAccessorIndex = accessorKeysInOrderedResult.stream()
                .mapToInt(accessorAlphaKey -> requireIndex(orderedAlphaKeys, accessorAlphaKey))
                .max()
                .orElseThrow();
        assertThat(lastAccessorIndex - firstAccessorIndex + 1).isEqualTo(accessorKeysInOrderedResult.size());
        int helloMethodIndex = requireIndex(orderedAlphaKeys, Constants.HELLO_ENABLED_FLAG_ALPHA_KEY);
        assertThat(helloMethodIndex).isGreaterThan(lastAccessorIndex);
    }

    private static int requireIndex(@NonNull List<String> alphaKeys, @NonNull String alphaKey) {
        int index = alphaKeys.indexOf(alphaKey);
        if (index < 0) {
            throw new IllegalArgumentException("Alpha key: " + alphaKey + " not found in the list: " + alphaKeys);
        }
        return index;
    }

    private static List<String> deriveAlphaKeys(@NonNull List<CtTypeMember> typeMembers) {
        return typeMembers.stream().map(SpoonTypeMemberUtils::deriveAlphaKey).toList();
    }

    private static class Constants {
        private static final String GROUP_MEMBER_ORDERING_COMPLEX_FIXTURE_CLASSPATH_PATH =
                "/test-cases/core/sorter/spoon/group-member-ordering/valid/GroupMemberOrderingComplexFixture.java";
        private static final String W_PROVIDER_ALPHA_KEY = "w_provider:int";
        private static final String X_PROVIDER_ALPHA_KEY = "x_provider:int";
        private static final String Y_PROVIDER_ALPHA_KEY = "y_provider:int";
        private static final String Z_PROVIDER_ALPHA_KEY = "z_provider:int";
        private static final Set<String> PROVIDER_ALPHA_KEYS =
                Set.of(W_PROVIDER_ALPHA_KEY, X_PROVIDER_ALPHA_KEY, Y_PROVIDER_ALPHA_KEY, Z_PROVIDER_ALPHA_KEY);
        private static final String A_DEPENDENT_ALPHA_KEY = "a_dependent:int";
        private static final String B_DEPENDENT_ALPHA_KEY = "b_dependent:int";
        private static final String C_DEPENDENT_ALPHA_KEY = "c_dependent:int";
        private static final String D_DEPENDENT_ALPHA_KEY = "d_dependent:int";
        private static final String INSTANCE_INITIALIZER_BLOCK_ALPHA_KEY = "<init>";
        private static final String GET_ENABLED_FLAG_ALPHA_KEY = "getEnabledFlag():java.lang.Boolean";
        private static final String HAS_ENABLED_FLAG_ALPHA_KEY = "hasEnabledFlag():boolean";
        private static final String IS_ENABLED_FLAG_ALPHA_KEY = "isEnabledFlag():boolean";
        private static final String SET_ENABLED_FLAG_ALPHA_KEY = "setEnabledFlag(boolean):void";
        private static final Set<String> ACCESSOR_ALPHA_KEYS = Set.of(
                GET_ENABLED_FLAG_ALPHA_KEY,
                HAS_ENABLED_FLAG_ALPHA_KEY,
                IS_ENABLED_FLAG_ALPHA_KEY,
                SET_ENABLED_FLAG_ALPHA_KEY);
        private static final String HELLO_ENABLED_FLAG_ALPHA_KEY = "helloEnabledFlag():boolean";
        private static final Set<String> METHOD_ALPHA_KEYS = Set.of(
                GET_ENABLED_FLAG_ALPHA_KEY,
                HAS_ENABLED_FLAG_ALPHA_KEY,
                IS_ENABLED_FLAG_ALPHA_KEY,
                SET_ENABLED_FLAG_ALPHA_KEY,
                HELLO_ENABLED_FLAG_ALPHA_KEY);
    }
}
