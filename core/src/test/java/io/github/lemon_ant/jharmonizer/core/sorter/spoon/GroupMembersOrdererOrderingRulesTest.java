package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraphBuilder;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

class GroupMembersOrdererOrderingRulesTest {

    @Test
    void orderMembersInsideGroups_orderingRulePreserve_keepOriginalSrcOrder() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "preserve", false, List.of(OrderingRule.PRESERVE));
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
    void orderMembersInsideGroups_orderingRulePreserveWithEqualSrcStart_applyAlphaTieBreakerDeterministically() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "preserve-equal-keys", false, List.of(OrderingRule.PRESERVE));
        CtTypeMember alphaFieldMember = createFieldTypeMember("alphaField", 100);
        CtTypeMember bravoFieldMember = createFieldTypeMember("bravoField", 100);
        CtTypeMember charlieFieldMember = createFieldTypeMember("charlieField", 100);
        List<CtTypeMember> alphaTieBreakerOrderedMembers =
                List.of(alphaFieldMember, bravoFieldMember, charlieFieldMember);
        List<CtTypeMember> inputMembers = new ArrayList<>(alphaTieBreakerOrderedMembers);
        Collections.reverse(inputMembers);
        MemberGroupBlock inputBlock = new MemberGroupBlock(compiledMemberGroup, inputMembers);
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                alphaFieldMember, compiledMemberGroup,
                bravoFieldMember, compiledMemberGroup,
                charlieFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks).hasSize(1);
        assertThat(orderedBlocks.getFirst().getTypeMembers()).containsExactlyElementsOf(alphaTieBreakerOrderedMembers);
    }

    @Test
    void orderMembersInsideGroups_orderingRuleAlphaWithFields_orderAlphabetically() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-fields", false, List.of(OrderingRule.ALPHA));
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
    void orderMembersInsideGroups_simpleNameCompileTimeConstantDependency_keepsProviderBeforeDependent() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "implicit-constant-source-order", false, List.of(OrderingRule.ALPHA));
        CtTypeMember patternFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MEMBERS, "zPattern");
        CtTypeMember formatterFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MEMBERS, "aFormatter");
        CtTypeMember anchorFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MEMBERS, "cAnchor");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(patternFieldMember, formatterFieldMember, anchorFieldMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                patternFieldMember, compiledMemberGroup,
                formatterFieldMember, compiledMemberGroup,
                anchorFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then — zPattern comes before aFormatter due to the declaration dependency;
        // cAnchor is unconstrained and follows the dependency chain
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(patternFieldMember, formatterFieldMember, anchorFieldMember);
    }

    @Test
    void orderMembersInsideGroups_orderingRuleAlphaWithOverloads_orderDeterministically() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-overloads", false, List.of(OrderingRule.ALPHA));
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
                        .map(GroupMembersOrdererOrderingRulesTest::extractParameterTypeQualifiedNames)
                        .toList())
                .containsExactly(List.of(), List.of("int"), List.of("java.lang.String"));
    }

    @Test
    void orderMembersInsideGroups_orderingRuleVisibilityAsc_orderByVisibilityRank() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "visibility-asc", false, List.of(OrderingRule.VISIBILITY_ASC));
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
                        privateFieldMember,
                        packagePrivateFieldMember,
                        protectedFieldMember,
                        publicFieldFirstMember,
                        publicFieldSecondMember);
    }

    @Test
    void orderMembersInsideGroups_orderingRuleVisibilityDesc_orderByVisibilityRankDescending() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "visibility-desc", false, List.of(OrderingRule.VISIBILITY_DESC));
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
                        publicFieldFirstMember,
                        publicFieldSecondMember,
                        protectedFieldMember,
                        packagePrivateFieldMember,
                        privateFieldMember);
    }

    @Test
    void orderMembersInsideGroups_keepAccessorsTogetherEnabled_keepAccessorPairContiguous() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-enabled", true, List.of(OrderingRule.ALPHA));
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

        // Then — the {getValue, setValue} bundle compares by the full alphaKey of its anchor
        // when compared against a non-cluster singleton: "getValue():int" (g) < "middleMethod():void" (m),
        // so the bundle sorts before middleMethod
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(getValueMethodMember, setValueMethodMember, middleMethodMember);
    }

    @Test
    void orderMembersInsideGroups_keepAccessorsTogetherDisabled_allowAlphaInterleaving() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-disabled", false, List.of(OrderingRule.ALPHA));
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
    void orderMembersInsideGroups_accessorBundleWithDependencyToGroupedMember_dependencyIgnoredBundleBeforeSingleton() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-with-dependency", true, List.of(OrderingRule.ALPHA));
        CtTypeMember getValueMethodMember = requireFixtureMemberBySimpleName("getValue");
        CtTypeMember middleMethodMember = requireFixtureMemberBySimpleName("middleMethod");
        CtTypeMember setValueMethodMember = requireFixtureMemberBySimpleName("setValue");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(middleMethodMember, setValueMethodMember, getValueMethodMember));
        MemberDependencyGraph dependencyGraph = mock(MemberDependencyGraph.class);
        // getValue and setValue form an accessor bundle; middleMethod is a standalone singleton
        doReturn(Set.of(setValueMethodMember))
                .when(dependencyGraph)
                .findDirectDependents(getValueMethodMember, Constants.ACCESSOR_BUNDLE_ONLY);
        // middleMethod has a declaration dependency to setValue, but setValue is in a group:
        // SimplifiedDependencyAwareSorter requires groups and dependencies to be mutually exclusive,
        // so this edge is filtered out during dependency construction.
        doReturn(Set.of(setValueMethodMember))
                .when(dependencyGraph)
                .findDirectDependents(middleMethodMember, Constants.DECLARATION_DEPENDENCY_ONLY);

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then — the {getValue, setValue} bundle compares by its anchor's alphaKey ("getValue():int", g)
        // against the singleton middleMethod ("middleMethod():void", m); g < m so the bundle sorts first.
        // The declaration dependency from middleMethod to setValue is ignored because setValue is grouped.
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(getValueMethodMember, setValueMethodMember, middleMethodMember);
    }

    @Test
    void orderMembersInsideGroups_keepAccessorsTogetherDisabledWithDifferentPropertyNames_orderByFullMethodName() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessors-disabled-multi-property", false, List.of(OrderingRule.ALPHA));
        CtTypeMember isActiveMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "isActive");
        CtTypeMember setActiveMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "setActive");
        CtTypeMember getBalanceMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "getBalance");
        CtTypeMember setBalanceMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "setBalance");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(setBalanceMethodMember, isActiveMethodMember, setActiveMethodMember, getBalanceMethodMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                isActiveMethodMember, compiledMemberGroup,
                setActiveMethodMember, compiledMemberGroup,
                getBalanceMethodMember, compiledMemberGroup,
                setBalanceMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then — without accessor clustering, all methods sort by their full method name:
        // "getBalance():int" (g) < "isActive():boolean" (i) < "setActive(boolean):void" (sA) < "setBalance(int):void"
        // (sB).
        // Property-name–based ordering ("active" < "balance") must NOT apply here.
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        getBalanceMethodMember, isActiveMethodMember, setActiveMethodMember, setBalanceMethodMember);
    }

    @Test
    void orderMembersInsideGroups_isAccessorClusterAlpha_clustersOrderedByPropertyName() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "accessor-cluster-property-name", true, List.of(OrderingRule.ALPHA));
        CtTypeMember isActiveMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "isActive");
        CtTypeMember setActiveMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "setActive");
        CtTypeMember getBalanceMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "getBalance");
        CtTypeMember setBalanceMethodMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS, "setBalance");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(getBalanceMethodMember, isActiveMethodMember, setActiveMethodMember, setBalanceMethodMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                isActiveMethodMember, compiledMemberGroup,
                setActiveMethodMember, compiledMemberGroup,
                getBalanceMethodMember, compiledMemberGroup,
                setBalanceMethodMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then — both accessor pairs join the indivisible accessor super-cluster. Inside the
        // super-cluster the two property clusters ("active", "balance") are compared by ALPHA;
        // the cross-cluster ALPHA branch compares property names ("active" < "balance"), so
        // the active cluster comes before the balance cluster. Within each cluster the members
        // fall back to their own alphaKey: "isActive" < "setActive"; "getBalance" < "setBalance".
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        isActiveMethodMember, setActiveMethodMember, getBalanceMethodMember, setBalanceMethodMember);
    }

    @Test
    void
            orderMembersInsideGroups_blankFinalWithStaticInitializerAndDependentField_fieldAndInitializerOrderedBeforeDependent() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-static-tie", false, List.of(OrderingRule.ALPHA));

        CtTypeMember readFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS, "READ");
        CtTypeMember valueFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS, "VALUE");
        CtTypeMember staticInitializerMember = Constants.FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS.stream()
                .filter(CtAnonymousExecutable.class::isInstance)
                .filter(typeMember -> typeMember.getModifiers().contains(ModifierKind.STATIC))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Static initializer was not found in tie fixture"));

        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(staticInitializerMember, readFieldMember, valueFieldMember));

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

    @Test
    @ResourceLock(Resources.LOCALE)
    void orderMembersInsideGroups_orderingRuleAlphaWithPolishLocale_keepLocaleIndependentOrder() {
        // Given
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"));
            CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                    "alpha-locale", false, List.of(OrderingRule.ALPHA));
            CtTypeMember lublinFieldMember =
                    SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.LOCALE_FIXTURE_MEMBERS, "lublin");
            CtTypeMember uppercaseLStrokeFieldMember =
                    SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.LOCALE_FIXTURE_MEMBERS, "Łodz");
            CtTypeMember lowercaseLStrokeFieldMember =
                    SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.LOCALE_FIXTURE_MEMBERS, "łan");
            MemberGroupBlock inputBlock = new MemberGroupBlock(
                    compiledMemberGroup,
                    List.of(uppercaseLStrokeFieldMember, lowercaseLStrokeFieldMember, lublinFieldMember));
            MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                    lublinFieldMember, compiledMemberGroup,
                    uppercaseLStrokeFieldMember, compiledMemberGroup,
                    lowercaseLStrokeFieldMember, compiledMemberGroup));

            // When
            List<MemberGroupBlock> orderedBlocks =
                    GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

            // Then
            assertThat(orderedBlocks.getFirst().getTypeMembers())
                    .containsExactly(lublinFieldMember, uppercaseLStrokeFieldMember, lowercaseLStrokeFieldMember);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void orderMembersInsideGroups_orderingRuleAlphaWithEqualAlphaKey_applySrcStartTieBreaker() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-source-start-tie", false, List.of(OrderingRule.ALPHA));
        CtTypeMember firstStaticInitializerMember = Constants.SOURCE_START_TIE_FIXTURE_MEMBERS.stream()
                .filter(CtAnonymousExecutable.class::isInstance)
                .filter(typeMember -> typeMember.getModifiers().contains(ModifierKind.STATIC))
                .min(Comparator.comparingInt(
                        typeMember -> typeMember.getPosition().getSourceStart()))
                .orElseThrow(() -> new IllegalStateException(
                        "First static initializer was not found in source-start tie fixture"));
        CtTypeMember secondStaticInitializerMember = Constants.SOURCE_START_TIE_FIXTURE_MEMBERS.stream()
                .filter(CtAnonymousExecutable.class::isInstance)
                .filter(typeMember -> typeMember.getModifiers().contains(ModifierKind.STATIC))
                .filter(typeMember -> typeMember != firstStaticInitializerMember)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Second static initializer was not found in source-start tie fixture"));
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup, List.of(secondStaticInitializerMember, firstStaticInitializerMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                firstStaticInitializerMember, compiledMemberGroup,
                secondStaticInitializerMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(firstStaticInitializerMember, secondStaticInitializerMember);
    }

    @Test
    void orderMembersInsideGroups_uriDependencyChainShuffled_chooseEligibleMembersByComparator() {
        // Given
        CompiledMemberGroup compiledMemberGroup = CompiledMemberGroupTestCreator.createCompiledMemberGroup(
                "alpha-uri-chain", false, List.of(OrderingRule.ALPHA));
        CtTypeMember httpLocalhostUriFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.URI_REGRESSION_FIXTURE_MEMBERS, "HTTP_LOCALHOST_URI");
        CtTypeMember parameterNameFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.URI_REGRESSION_FIXTURE_MEMBERS, "PARAMETER_NAME");
        CtTypeMember resourcesPathSegmentFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.URI_REGRESSION_FIXTURE_MEMBERS, "RESOURCES_PATH_SEGMENT");
        CtTypeMember httpLocalhostResourcesUriFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.URI_REGRESSION_FIXTURE_MEMBERS, "HTTP_LOCALHOST_RESOURCES_URI");
        CtTypeMember httpLocalhostQueryUriFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                Constants.URI_REGRESSION_FIXTURE_MEMBERS, "HTTP_LOCALHOST_QUERY_URI");
        MemberGroupBlock inputBlock = new MemberGroupBlock(
                compiledMemberGroup,
                List.of(
                        httpLocalhostQueryUriFieldMember,
                        httpLocalhostResourcesUriFieldMember,
                        resourcesPathSegmentFieldMember,
                        parameterNameFieldMember,
                        httpLocalhostUriFieldMember));
        MemberDependencyGraph dependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(Map.of(
                httpLocalhostUriFieldMember, compiledMemberGroup,
                parameterNameFieldMember, compiledMemberGroup,
                resourcesPathSegmentFieldMember, compiledMemberGroup,
                httpLocalhostResourcesUriFieldMember, compiledMemberGroup,
                httpLocalhostQueryUriFieldMember, compiledMemberGroup));

        // When
        List<MemberGroupBlock> orderedBlocks =
                GroupMembersOrderer.orderMembersInsideGroups(List.of(inputBlock), dependencyGraph);

        // Then
        assertThat(orderedBlocks.getFirst().getTypeMembers())
                .containsExactly(
                        httpLocalhostUriFieldMember,
                        parameterNameFieldMember,
                        resourcesPathSegmentFieldMember,
                        httpLocalhostResourcesUriFieldMember,
                        httpLocalhostQueryUriFieldMember);
    }

    @NonNull
    private static CtTypeMember requireFixtureMemberBySimpleName(String expectedSimpleName) {
        return SpoonTestCaseUtils.requireTypeMemberBySimpleName(Constants.FIXTURE_MEMBERS, expectedSimpleName);
    }

    @NonNull
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

    @NonNull
    private static List<String> extractParameterTypeQualifiedNames(CtMethod<?> method) {
        return method.getParameters().stream()
                .map(parameter -> parameter.getType().getQualifiedName())
                .toList();
    }

    @NonNull
    private static CtTypeMember createFieldTypeMember(String fieldName, int srcStart) {
        CtField<?> fieldTypeMember = mock(CtField.class);
        SourcePosition srcPosition = mock(SourcePosition.class);
        CtTypeReference<?> fieldTypeReference = mock(CtTypeReference.class);
        when(srcPosition.isValidPosition()).thenReturn(true);
        when(srcPosition.getSourceStart()).thenReturn(srcStart);
        when(fieldTypeMember.getPosition()).thenReturn(srcPosition);
        when(fieldTypeMember.getSimpleName()).thenReturn(fieldName);
        doReturn(fieldTypeReference).when(fieldTypeMember).getType();
        when(fieldTypeReference.getQualifiedName()).thenReturn("int");
        return fieldTypeMember;
    }

    private static final class Constants {

        private static final String FIELD_INITIALIZER_TIE_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleFieldInitializerTieFixture.java";
        private static final URL FIELD_INITIALIZER_TIE_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(
                        FIELD_INITIALIZER_TIE_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> FIELD_INITIALIZER_TIE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIELD_INITIALIZER_TIE_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> FIELD_INITIALIZER_TIE_FIXTURE_MEMBERS = streamExplicitSrcTypeMembers(
                        FIELD_INITIALIZER_TIE_FIXTURE_MAIN_TYPE)
                .toList();

        private static final String SOURCE_START_TIE_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleSourceStartTieFixture.java";
        private static final URL SOURCE_START_TIE_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(SOURCE_START_TIE_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> SOURCE_START_TIE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(SOURCE_START_TIE_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> SOURCE_START_TIE_FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(SOURCE_START_TIE_FIXTURE_MAIN_TYPE).toList();

        private static final String LOCALE_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleLocaleFixture.java";
        private static final URL LOCALE_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(LOCALE_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> LOCALE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(LOCALE_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> LOCALE_FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(LOCALE_FIXTURE_MAIN_TYPE).toList();

        private static final String IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleImplicitConstantSourceOrderFixture.java";
        private static final URL IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(
                        IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(IMPLICIT_CONSTANT_SOURCE_ORDER_FIXTURE_MAIN_TYPE)
                        .toList();

        private static final String URI_REGRESSION_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/e2e/regression/05-uri-field-initializer-string-forward-reference/input/UriFieldInitializerStringForwardReferenceRegressionSample.java";
        private static final URL URI_REGRESSION_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(URI_REGRESSION_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> URI_REGRESSION_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(URI_REGRESSION_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> URI_REGRESSION_FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(URI_REGRESSION_FIXTURE_MAIN_TYPE).toList();

        private static final String FIXTURE_CLASSPATH_RESOURCE =
                "/" + TEST_CASES_DIR + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleFixture.java";
        private static final URL FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(FIXTURE_MAIN_TYPE).toList();

        private static final Set<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
                EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
        private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        private static final String ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_CLASSPATH_RESOURCE = "/" + TEST_CASES_DIR
                + "/core/sorter/spoon/group-ordering-rule/valid/GroupOrderingRuleAccessorClusterPropertyNameFixture.java";
        private static final URL ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_RESOURCE_URL =
                GroupMembersOrdererOrderingRulesTest.class.getResource(
                        ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_CLASSPATH_RESOURCE);
        private static final CtType<?> ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_RESOURCE_URL);
        private static final List<CtTypeMember> ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MEMBERS =
                streamExplicitSrcTypeMembers(ACCESSOR_CLUSTER_PROPERTY_NAME_FIXTURE_MAIN_TYPE)
                        .toList();

        private Constants() {}
    }
}
