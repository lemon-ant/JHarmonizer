package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.requireTypeMemberBySimpleName;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class DeclaringTypeFieldReferenceUtilsTest {

    private static final URL LAZY_CONTEXT_FIXTURE_RESOURCE = requireClasspathResourceUrl(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/DeclaringTypeFieldReferenceUtilsLazyContextFixture.java");

    @Test
    void findFieldsWrittenByMember_lambdaBodyWrite_regressionForConfigurableLazyFilter() {
        // Given
        CtField<?> lambdaWriterField = requireLambdaWriterField();

        // When
        Set<CtField<?>> writtenFields = DeclaringTypeFieldReferenceUtils.findFieldsWrittenByMember(
                lambdaWriterField, lambdaWriterField.getDefaultExpression());

        // Then
        // This is a regression check: with old always-on lazy filtering this assertion failed.
        assertThat(writtenFields)
                .extracting(CtField::getSimpleName)
                .containsExactly("value");
    }

    @Test
    void findFieldsReadByMember_lambdaBodyRead_isIgnored() {
        // Given
        CtField<?> lambdaWriterField = requireLambdaWriterField();

        // When
        Set<CtField<?>> readFields =
                DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(lambdaWriterField, lambdaWriterField.getDefaultExpression());

        // Then
        assertThat(readFields).isEmpty();
    }

    private static CtField<?> requireLambdaWriterField() {
        CtType<?> fixtureType = parseMainTypeFromJavaFixtureResource(LAZY_CONTEXT_FIXTURE_RESOURCE);
        Set<CtTypeMember> typeMembers = streamExplicitSourceTypeMembers(fixtureType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return (CtField<?>) requireTypeMemberBySimpleName(typeMembers, "lambdaWriter");
    }
}
