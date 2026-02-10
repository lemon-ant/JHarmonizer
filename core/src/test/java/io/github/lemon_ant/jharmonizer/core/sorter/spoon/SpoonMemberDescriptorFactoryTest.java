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

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Review
class SpoonMemberDescriptorFactoryTest {

    private static final URL TEST_CASES_RESOURCE_ROOT_URL =SpoonMemberDescriptorFactoryTest.class.getResource(
            "/test-cases/core/sorter/spoon/member-descriptor/valid/");

    private static CtType<?> parseMainTypeFromResource(String fileName) {
        URL javaFixtureResource = TestCaseResourceUtils.resolveRelativeUrl(TEST_CASES_RESOURCE_ROOT_URL, fileName);
        return SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(javaFixtureResource, Path.of(fileName));
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldDescribeAllExplicitMembers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");
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
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor publicStaticFinalFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "PUBLIC_STATIC_FINAL_FIELD");
        MemberDescriptor protectedFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "protectedField");
        MemberDescriptor packageFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "packageField");

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
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor privateStaticMethodDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "privateStaticMethod");
        MemberDescriptor abstractMethodDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "abstractMethod");

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
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor constructorDescriptor =
                SpoonTestCaseUtils.requireUniqueMemberDescriptorByKind(describedMembers, CONSTRUCTOR);

        // Then
        assertThat(constructorDescriptor.getName()).isEmpty();
        assertThat(constructorDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_whenClassTypeParsed_shouldClassifyInitializerBlocksByStaticModifier() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");

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
        CtType<?> parsedType = parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor nestedClassDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "PublicNestedClass");
        MemberDescriptor nestedInterfaceDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "ProtectedNestedInterface");
        MemberDescriptor nestedEnumDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "NestedEnum");
        MemberDescriptor nestedAnnotationDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "NestedAnnotation");
        MemberDescriptor nestedRecordDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "NestedRecord");

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
        CtType<?> parsedType = parseMainTypeFromResource("EnumWithConstantsAndRegularMembers.java");

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
        CtType<?> parsedType = parseMainTypeFromResource("EnumWithConstantsAndRegularMembers.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor valueFieldDescriptor = SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "value");

        // Then
        assertThat(valueFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(valueFieldDescriptor.getDeclarationModifiers()).contains(FINAL);
        assertThat(valueFieldDescriptor.getMemberAccess()).contains(PRIVATE);
    }

    @Test
    void describeMembers_whenRecordTypeParsed_shouldNotReturnImplicitTypeMembers() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("RecordWithExplicitMethodAndNoImplicitMembers.java");

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
        CtType<?> parsedType = parseMainTypeFromResource("RecordWithExplicitMethodAndNoImplicitMembers.java");

        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);
        MemberDescriptor sumMethodDescriptor = SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "sum");

        // Then
        assertThat(sumMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(sumMethodDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_whenRecordTypeParsed_shouldNotClassifyImplicitBackingFieldsAsExplicitFields() {
        // Given
        CtType<?> parsedType = parseMainTypeFromResource("RecordWithExplicitMethodAndNoImplicitMembers.java");

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
