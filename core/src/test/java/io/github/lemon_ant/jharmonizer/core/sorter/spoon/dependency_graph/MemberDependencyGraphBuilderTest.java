package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

class MemberDependencyGraphBuilderTest {

    @Test
    void buildDependencyGraph_keepAccessorsTogetherEnabled_accessorBundleEdgesCreatedBetweenPairedAccessors() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING);

        // When
        Set<CtTypeMember> directProvidersOfSetter = memberDependencyGraph.findDirectProviders(
                Constants.SET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        Set<CtTypeMember> directProvidersOfGetter = memberDependencyGraph.findDirectProviders(
                Constants.GET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));

        // Then
        assertThat(directProvidersOfSetter).containsExactlyInAnyOrder(Constants.GET_VALUE_METHOD_MEMBER);
        assertThat(directProvidersOfGetter).containsExactlyInAnyOrder(Constants.SET_VALUE_METHOD_MEMBER);
    }

    @Test
    void buildDependencyGraph_keepAccessorsTogetherDisabled_noAccessorBundleEdgesCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.ACCESSOR_PAIR_MEMBERS_WITHOUT_ACCESSOR_BUNDLING);

        // When
        Set<CtTypeMember> directProvidersOfSetter = memberDependencyGraph.findDirectProviders(
                Constants.SET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));

        // Then
        assertThat(directProvidersOfSetter).isEmpty();
    }

    @Test
    void buildDependencyGraph_fieldInitializerReferencesEarlierField_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(Constants.FIELD_INITIALIZER_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.BRAVO_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_fieldInitializerReferencesCompileTimeConstant_constantExcludedFromDependencies() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.COMPILE_TIME_CONSTANT_EXCLUSION_ALPHA_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.COMPILE_TIME_CONSTANT_EXCLUSION_BRAVO_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_fieldInitializerReferencesCompileTimeConstant_createsSourceOrderConstraint() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS);

        // When
        Set<CtTypeMember> sourceOrderProviders = memberDependencyGraph.findDirectProviders(
                Constants.COMPILE_TIME_CONSTANT_EXCLUSION_ALPHA_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.SOURCE_ORDER_CONSTRAINT));

        // Then
        assertThat(sourceOrderProviders)
                .containsExactly(Constants.COMPILE_TIME_CONSTANT_EXCLUSION_CONSTANT_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_instanceFinalLiteralField_keepsDeclarationDependencyEdge() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.INSTANCE_FINAL_LITERAL_ALPHA_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.INSTANCE_FINAL_LITERAL_BRAVO_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReference_preservesSrcDeclarationOrder() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.EXPLICIT_THIS_FORWARD_REFERENCE_ALPHA_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitThisForwardRef_withStaticField_usesOnlyInstanceProvider() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactly(Constants.EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_ALPHA_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithExplicitDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithImplicitDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithFoldedDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardRefToFieldWithBooleanFalseDefault_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithCharZeroDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithNullDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceToFieldWithMinusZeroDefaultValue_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitThisForwardReferenceWithMethodReference_preservesSrcDeclarationOrder() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactly(Constants.EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_ALPHA_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitDeclaringTypeForwardReference_preservesSrcDeclarationOrder() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactly(Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_ALPHA_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitTypeForwardRef_finalNonConstant_keepsDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactly(
                        Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_ALPHA_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_explicitDeclaringTypeForwardReferenceToConstantVariable_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitTypeForwardRef_explicitDefault_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitTypeForwardRef_implicitDefault_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_explicitTypeForwardRef_nullDefault_noDeclarationDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_BRAVO_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).isEmpty();
    }

    @Test
    void buildDependencyGraph_initializerBlockReferencesCompileTimeConstant_constantExcludedFromDependencies() {
        // Given
        MemberDependencyGraph memberDependencyGraph = MemberDependencyGraphBuilder.buildDependencyGraph(
                Constants.INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_STATIC_INITIALIZER_BLOCK_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactly(Constants.INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_B_PROVIDER_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_initializerBlockReferencesEarlierField_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(Constants.INITIALIZER_BLOCK_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.STATIC_INITIALIZER_BLOCK_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.INITIALIZER_BLOCK_ALPHA_FIELD_MEMBER);
    }

    @Test
    @Disabled("TODO Enable when enum constants are supported in tests and graph building")
    void buildDependencyGraph_enumConstantInitializerReferencesEarlierConstant_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(Constants.ENUM_CONSTANT_INITIALIZER_MEMBERS);

        /* TODO Enable when enum constants are supported
        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.ENUM_CONSTANT_ALPHA_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders).containsExactly(Constants.ENUM_CONSTANT_BRAVO_MEMBER);
        */
    }

    @Test
    void buildDependencyGraph_blankFinalReadOccursAfterAssignment_assignmentProviderAddedAsDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(Constants.BLANK_FINAL_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.READ_AFTER_ASSIGNMENT_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactlyInAnyOrder(
                        Constants.BLANK_FINAL_FIELD_MEMBER, Constants.INSTANCE_INITIALIZER_BLOCK_MEMBER);
    }

    @Test
    void buildDependencyGraph_blankFinalStaticRead_hasDependenciesFromFieldAndStaticInitializerOnly() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(Constants.BLANK_FINAL_STATIC_READ_MEMBERS);

        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                Constants.BLANK_FINAL_STATIC_READ_FIELD_MEMBER,
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));

        // Then
        assertThat(directProviders)
                .containsExactlyInAnyOrder(
                        Constants.BLANK_FINAL_STATIC_FIELD_MEMBER,
                        Constants.BLANK_FINAL_STATIC_INITIALIZER_BLOCK_MEMBER);
        assertThat(memberDependencyGraph.findDirectProviders(
                        Constants.BLANK_FINAL_STATIC_INITIALIZER_BLOCK_MEMBER,
                        EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)))
                .doesNotContain(Constants.BLANK_FINAL_STATIC_FIELD_MEMBER);
        assertThat(memberDependencyGraph.findDirectProviders(
                        Constants.BLANK_FINAL_STATIC_FIELD_MEMBER,
                        EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)))
                .doesNotContain(Constants.BLANK_FINAL_STATIC_INITIALIZER_BLOCK_MEMBER);
    }

    @Test
    void buildDependencyGraph_naturalGroupNull_illegalStateExceptionThrown() {
        // Given
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalGroup =
                new HashMap<>(Constants.FIELD_INITIALIZER_MEMBERS);
        typeMember2NaturalGroup.put(Constants.ALPHA_FIELD_MEMBER, null);

        // When
        Throwable thrownException =
                catchThrowable(() -> MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalGroup));

        // Then
        assertThat(thrownException)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Natural group was not resolved");
    }

    @NonNull
    private static Map<CtTypeMember, CompiledMemberGroup> buildTypeMember2NaturalGroup(
            CtType<?> mainType, CompiledMemberGroup compiledMemberGroup) {
        return streamExplicitSrcTypeMembers(mainType)
                .collect(Collectors.toUnmodifiableMap(typeMember -> typeMember, ignoredMember -> compiledMemberGroup));
    }

    @NonNull
    private static CtTypeMember requireUniqueInitializerBlockMember(
            CtType<?> declaringType, boolean requiredStaticness) {
        List<CtAnonymousExecutable> initializerBlocks = streamExplicitSrcTypeMembers(declaringType)
                .filter(typeMember -> typeMember instanceof CtAnonymousExecutable)
                .map(typeMember -> (CtAnonymousExecutable) typeMember)
                .filter(initializerBlock ->
                        initializerBlock.getModifiers().contains(ModifierKind.STATIC) == requiredStaticness)
                .toList();

        if (initializerBlocks.size() != 1) {
            throw new IllegalStateException("Expected exactly one initializer block. requiredStaticness="
                    + requiredStaticness + ", found=" + requiredStaticness + ", found=" + initializerBlocks.size());
        }

        return initializerBlocks.getFirst();
    }

    private static final class Constants {
        private static final CompiledMemberGroup MEMBER_GROUP_WITH_ACCESSOR_BUNDLING =
                createTrivialMemberGroup("test-group-with-accessor-bundling", true);
        private static final CompiledMemberGroup MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING =
                createTrivialMemberGroup("test-group-without-accessor-bundling", false);

        private static final URL ACCESSOR_PAIR_FIXTURE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
                "/test-cases/core/sorter/spoon/dependency-graph/valid/AccessorPairBuilderFixture.java");
        private static final CtType<?> ACCESSOR_PAIR_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(ACCESSOR_PAIR_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING =
                buildTypeMember2NaturalGroup(ACCESSOR_PAIR_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITH_ACCESSOR_BUNDLING);
        private static final Map<CtTypeMember, CompiledMemberGroup> ACCESSOR_PAIR_MEMBERS_WITHOUT_ACCESSOR_BUNDLING =
                buildTypeMember2NaturalGroup(ACCESSOR_PAIR_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember GET_VALUE_METHOD_MEMBER = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING, "getValue");
        private static final CtTypeMember SET_VALUE_METHOD_MEMBER = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING, "setValue");

        private static final URL FIELD_INITIALIZER_FIXTURE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
                "/test-cases/core/sorter/spoon/dependency-graph/valid/FieldInitializerBuilderFixture.java");
        private static final CtType<?> FIELD_INITIALIZER_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIELD_INITIALIZER_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> FIELD_INITIALIZER_MEMBERS =
                buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(FIELD_INITIALIZER_MEMBERS, "BRAVO");
        private static final CtTypeMember ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(FIELD_INITIALIZER_MEMBERS, "ALPHA");

        private static final URL FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/FieldInitializerCompileTimeConstantExclusionFixture.java");
        private static final CtType<?> FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS = buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember COMPILE_TIME_CONSTANT_EXCLUSION_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS, "BRAVO");
        private static final CtTypeMember COMPILE_TIME_CONSTANT_EXCLUSION_CONSTANT_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS, "CONSTANT");
        private static final CtTypeMember COMPILE_TIME_CONSTANT_EXCLUSION_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS, "ALPHA");

        private static final URL FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/FieldInitializerInstanceFinalLiteralFixture.java");
        private static final CtType<?> FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_MEMBERS =
                buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember INSTANCE_FINAL_LITERAL_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_MEMBERS, "BRAVO");
        private static final CtTypeMember INSTANCE_FINAL_LITERAL_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_INSTANCE_FINAL_LITERAL_MEMBERS, "ALPHA");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceFixture.java");
        private static final CtType<?> FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MEMBERS = buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MEMBERS, "alpha");
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceWithStaticReferrerFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_MEMBERS, "alpha");
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_WITH_STATIC_REFERRER_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS = buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceImplicitDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceFoldedDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_FOLDED_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceBooleanFalseDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_BOOLEAN_FALSE_DEFAULT_VALUE_MEMBERS,
                                "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceCharZeroDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_CHAR_ZERO_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceNullDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceMinusZeroDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_MINUS_ZERO_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-this-forward-reference/FieldInitializerExplicitThisForwardReferenceMethodReferenceFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_MEMBERS, "alpha");
        private static final CtTypeMember EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_THIS_FORWARD_REFERENCE_METHOD_REFERENCE_MEMBERS, "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceFixture.java");
        private static final CtType<?> FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_MEMBERS = buildTypeMember2NaturalGroup(
                        FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_MEMBERS, "alpha");
        private static final CtTypeMember EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_MEMBERS, "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceConstantVariableFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_CONSTANT_VARIABLE_MEMBERS,
                                "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceFinalNonConstantFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_ALPHA_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_MEMBERS,
                                "alpha");
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_FINAL_NON_CONSTANT_MEMBERS,
                                "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceInstanceReferrerFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_INSTANCE_REFERRER_MEMBERS,
                                "bravo");

        private static final URL FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_DEFAULT_VALUE_MEMBERS, "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceImplicitDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_IMPLICIT_DEFAULT_VALUE_MEMBERS,
                                "bravo");

        private static final URL
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_URL =
                        TestCaseResourceUtils.requireClasspathResourceUrl(
                                "/test-cases/core/sorter/spoon/dependency-graph/valid/explicit-declaring-type-forward-reference/FieldInitializerExplicitDeclaringTypeForwardReferenceNullDefaultValueFixture.java");
        private static final CtType<?>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_MAIN_TYPE =
                        SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS =
                        buildTypeMember2NaturalGroup(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_FIXTURE_MAIN_TYPE,
                                MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember
                EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_BRAVO_FIELD_MEMBER =
                        SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                                FIELD_INITIALIZER_EXPLICIT_DECLARING_TYPE_FORWARD_REFERENCE_NULL_DEFAULT_VALUE_MEMBERS,
                                "bravo");

        private static final URL INITIALIZER_BLOCK_FIXTURE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
                "/test-cases/core/sorter/spoon/dependency-graph/valid/InitializerBlockBuilderFixture.java");
        private static final CtType<?> INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(INITIALIZER_BLOCK_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> INITIALIZER_BLOCK_MEMBERS =
                buildTypeMember2NaturalGroup(
                        INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember INITIALIZER_BLOCK_ALPHA_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(INITIALIZER_BLOCK_MEMBERS, "ALPHA");
        private static final CtTypeMember STATIC_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE, true);

        private static final URL INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/InitializerBlockCompileTimeConstantExclusionFixture.java");
        private static final CtType<?> INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(
                        INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup>
                INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS = buildTypeMember2NaturalGroup(
                        INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_MAIN_TYPE,
                        MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_B_PROVIDER_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS, "B_PROVIDER");
        private static final CtTypeMember INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_Z_CONSTANT_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                        INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_MEMBERS, "Z_CONSTANT");
        private static final CtTypeMember
                INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_STATIC_INITIALIZER_BLOCK_MEMBER =
                        requireUniqueInitializerBlockMember(
                                INITIALIZER_BLOCK_COMPILE_TIME_CONSTANT_EXCLUSION_FIXTURE_MAIN_TYPE, true);

        private static final URL ENUM_CONSTANT_INITIALIZER_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/EnumConstantInitializerBuilderFixture.java");
        private static final CtType<?> ENUM_CONSTANT_INITIALIZER_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(ENUM_CONSTANT_INITIALIZER_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> ENUM_CONSTANT_INITIALIZER_MEMBERS =
                buildTypeMember2NaturalGroup(
                        ENUM_CONSTANT_INITIALIZER_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        /* TODO Enable when enum constants are supported
        private static final CtTypeMember ENUM_CONSTANT_BRAVO_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(ENUM_CONSTANT_INITIALIZER_MEMBERS, "BRAVO");
        private static final CtTypeMember ENUM_CONSTANT_ALPHA_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(ENUM_CONSTANT_INITIALIZER_MEMBERS, "ALPHA");
        */

        private static final URL BLANK_FINAL_STATIC_READ_FIXTURE_URL =
                TestCaseResourceUtils.requireClasspathResourceUrl(
                        "/test-cases/core/sorter/spoon/dependency-graph/valid/BlankFinalStaticReadWithStaticInitializerFixture.java");
        private static final CtType<?> BLANK_FINAL_STATIC_READ_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(BLANK_FINAL_STATIC_READ_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> BLANK_FINAL_STATIC_READ_MEMBERS =
                buildTypeMember2NaturalGroup(
                        BLANK_FINAL_STATIC_READ_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember BLANK_FINAL_STATIC_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(BLANK_FINAL_STATIC_READ_MEMBERS, "STATIC_BLANK_FINAL");
        private static final CtTypeMember BLANK_FINAL_STATIC_READ_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(BLANK_FINAL_STATIC_READ_MEMBERS, "B_STATIC_READ");
        private static final CtTypeMember BLANK_FINAL_STATIC_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(BLANK_FINAL_STATIC_READ_FIXTURE_MAIN_TYPE, true);

        private static final URL BLANK_FINAL_FIXTURE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
                "/test-cases/core/sorter/spoon/dependency-graph/valid/BlankFinalDefiniteAssignmentBuilderFixture.java");
        private static final CtType<?> BLANK_FINAL_FIXTURE_MAIN_TYPE =
                SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(BLANK_FINAL_FIXTURE_URL);
        private static final Map<CtTypeMember, CompiledMemberGroup> BLANK_FINAL_MEMBERS =
                buildTypeMember2NaturalGroup(BLANK_FINAL_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
        private static final CtTypeMember BLANK_FINAL_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(BLANK_FINAL_MEMBERS, "BLANK_FINAL");
        private static final CtTypeMember READ_AFTER_ASSIGNMENT_FIELD_MEMBER =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(BLANK_FINAL_MEMBERS, "READ_AFTER_ASSIGNMENT");
        private static final CtTypeMember INSTANCE_INITIALIZER_BLOCK_MEMBER =
                requireUniqueInitializerBlockMember(BLANK_FINAL_FIXTURE_MAIN_TYPE, false);

        private Constants() {}
    }
}
