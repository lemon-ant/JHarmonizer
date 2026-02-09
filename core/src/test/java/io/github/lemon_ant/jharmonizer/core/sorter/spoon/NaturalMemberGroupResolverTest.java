package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager.parseUnifiedConfigFromClasspathResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class NaturalMemberGroupResolverTest {

    private static final String CONFIG_RESOURCE_PATH =
            "/test-cases/core/sorter/spoon/natural-group-resolution/natural-group-resolution-config.yml";

    private static final String FIXTURE_RESOURCE_ROOT = "/test-cases/core/sorter/spoon/natural-group-resolution/valid";

    @Test
    void resolveNaturalGroups_whenFieldMatchesNestedGroups_shouldSelectDeepestMatchingGroup() {
        // Given
        CtType<?> parsedMainType = parseMainTypeFromResource("NaturalGroupResolutionType.java");
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(parsedMainType);
        CtTypeMember alphaFieldMember = findTypeMemberBySimpleNameOrFail(describedMembers, "alpha");
        CtTypeMember betaFieldMember = findTypeMemberBySimpleNameOrFail(describedMembers, "beta");
        CompiledMemberGroup rootMemberGroup = compileRootMemberGroupFromConfigResource();

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
        CtType<?> parsedMainType = parseMainTypeFromResource("NaturalGroupResolutionType.java");
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(parsedMainType);
        CtTypeMember nestedTypeMember = findTypeMemberBySimpleNameOrFail(describedMembers, "NestedType");
        CompiledMemberGroup rootMemberGroup = compileRootMemberGroupFromConfigResource();

        // When
        Map<CtTypeMember, CompiledMemberGroup> resolvedNaturalGroups =
                NaturalMemberGroupResolver.resolveNaturalGroups(rootMemberGroup, describedMembers);

        // Then
        assertThat(resolvedNaturalGroups).containsKey(nestedTypeMember);
        assertThat(resolvedNaturalGroups.get(nestedTypeMember).getName()).isEqualTo("Default Rule");
        assertThat(resolvedNaturalGroups.values())
                .noneMatch(resolvedGroup -> rootMemberGroup.getName().equals(resolvedGroup.getName()));
    }

    private static CtType<?> parseMainTypeFromResource(String fileName) {
        String classpathResourcePath = FIXTURE_RESOURCE_ROOT + "/" + fileName;
        String sourceCode = readClasspathResourceAsString(classpathResourcePath);
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of(fileName), sourceCode);
        Optional<CtType<?>> mainType = spoonAstModel.getMainType();
        if (mainType.isEmpty()) {
            fail("Expected a main type to be detected for resource: " + classpathResourcePath);
        }
        return mainType.orElseThrow();
    }

    private static String readClasspathResourceAsString(String classpathResourcePath) {
        try (InputStream inputStream =
                NaturalMemberGroupResolverTest.class.getResourceAsStream(classpathResourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing test resource: " + classpathResourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read test resource: " + classpathResourcePath, exception);
        }
    }

    private static CtTypeMember findTypeMemberBySimpleNameOrFail(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedSimpleName) {
        return describedMembers.keySet().stream()
                .filter(typeMember -> expectedSimpleName.equals(typeMember.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No type member found for simple name: " + expectedSimpleName));
    }

    private static CompiledMemberGroup compileRootMemberGroupFromConfigResource() {
        UnifiedConfig unifiedConfig = parseUnifiedConfigFromClasspathResource(CONFIG_RESOURCE_PATH);
        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(unifiedConfig);

        assertThat(compiledConfig.getRootMemberGroups())
                .as("This test expects exactly one root member group (the synthetic root) in the fixture config.")
                .hasSize(1);

        return compiledConfig.getRootMemberGroups().getFirst();
    }
}
