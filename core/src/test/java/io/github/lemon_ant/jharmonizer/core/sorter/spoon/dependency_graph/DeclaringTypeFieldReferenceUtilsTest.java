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

    @Test
    void findFieldsWrittenByMember_lambdaBodyWrite_isDetected() {
        CtType<?> fixtureType = parseMainTypeFromJavaFixtureResource(Constants.LAZY_CONTEXT_FIXTURE_RESOURCE);
        Set<CtTypeMember> typeMembers = streamExplicitSourceTypeMembers(fixtureType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        CtField<?> lambdaWriterField = (CtField<?>) requireTypeMemberBySimpleName(typeMembers, "lambdaWriter");

        Set<CtField<?>> writtenFields = DeclaringTypeFieldReferenceUtils.findFieldsWrittenByMember(
                lambdaWriterField, lambdaWriterField.getDefaultExpression());

        assertThat(writtenFields)
                .extracting(CtField::getSimpleName)
                .containsExactly("value");
    }

    @Test
    void findFieldsReadByMember_lambdaBodyRead_isIgnored() {
        CtType<?> fixtureType = parseMainTypeFromJavaFixtureResource(Constants.LAZY_CONTEXT_FIXTURE_RESOURCE);
        Set<CtTypeMember> typeMembers = streamExplicitSourceTypeMembers(fixtureType)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        CtField<?> lambdaWriterField = (CtField<?>) requireTypeMemberBySimpleName(typeMembers, "lambdaWriter");

        Set<CtField<?>> readFields =
                DeclaringTypeFieldReferenceUtils.findFieldsReadByMember(lambdaWriterField, lambdaWriterField.getDefaultExpression());

        assertThat(readFields).isEmpty();
    }

    private static final class Constants {
        private static final java.net.URL LAZY_CONTEXT_FIXTURE_RESOURCE = getTestResource(
                "/test-cases/core/sorter/spoon/dependency-graph/valid/DeclaringTypeFieldReferenceUtilsLazyContextFixture.java");

        private Constants() {}
    }
}
