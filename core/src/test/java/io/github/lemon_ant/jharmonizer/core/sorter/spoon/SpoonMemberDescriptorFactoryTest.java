package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier.ABSTRACT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier.FINAL;
import static io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier.STATIC;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PACKAGE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PRIVATE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PROTECTED;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess.PUBLIC;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.CONSTRUCTOR;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.FIELD;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.INIT_BLOCK;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.METHOD;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_ANNOTATION;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_CLASS;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_ENUM;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_INTERFACE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_RECORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Review and guarantee test-case compilation
class SpoonMemberDescriptorFactoryTest {

    private static final String TEST_CASES_RESOURCE_ROOT = "/test-cases/core/sorter/spoon/member-descriptor";

    private static CtType<?> parseMainTypeFromResource(String fileName) {
        String resourcePath = TEST_CASES_RESOURCE_ROOT + "/" + fileName;
        String sourceCode = readResourceAsString(resourcePath);
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of(fileName), sourceCode);
        Optional<CtType<?>> mainType = spoonAstModel.getMainType();
        if (mainType.isEmpty()) {
            fail("Expected a main type to be detected for resource: " + resourcePath);
        }
        return mainType.orElseThrow();
    }

    private static String readResourceAsString(String classpathResourcePath) {
        try (InputStream inputStream =
                SpoonMemberDescriptorFactoryTest.class.getResourceAsStream(classpathResourcePath)) {
            if (inputStream == null) {
                fail("Missing test resource: " + classpathResourcePath);
            }
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read test resource: " + classpathResourcePath, exception);
        }
    }

    private static MemberDescriptor findDescriptorByNameOrFail(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedName) {
        return describedMembers.values().stream()
                .filter(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals(expectedName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No member descriptor found for name: " + expectedName
                        + ". Available named members: "
                        + describedMembers.values().stream()
                                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                                .sorted()
                                .toList()));
    }

    private static MemberDescriptor findUniqueDescriptorByKindOrFail(
            Map<CtTypeMember, MemberDescriptor> describedMembers, MemberKind expectedKind) {
        List<MemberDescriptor> matchingDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == expectedKind)
                .sorted(java.util.Comparator.comparing(
                        memberDescriptor -> memberDescriptor.getName().orElse("<unnamed>")))
                .toList();

        assertThat(matchingDescriptors)
                .withFailMessage(
                        "Expected exactly one descriptor with kind %s, but found: %s",
                        expectedKind, matchingDescriptors)
                .hasSize(1);

        return matchingDescriptors.getFirst();
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldDescribeAllExplicitMembers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");
        List<String> expectedNamedMembers = List.of(
                "PUBLIC_STATIC_FINAL_FIELD",
                "protectedField",
                "packageField",
                "privateStaticMethod",
                "abstractMethod",
                "PublicNestedClass",
                "ProtectedNestedInterface",
                "NestedEnum",
                "NestedAnnotation",
                "NestedRecord");
        int expectedUnnamedMembersCount = 3; // constructor + 2 init-blocks

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        List<String> actualNamedMembers = describedMembers.values().stream()
                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                .sorted()
                .toList();
        long actualUnnamedMembersCount = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getName().isEmpty())
                .count();

        // Then
        assertThat(actualNamedMembers).containsExactlyInAnyOrderElementsOf(expectedNamedMembers);
        assertThat(actualUnnamedMembersCount).isEqualTo(expectedUnnamedMembersCount);
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldClassifyFieldsWithAccessAndModifiers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor publicStaticFinalFieldDescriptor =
                findDescriptorByNameOrFail(describedMembers, "PUBLIC_STATIC_FINAL_FIELD");
        MemberDescriptor protectedFieldDescriptor = findDescriptorByNameOrFail(describedMembers, "protectedField");
        MemberDescriptor packageFieldDescriptor = findDescriptorByNameOrFail(describedMembers, "packageField");

        // Then
        assertThat(publicStaticFinalFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(publicStaticFinalFieldDescriptor.getMemberAccess()).contains(PUBLIC);
        assertThat(publicStaticFinalFieldDescriptor.getDeclarationModifiers()).containsExactlyInAnyOrder(STATIC, FINAL);
        assertThat(publicStaticFinalFieldDescriptor.getAnnotationQualifiedNames())
                .contains("Deprecated")
                .contains("java.lang.Deprecated");
        assertThat(protectedFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(protectedFieldDescriptor.getMemberAccess()).contains(PROTECTED);
        assertThat(protectedFieldDescriptor.getDeclarationModifiers()).isEmpty();
        assertThat(packageFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(packageFieldDescriptor.getMemberAccess()).contains(PACKAGE);
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldClassifyMethodsWithAccessAndModifiers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor privateStaticMethodDescriptor =
                findDescriptorByNameOrFail(describedMembers, "privateStaticMethod");
        MemberDescriptor abstractMethodDescriptor = findDescriptorByNameOrFail(describedMembers, "abstractMethod");

        // Then
        assertThat(privateStaticMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(privateStaticMethodDescriptor.getMemberAccess()).contains(PRIVATE);
        assertThat(privateStaticMethodDescriptor.getDeclarationModifiers()).containsExactly(STATIC);
        assertThat(privateStaticMethodDescriptor.getAnnotationQualifiedNames())
                .contains("Deprecated")
                .contains("java.lang.Deprecated");
        assertThat(abstractMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(abstractMethodDescriptor.getMemberAccess()).contains(PUBLIC);
        assertThat(abstractMethodDescriptor.getDeclarationModifiers()).containsExactly(ABSTRACT);
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldDescribeConstructorAsUnnamedMember() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor constructorDescriptor = findUniqueDescriptorByKindOrFail(describedMembers, CONSTRUCTOR);

        // Then
        assertThat(constructorDescriptor.getName()).isEmpty();
        assertThat(constructorDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldClassifyInitializerBlocksByStaticModifier() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        List<MemberDescriptor> initBlockDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == INIT_BLOCK)
                .toList();
        boolean hasStaticInitBlock = initBlockDescriptors.stream()
                .anyMatch(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().contains(STATIC));
        boolean hasInstanceInitBlock = initBlockDescriptors.stream()
                .anyMatch(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().isEmpty());

        // Then
        assertThat(initBlockDescriptors).hasSize(2);
        assertThat(hasStaticInitBlock).isTrue();
        assertThat(hasInstanceInitBlock).isTrue();
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldClassifyNestedTypesByKind() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidClass.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor nestedClassDescriptor = findDescriptorByNameOrFail(describedMembers, "PublicNestedClass");
        MemberDescriptor nestedInterfaceDescriptor =
                findDescriptorByNameOrFail(describedMembers, "ProtectedNestedInterface");
        MemberDescriptor nestedEnumDescriptor = findDescriptorByNameOrFail(describedMembers, "NestedEnum");
        MemberDescriptor nestedAnnotationDescriptor = findDescriptorByNameOrFail(describedMembers, "NestedAnnotation");
        MemberDescriptor nestedRecordDescriptor = findDescriptorByNameOrFail(describedMembers, "NestedRecord");

        // Then
        assertThat(nestedClassDescriptor.getMemberKind()).isEqualTo(TYPE_CLASS);
        assertThat(nestedInterfaceDescriptor.getMemberKind()).isEqualTo(TYPE_INTERFACE);
        assertThat(nestedEnumDescriptor.getMemberKind()).isEqualTo(TYPE_ENUM);
        assertThat(nestedAnnotationDescriptor.getMemberKind()).isEqualTo(TYPE_ANNOTATION);
        assertThat(nestedRecordDescriptor.getMemberKind()).isEqualTo(TYPE_RECORD);
    }

    @Test
    void describeMembers_whenEnumTypeParsed_shouldSkipEnumConstantsInThisVersion() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidEnum.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);

        // Then
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals("FIRST"));
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals("SECOND"));
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getMemberKind().name().equals("ENUM_CONSTANT"));
    }

    @Test
    void describeMembers_whenEnumTypeParsed_shouldDescribeRegularEnumMembers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidEnum.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor valueFieldDescriptor = findDescriptorByNameOrFail(describedMembers, "value");

        // Then
        assertThat(valueFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(valueFieldDescriptor.getDeclarationModifiers()).contains(FINAL);
        assertThat(valueFieldDescriptor.getMemberAccess()).contains(PRIVATE);
    }

    @Test
    void describeMembers_whenRecordTypeParsed_shouldNotReturnImplicitTypeMembers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidRecord.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        boolean anyImplicitTypeMemberReturned =
                describedMembers.keySet().stream().anyMatch(CtElement::isImplicit);

        // Then
        assertThat(anyImplicitTypeMemberReturned).isFalse();
    }

    @Test
    void describeMembers_whenRecordTypeParsed_shouldDescribeExplicitMethods() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidRecord.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor sumMethodDescriptor = findDescriptorByNameOrFail(describedMembers, "sum");

        // Then
        assertThat(sumMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(sumMethodDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_whenRecordTypeParsed_shouldNotClassifyImplicitBackingFieldsAsExplicitFields() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ValidRecord.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        List<String> describedFieldNames = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == FIELD)
                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                .sorted()
                .toList();

        // Then
        assertThat(describedFieldNames).doesNotContain("alpha");
        assertThat(describedFieldNames).doesNotContain("beta");
    }
}
