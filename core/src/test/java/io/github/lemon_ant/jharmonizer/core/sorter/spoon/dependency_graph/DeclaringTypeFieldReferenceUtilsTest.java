package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSourceTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.requireTypeMemberBySimpleName;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.getTestResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class DeclaringTypeFieldReferenceUtilsTest {

    private static final java.net.URL LAZY_CONTEXT_FIXTURE_RESOURCE = getTestResource(
            "/test-cases/core/sorter/spoon/dependency-graph/valid/DeclaringTypeFieldReferenceUtilsLazyContextFixture.java");

    @Test
    void findFieldsWrittenByMember_lambdaBodyWrite_isDetected() {
        // Given
        CtType<?> fixtureType = parseMainTypeFromJavaFixtureResource(LAZY_CONTEXT_FIXTURE_RESOURCE);
        Set<CtTypeMember> typeMembers = streamExplicitSourceTypeMembers(fixtureType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        CtField<?> lambdaWriterField = (CtField<?>) requireTypeMemberBySimpleName(typeMembers, "lambdaWriter");

        // When
        Set<CtField<?>> writtenFields = DeclaringTypeFieldReferenceUtils.findFieldsWrittenByMember(
                lambdaWriterField, lambdaWriterField.getDefaultExpression());

        // Then
        assertThat(writtenFields)
                .extracting(CtField::getSimpleName)
                .containsExactly("value");
    }

    @Test
    void findFieldsReadByMember_lambdaBodyRead_isIgnored() {
        // Given
        CtType<?> fixtureType = parseMainTypeFromJavaFixtureResource(LAZY_CONTEXT_FIXTURE_RESOURCE);
        Set<CtTypeMember> typeMembers = streamExplicitSourceTypeMembers(fixtureType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        CtField<?> lambdaWriterField = (CtField<?>) requireTypeMemberBySimpleName(typeMembers, "lambdaWriter");

        // When
        Set<CtField<?>> readFields =
                DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(lambdaWriterField, lambdaWriterField.getDefaultExpression());

        // Then
        assertThat(readFields).isEmpty();
    }
}
