package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.GroupMembersOrderer.orderMembersInsideGroups;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Review
class GroupMembersOrdererComplexDependenciesTest {

    @Test
    @Disabled("TODO Flaky test! Debug it!!!")
    void orderMembersInsideGroups_alphaDepsAndAccessors_expectedStableOrder() {
        // Given
        URL fixtureResourceUrl =
                requireClasspathResourceUrl(Constants.GROUP_MEMBER_ORDERING_COMPLEX_FIXTURE_CLASSPATH_PATH);
        CtType<?> mainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(fixtureResourceUrl);
        List<CtTypeMember> explicitSrcTypeMembers =
                SpoonTypeMemberUtils.streamExplicitSrcTypeMembers(mainType).toList();
        CompiledMemberGroup compiledMemberGroup =
                CompiledMemberGroupTestCreator.createCompiledMemberGroup("complex", true, List.of(OrderingRule.ALPHA));
        Map<CtTypeMember, CompiledMemberGroup> memberToNaturalGroup = explicitSrcTypeMembers.stream()
                .collect(Collectors.toUnmodifiableMap(Function.identity(), typeMember -> compiledMemberGroup));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(memberToNaturalGroup);
        List<MemberGroupBlock> groupBlocks = TypeMemberGrouper.groupMembersByEffectiveGroups(memberToNaturalGroup);

        // When
        List<MemberGroupBlock> orderedGroupBlocks = orderMembersInsideGroups(groupBlocks, dependencyGraph);

        // Then
        assertThat(orderedGroupBlocks)
                .withFailMessage(
                        "Expected a single ordered block but got %s.%n%s",
                        orderedGroupBlocks.size(),
                        buildDiagnosticReport(explicitSrcTypeMembers, List.of(), dependencyGraph))
                .hasSize(1);

        MemberGroupBlock orderedGroupBlock = orderedGroupBlocks.getFirst();
        List<CtTypeMember> orderedTypeMembers = orderedGroupBlock.getTypeMembers();
        List<String> srcAlphaKeys = deriveAlphaKeys(explicitSrcTypeMembers);
        List<String> orderedAlphaKeys = deriveAlphaKeys(orderedTypeMembers);
        String stateSnapshot = buildStateSnapshot(
                explicitSrcTypeMembers,
                orderedTypeMembers,
                dependencyGraph,
                memberToNaturalGroup,
                groupBlocks,
                orderedGroupBlocks);

        // TODO Flaky test
        assertProvidersAreReorderedAlphabeticallyButStillBeforeDependents(
                srcAlphaKeys, orderedAlphaKeys, explicitSrcTypeMembers, orderedTypeMembers, dependencyGraph);
        assertTransitiveDependencyChainIsRespected(
                orderedAlphaKeys, explicitSrcTypeMembers, orderedTypeMembers, dependencyGraph);
        assertInitializerBlockIsAfterTheDeepestDependent(
                orderedAlphaKeys, explicitSrcTypeMembers, orderedTypeMembers, dependencyGraph);
        assertAccessorBundlingPreventsInterleaving(
                srcAlphaKeys, orderedAlphaKeys, explicitSrcTypeMembers, orderedTypeMembers, dependencyGraph);

        emitSuccessfulRunSnapshot(stateSnapshot);
    }

