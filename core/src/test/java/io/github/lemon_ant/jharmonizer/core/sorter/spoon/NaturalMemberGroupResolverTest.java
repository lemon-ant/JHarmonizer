package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.testutils.CompiledConfigTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class NaturalMemberGroupResolverTest {

    private static final URL CONFIG_RESOURCE_URL = TestCaseResourceUtils.requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/natural-group-resolution/natural-group-resolution-config.yml");
    private static final URL FIXTURE_DIRECTORY_RESOURCE_URL = TestCaseResourceUtils.requireClasspathDirectoryUrl(
            "/test-cases/core/sorter/spoon/natural-group-resolution/valid/");

    @Test
    void resolveNaturalGroups_whenFieldMatchesNestedGroups_shouldSelectDeepestMatchingGroup() {
        // Given
        URL javaFixtureResourceUrl = TestCaseResourceUtils.resolveRelativeUrl(
                FIXTURE_DIRECTORY_RESOURCE_URL, "NaturalGroupResolutionType.java");
        CtType<?> parsedMainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(javaFixtureResourceUrl);
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(parsedMainType);
        CtTypeMember alphaFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(describedMembers, "alpha");
        CtTypeMember betaFieldMember = SpoonTestCaseUtils.requireTypeMemberBySimpleName(describedMembers, "beta");
        CompiledMemberGroup rootMemberGroup =
                CompiledConfigTestCaseUtils.compileSingleRootMemberGroupFromJHarmonizerConfigResource(
                        CONFIG_RESOURCE_URL);

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedNaturalGroups =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, describedMembers);

        // Then
        assertThat(resolvedNaturalGroups).containsKeys(alphaFieldMember, betaFieldMember);
        assertThat(resolvedNaturalGroups.get(alphaFieldMember).getName()).isEqualTo("Alpha fields");
        assertThat(resolvedNaturalGroups.get(betaFieldMember).getName()).isEqualTo("Any fields");
    }

    @Test
    void resolveNaturalGroups_whenNoSpecificGroupMatches_shouldFallbackToDefaultRuleGroup() {
        // Given
        URL javaFixtureResourceUrl = TestCaseResourceUtils.resolveRelativeUrl(
                FIXTURE_DIRECTORY_RESOURCE_URL, "NaturalGroupResolutionType.java");
        CtType<?> parsedMainType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(javaFixtureResourceUrl);
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(parsedMainType);
        CtTypeMember nestedTypeMember =
                SpoonTestCaseUtils.requireTypeMemberBySimpleName(describedMembers, "NestedType");
        CompiledMemberGroup rootMemberGroup =
                CompiledConfigTestCaseUtils.compileSingleRootMemberGroupFromJHarmonizerConfigResource(
                        CONFIG_RESOURCE_URL);

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedNaturalGroups =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, describedMembers);

        // Then
        assertThat(resolvedNaturalGroups).containsKey(nestedTypeMember);
        assertThat(resolvedNaturalGroups.get(nestedTypeMember).getName()).isEqualTo("Default Rule");
        assertThat(resolvedNaturalGroups.values())
                .noneMatch(resolvedGroup -> rootMemberGroup.getName().equals(resolvedGroup.getName()));
    }
}
