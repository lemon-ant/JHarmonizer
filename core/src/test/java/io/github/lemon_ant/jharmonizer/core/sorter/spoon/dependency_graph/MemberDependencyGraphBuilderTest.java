package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroupTestCreator.createTrivialMemberGroup;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

class MemberDependencyGraphBuilderTest {

    private static final CompiledMemberGroup MEMBER_GROUP_WITH_ACCESSOR_BUNDLING =
            createTrivialMemberGroup("test-group-with-accessor-bundling", true);
    private static final CompiledMemberGroup MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING =
            createTrivialMemberGroup("test-group-without-accessor-bundling", false);

    private static final URL ACCESSOR_PAIR_FIXTURE_URL = MemberDependencyGraphBuilderTest.class.getResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/AccessorPairBuilderFixture.java");
    private static final CtType<?> ACCESSOR_PAIR_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(ACCESSOR_PAIR_FIXTURE_URL);
    private static final Map<CtTypeMember, CompiledMemberGroup> ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING =
            buildTypeMember2NaturalGroup(ACCESSOR_PAIR_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITH_ACCESSOR_BUNDLING);
    private static final Map<CtTypeMember, CompiledMemberGroup> ACCESSOR_PAIR_MEMBERS_WITHOUT_ACCESSOR_BUNDLING =
            buildTypeMember2NaturalGroup(ACCESSOR_PAIR_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
    private static final CtTypeMember GET_VALUE_METHOD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING, "getValue");
    private static final CtTypeMember SET_VALUE_METHOD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING, "setValue");

    private static final URL FIELD_INITIALIZER_FIXTURE_URL = MemberDependencyGraphBuilderTest.class.getResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/FieldInitializerBuilderFixture.java");
    private static final CtType<?> FIELD_INITIALIZER_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(FIELD_INITIALIZER_FIXTURE_URL);
    private static final Map<CtTypeMember, CompiledMemberGroup> FIELD_INITIALIZER_MEMBERS =
            buildTypeMember2NaturalGroup(FIELD_INITIALIZER_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
    private static final CtTypeMember BRAVO_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(FIELD_INITIALIZER_MEMBERS, "BRAVO");
    private static final CtTypeMember ALPHA_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(FIELD_INITIALIZER_MEMBERS, "ALPHA");

    private static final URL INITIALIZER_BLOCK_FIXTURE_URL = MemberDependencyGraphBuilderTest.class.getResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/InitializerBlockBuilderFixture.java");
    private static final CtType<?> INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(INITIALIZER_BLOCK_FIXTURE_URL);
    private static final Map<CtTypeMember, CompiledMemberGroup> INITIALIZER_BLOCK_MEMBERS =
            buildTypeMember2NaturalGroup(INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
    private static final CtTypeMember INITIALIZER_BLOCK_ALPHA_FIELD_MEMBER =
            SpoonTestCaseUtils.requireTypeMemberBySimpleName(INITIALIZER_BLOCK_MEMBERS, "ALPHA");
    private static final CtTypeMember STATIC_INITIALIZER_BLOCK_MEMBER =
            requireUniqueInitializerBlockMember(INITIALIZER_BLOCK_FIXTURE_MAIN_TYPE, true);

    private static final URL ENUM_CONSTANT_INITIALIZER_FIXTURE_URL = MemberDependencyGraphBuilderTest.class.getResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/EnumConstantInitializerBuilderFixture.java");
    private static final CtType<?> ENUM_CONSTANT_INITIALIZER_FIXTURE_MAIN_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(ENUM_CONSTANT_INITIALIZER_FIXTURE_URL);
    private static final Map<CtTypeMember, CompiledMemberGroup> ENUM_CONSTANT_INITIALIZER_MEMBERS =
            buildTypeMember2NaturalGroup(
                    ENUM_CONSTANT_INITIALIZER_FIXTURE_MAIN_TYPE, MEMBER_GROUP_WITHOUT_ACCESSOR_BUNDLING);
    /*
    private static final CtTypeMember ENUM_CONSTANT_BRAVO_MEMBER =
              SpoonTestCaseUtils.requireTypeMemberBySimpleName(ENUM_CONSTANT_INITIALIZER_MEMBERS, "BRAVO");
    private static final CtTypeMember ENUM_CONSTANT_ALPHA_MEMBER =
              SpoonTestCaseUtils.requireTypeMemberBySimpleName(ENUM_CONSTANT_INITIALIZER_MEMBERS, "ALPHA");
    */
    private static final URL BLANK_FINAL_FIXTURE_URL = MemberDependencyGraphBuilderTest.class.getResource(
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

    @Test
    void buildDependencyGraph_keepAccessorsTogetherEnabled_accessorBundleEdgesCreatedBetweenPairedAccessors() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(ACCESSOR_PAIR_MEMBERS_WITH_ACCESSOR_BUNDLING);
        // When
        Set<CtTypeMember> directProvidersOfSetter = memberDependencyGraph.findDirectProviders(
                SET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        Set<CtTypeMember> directProvidersOfGetter = memberDependencyGraph.findDirectProviders(
                GET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        // Then
        assertThat(directProvidersOfSetter).containsExactlyInAnyOrder(GET_VALUE_METHOD_MEMBER);
        assertThat(directProvidersOfGetter).containsExactlyInAnyOrder(SET_VALUE_METHOD_MEMBER);
    }

    @Test
    void buildDependencyGraph_keepAccessorsTogetherDisabled_noAccessorBundleEdgesCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(ACCESSOR_PAIR_MEMBERS_WITHOUT_ACCESSOR_BUNDLING);
        // When
        Set<CtTypeMember> directProvidersOfSetter = memberDependencyGraph.findDirectProviders(
                SET_VALUE_METHOD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE));
        // Then
        assertThat(directProvidersOfSetter).isEmpty();
    }

    @Test
    void buildDependencyGraph_fieldInitializerReferencesEarlierField_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(FIELD_INITIALIZER_MEMBERS);
        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                ALPHA_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        // Then
        assertThat(directProviders).containsExactly(BRAVO_FIELD_MEMBER);
    }

    @Test
    void buildDependencyGraph_initializerBlockReferencesEarlierField_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(INITIALIZER_BLOCK_MEMBERS);
        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                STATIC_INITIALIZER_BLOCK_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        // Then
        assertThat(directProviders).containsExactly(INITIALIZER_BLOCK_ALPHA_FIELD_MEMBER);
    }

    @Test
    @Disabled("TODO Enable when enum constants are supported in tests and graph building")
    void buildDependencyGraph_enumConstantInitializerReferencesEarlierConstant_declarationDependencyEdgeCreated() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(ENUM_CONSTANT_INITIALIZER_MEMBERS);
        /*
        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                ENUM_CONSTANT_ALPHA_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        // Then
        assertThat(directProviders).containsExactly(ENUM_CONSTANT_BRAVO_MEMBER);
        */
    }

    @Test
    void buildDependencyGraph_blankFinalReadOccursAfterAssignment_assignmentProviderAddedAsDependency() {
        // Given
        MemberDependencyGraph memberDependencyGraph =
                MemberDependencyGraphBuilder.buildDependencyGraph(BLANK_FINAL_MEMBERS);
        // When
        Set<CtTypeMember> directProviders = memberDependencyGraph.findDirectProviders(
                READ_AFTER_ASSIGNMENT_FIELD_MEMBER, EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY));
        // Then
        assertThat(directProviders)
                .containsExactlyInAnyOrder(BLANK_FINAL_FIELD_MEMBER, INSTANCE_INITIALIZER_BLOCK_MEMBER);
    }

    @Test
    void buildDependencyGraph_naturalGroupNull_illegalStateExceptionThrown() {
        // Given
        Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalGroup = new HashMap<>(FIELD_INITIALIZER_MEMBERS);
        typeMember2NaturalGroup.put(ALPHA_FIELD_MEMBER, null);
        // When
        Throwable thrownException =
                catchThrowable(() -> MemberDependencyGraphBuilder.buildDependencyGraph(typeMember2NaturalGroup));
        // Then
        assertThat(thrownException)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Natural group was not resolved");
    }

    private static Map<CtTypeMember, CompiledMemberGroup> buildTypeMember2NaturalGroup(
            CtType<?> mainType, CompiledMemberGroup compiledMemberGroup) {
        return streamExplicitSourceTypeMembers(mainType)
                .collect(Collectors.toUnmodifiableMap(typeMember -> typeMember, ignoredMember -> compiledMemberGroup));
    }

    private static CtTypeMember requireUniqueInitializerBlockMember(
            CtType<?> declaringType, boolean requiredStaticness) {
        List<CtAnonymousExecutable> initializerBlocks = streamExplicitSourceTypeMembers(declaringType)
                .filter(typeMember -> typeMember instanceof CtAnonymousExecutable)
                .map(typeMember -> (CtAnonymousExecutable) typeMember)
                .filter(initializerBlock ->
                        initializerBlock.getModifiers().contains(ModifierKind.STATIC) == requiredStaticness)
                .sorted((left, right) -> Integer.compare(requireSourceStart(left), requireSourceStart(right)))
                .toList();

        if (initializerBlocks.size() != 1) {
            throw new IllegalStateException("Expected exactly one initializer block. requiredStaticness="
                    + requiredStaticness + ", found=" + requiredStaticness + ", found=" + initializerBlocks.size());
        }

        return initializerBlocks.getFirst();
    }

    private static int requireSourceStart(CtTypeMember typeMember) {
        SourcePosition sourcePosition = typeMember.getPosition();
        if (sourcePosition != null && sourcePosition.isValidPosition()) {
            return sourcePosition.getSourceStart();
        }

        throw new IllegalStateException("Expected valid SourcePosition. member=" + typeMember);
    }
}