    private static void assertProvidersAreReorderedAlphabeticallyButStillBeforeDependents(
            List<String> srcAlphaKeys,
            List<String> orderedAlphaKeys,
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph) {
        List<String> providerKeysInSrcOrder = srcAlphaKeys.stream()
                .filter(Constants.PROVIDER_ALPHA_KEYS::contains)
                .toList();
        assertThat(providerKeysInSrcOrder)
                .containsExactly(
                        Constants.Y_PROVIDER_ALPHA_KEY,
                        Constants.W_PROVIDER_ALPHA_KEY,
                        Constants.Z_PROVIDER_ALPHA_KEY,
                        Constants.X_PROVIDER_ALPHA_KEY);
        List<String> providerKeysInOrderedResult = orderedAlphaKeys.stream()
                .filter(Constants.PROVIDER_ALPHA_KEYS::contains)
                .toList();
        // TODO Flaky test
        assertThat(providerKeysInOrderedResult)
                .withFailMessage(
                        "Provider keys are expected to be alphabetically ordered in the result.%n%s",
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .containsExactly(
                        Constants.W_PROVIDER_ALPHA_KEY,
                        Constants.X_PROVIDER_ALPHA_KEY,
                        Constants.Y_PROVIDER_ALPHA_KEY,
                        Constants.Z_PROVIDER_ALPHA_KEY);
        int dependentIndex = requireIndex(orderedAlphaKeys, Constants.A_DEPENDENT_ALPHA_KEY);
        providerKeysInOrderedResult.forEach(providerAlphaKey -> {
            int providerIndex = requireIndex(orderedAlphaKeys, providerAlphaKey);
            assertThat(providerIndex)
                    .withFailMessage(
                            "Provider key '%s' should be before dependent '%s'.%n%s",
                            providerAlphaKey,
                            Constants.A_DEPENDENT_ALPHA_KEY,
                            buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                    .isLessThan(dependentIndex);
        });
    }

    private static void assertTransitiveDependencyChainIsRespected(
            List<String> orderedAlphaKeys,
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph) {
        int aDependentIndex = requireIndex(orderedAlphaKeys, Constants.A_DEPENDENT_ALPHA_KEY);
        int cDependentIndex = requireIndex(orderedAlphaKeys, Constants.C_DEPENDENT_ALPHA_KEY);
        int bDependentIndex = requireIndex(orderedAlphaKeys, Constants.B_DEPENDENT_ALPHA_KEY);
        int dDependentIndex = requireIndex(orderedAlphaKeys, Constants.D_DEPENDENT_ALPHA_KEY);
        assertThat(aDependentIndex)
                .withFailMessage(
                        "Expected '%s' before '%s'.%n%s",
                        Constants.A_DEPENDENT_ALPHA_KEY,
                        Constants.C_DEPENDENT_ALPHA_KEY,
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .isLessThan(cDependentIndex);
        assertThat(cDependentIndex)
                .withFailMessage(
                        "Expected '%s' before '%s'.%n%s",
                        Constants.C_DEPENDENT_ALPHA_KEY,
                        Constants.B_DEPENDENT_ALPHA_KEY,
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .isLessThan(bDependentIndex);
        assertThat(bDependentIndex)
                .withFailMessage(
                        "Expected '%s' before '%s'.%n%s",
                        Constants.B_DEPENDENT_ALPHA_KEY,
                        Constants.D_DEPENDENT_ALPHA_KEY,
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .isLessThan(dDependentIndex);
    }

    private static void assertInitializerBlockIsAfterTheDeepestDependent(
            List<String> orderedAlphaKeys,
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph) {
        int deepestDependentIndex = requireIndex(orderedAlphaKeys, Constants.C_DEPENDENT_ALPHA_KEY);
        int initializerBlockIndex = requireIndex(orderedAlphaKeys, Constants.INSTANCE_INITIALIZER_BLOCK_ALPHA_KEY);
        assertThat(initializerBlockIndex)
                .withFailMessage(
                        "Initializer block should be after '%s'.%n%s",
                        Constants.C_DEPENDENT_ALPHA_KEY,
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .isGreaterThan(deepestDependentIndex);
    }

    private static void assertAccessorBundlingPreventsInterleaving(
            List<String> srcAlphaKeys,
            List<String> orderedAlphaKeys,
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph) {
        List<String> methodKeysInSrcOrder = srcAlphaKeys.stream()
                .filter(Constants.METHOD_ALPHA_KEYS::contains)
                .toList();
        assertThat(methodKeysInSrcOrder)
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
                .withFailMessage(
                        "Accessor methods should stay bundled and alphabetically ordered.%n%s",
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
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
        assertThat(helloMethodIndex)
                .withFailMessage(
                        "Method '%s' should be after accessor bundle.%n%s",
                        Constants.HELLO_ENABLED_FLAG_ALPHA_KEY,
                        buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph))
                .isGreaterThan(lastAccessorIndex);
    }

    @NonNull
    private static String buildDiagnosticReport(
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph) {
        List<String> srcAlphaKeys = deriveAlphaKeys(srcTypeMembers);
        List<String> orderedAlphaKeys = deriveAlphaKeys(orderedTypeMembers);
        Map<String, CtTypeMember> srcMembersByAlphaKey = srcTypeMembers.stream()
                .collect(Collectors.toMap(
                        SpoonTypeMemberUtils::deriveAlphaKey,
                        Function.identity(),
                        (leftMember, ignored) -> leftMember,
                        LinkedHashMap::new));

        String diagnostic = "Diagnostic report for flaky ordering test:\n" + "- sourceAlphaKeys=" + srcAlphaKeys
                + '\n' + "- orderedAlphaKeys="
                + orderedAlphaKeys + '\n' + "- trackedIndexes="
                + renderTrackedIndexes(orderedAlphaKeys)
                + '\n'
                + "- declarationDirectDependencies="
                + renderDirectDependenciesByMember(
                        srcMembersByAlphaKey, dependencyGraph, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)
                + '\n'
                + "- declarationTransitiveDependencies="
                + renderTransitiveDependenciesByMember(
                        srcMembersByAlphaKey, dependencyGraph, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)
                + '\n'
                + "- accessorBundleDirectDependencies="
                + renderDirectDependenciesByMember(
                        srcMembersByAlphaKey, dependencyGraph, MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
        return diagnostic;
    }

    @NonNull
    private static String buildStateSnapshot(
            List<CtTypeMember> srcTypeMembers,
            List<CtTypeMember> orderedTypeMembers,
            MemberDependencyGraph dependencyGraph,
            Map<CtTypeMember, CompiledMemberGroup> memberToNaturalGroup,
            List<MemberGroupBlock> srcGroupBlocks,
            List<MemberGroupBlock> orderedGroupBlocks) {
        String snapshot = "State snapshot for complex ordering test:\n" + "- memberToNaturalGroup="
                + renderMemberToGroup(memberToNaturalGroup)
                + '\n'
                + "- sourceGroupBlocks="
                + renderGroupBlocks(srcGroupBlocks)
                + '\n'
                + "- orderedGroupBlocks="
                + renderGroupBlocks(orderedGroupBlocks)
                + '\n'
                + buildDiagnosticReport(srcTypeMembers, orderedTypeMembers, dependencyGraph);
        return snapshot;
    }

    @NonNull
    private static Map<String, String> renderMemberToGroup(
            Map<CtTypeMember, CompiledMemberGroup> memberToNaturalGroup) {
        return memberToNaturalGroup.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> SpoonTypeMemberUtils.deriveAlphaKey(entry.getKey()),
                        entry -> entry.getValue().getName(),
                        (leftEntry, ignored) -> leftEntry,
                        LinkedHashMap::new));
    }

    @NonNull
    private static List<List<String>> renderGroupBlocks(List<MemberGroupBlock> groupBlocks) {
        return groupBlocks.stream()
                .map(groupBlock -> deriveAlphaKeys(groupBlock.getTypeMembers()))
                .toList();
    }

    private static void emitSuccessfulRunSnapshot(String stateSnapshot) {
        System.out.println("PASS_SNAPSHOT_START");
        System.out.println(stateSnapshot);
        System.out.println("PASS_SNAPSHOT_END");
    }

    @NonNull
    private static Map<String, Integer> renderTrackedIndexes(List<String> orderedAlphaKeys) {
        LinkedHashMap<String, Integer> trackedIndexes = new LinkedHashMap<>();
        trackedIndexes.put(Constants.W_PROVIDER_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.W_PROVIDER_ALPHA_KEY));
        trackedIndexes.put(Constants.X_PROVIDER_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.X_PROVIDER_ALPHA_KEY));
        trackedIndexes.put(Constants.Y_PROVIDER_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.Y_PROVIDER_ALPHA_KEY));
        trackedIndexes.put(Constants.Z_PROVIDER_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.Z_PROVIDER_ALPHA_KEY));
        trackedIndexes.put(Constants.A_DEPENDENT_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.A_DEPENDENT_ALPHA_KEY));
        trackedIndexes.put(Constants.B_DEPENDENT_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.B_DEPENDENT_ALPHA_KEY));
        trackedIndexes.put(Constants.C_DEPENDENT_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.C_DEPENDENT_ALPHA_KEY));
        trackedIndexes.put(Constants.D_DEPENDENT_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.D_DEPENDENT_ALPHA_KEY));
        trackedIndexes.put(
                Constants.INSTANCE_INITIALIZER_BLOCK_ALPHA_KEY,
                orderedAlphaKeys.indexOf(Constants.INSTANCE_INITIALIZER_BLOCK_ALPHA_KEY));
        trackedIndexes.put(
                Constants.GET_ENABLED_FLAG_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.GET_ENABLED_FLAG_ALPHA_KEY));
        trackedIndexes.put(
                Constants.HAS_ENABLED_FLAG_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.HAS_ENABLED_FLAG_ALPHA_KEY));
        trackedIndexes.put(
                Constants.IS_ENABLED_FLAG_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.IS_ENABLED_FLAG_ALPHA_KEY));
        trackedIndexes.put(
                Constants.SET_ENABLED_FLAG_ALPHA_KEY, orderedAlphaKeys.indexOf(Constants.SET_ENABLED_FLAG_ALPHA_KEY));
        trackedIndexes.put(
                Constants.HELLO_ENABLED_FLAG_ALPHA_KEY,
                orderedAlphaKeys.indexOf(Constants.HELLO_ENABLED_FLAG_ALPHA_KEY));
        return trackedIndexes;
    }

    @NonNull
    private static Map<String, List<String>> renderDirectDependenciesByMember(
            Map<String, CtTypeMember> srcMembersByAlphaKey,
            MemberDependencyGraph dependencyGraph,
            MemberDependencyEdgeKind edgeKind) {
        return srcMembersByAlphaKey.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        srcEntry ->
                                dependencyGraph.findDirectDependents(srcEntry.getValue(), EnumSet.of(edgeKind)).stream()
                                        .map(SpoonTypeMemberUtils::deriveAlphaKey)
                                        .sorted()
                                        .toList(),
                        (leftEntry, ignored) -> leftEntry,
                        LinkedHashMap::new));
    }

    @NonNull
    private static Map<String, List<String>> renderTransitiveDependenciesByMember(
            Map<String, CtTypeMember> srcMembersByAlphaKey,
            MemberDependencyGraph dependencyGraph,
            MemberDependencyEdgeKind edgeKind) {
        return srcMembersByAlphaKey.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        srcEntry ->
                                dependencyGraph
                                        .findTransitiveDependents(srcEntry.getValue(), EnumSet.of(edgeKind))
                                        .stream()
                                        .map(SpoonTypeMemberUtils::deriveAlphaKey)
                                        .sorted()
                                        .toList(),
                        (leftEntry, ignored) -> leftEntry,
                        LinkedHashMap::new));
    }

    private static int requireIndex(List<String> alphaKeys, String alphaKey) {
        int index = alphaKeys.indexOf(alphaKey);
        if (index < 0) {
            throw new IllegalArgumentException("Alpha key: " + alphaKey + " not found in the list: " + alphaKeys);
        }
        return index;
    }

    @NonNull
    private static List<String> deriveAlphaKeys(List<CtTypeMember> typeMembers) {
        return typeMembers.stream().map(SpoonTypeMemberUtils::deriveAlphaKey).toList();
    }

    private static class Constants {
        private static final String GROUP_MEMBER_ORDERING_COMPLEX_FIXTURE_CLASSPATH_PATH =
                "/test-cases/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleComplexFixture.java";
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
