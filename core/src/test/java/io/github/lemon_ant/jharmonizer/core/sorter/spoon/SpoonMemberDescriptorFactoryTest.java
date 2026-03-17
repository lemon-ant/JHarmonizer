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
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class SpoonMemberDescriptorFactoryTest {

    private static final URL TEST_CASES_RESOURCE_ROOT_URL = TestCaseResourceUtils.requireClasspathDirectoryUrl(
            "/test-cases/core/sorter/spoon/member-descriptor/valid/");
    private static final CtType<?> CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES =
            parseMainTypeFromResource("ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");
    private static final CtType<?> ENUM_WITH_CONSTANTS_AND_REGULAR_MEMBERS =
            parseMainTypeFromResource("EnumWithConstantsAndRegularMembers.java");
    private static final CtType<?> RECORD_WITH_EXPLICIT_METHOD_AND_NO_IMPLICIT_MEMBERS =
            parseMainTypeFromResource("RecordWithExplicitMethodAndNoImplicitMembers.java");

    @Test
    void describeMembers_classTypeParsed_describesAllExplicitMembers() {
        // Given
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
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
        List<String> actualNamedMembers = describedMembers.values().stream()
                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                .toList();
        long actualUnnamedMembersCount = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getName().isEmpty())
                .count();
        assertThat(actualNamedMembers).containsExactlyInAnyOrderElementsOf(expectedNamedMembers);
        assertThat(actualUnnamedMembersCount).isEqualTo(expectedUnnamedMembersCount);
    }

    @Test
    void describeMembers_classTypeParsed_classifiesFieldsWithAccessAndModifiers() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
        MemberDescriptor publicStaticFinalFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "PUBLIC_STATIC_FINAL_FIELD");
        MemberDescriptor protectedFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "protectedField");
        MemberDescriptor packageFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "packageField");
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
    void describeMembers_classTypeParsed_classifiesMethodsWithAccessAndModifiers() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
        MemberDescriptor privateStaticMethodDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "privateStaticMethod");
        MemberDescriptor abstractMethodDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "abstractMethod");
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
    void describeMembers_classTypeParsed_describesConstructorAsUnnamedMember() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
        MemberDescriptor constructorDescriptor =
                SpoonTestCaseUtils.requireUniqueMemberDescriptorByKind(describedMembers, CONSTRUCTOR);
        assertThat(constructorDescriptor.getName()).isEmpty();
        assertThat(constructorDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_classTypeParsed_classifiesInitializerBlocksByStaticModifier() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
        List<MemberDescriptor> initBlockDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == INIT_BLOCK)
                .toList();
        boolean hasStaticInitBlock = initBlockDescriptors.stream()
                .anyMatch(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().contains(STATIC));
        boolean hasInstanceInitBlock = initBlockDescriptors.stream()
                .anyMatch(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().isEmpty());
        assertThat(initBlockDescriptors).hasSize(2);
        assertThat(hasStaticInitBlock).isTrue();
        assertThat(hasInstanceInitBlock).isTrue();
    }

    @Test
    void describeMembers_classTypeParsed_classifiesNestedTypesByKind() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(CLASS_WITH_FIELDS_METHODS_INIT_BLOCKS_AND_NESTED_TYPES);

        // Then
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
        assertThat(nestedClassDescriptor.getMemberKind()).isEqualTo(TYPE_CLASS);
        assertThat(nestedInterfaceDescriptor.getMemberKind()).isEqualTo(TYPE_INTERFACE);
        assertThat(nestedEnumDescriptor.getMemberKind()).isEqualTo(TYPE_ENUM);
        assertThat(nestedAnnotationDescriptor.getMemberKind()).isEqualTo(TYPE_ANNOTATION);
        assertThat(nestedRecordDescriptor.getMemberKind()).isEqualTo(TYPE_RECORD);
    }

    @Test
    void describeMembers_enumTypeParsed_skipsEnumConstantsInThisVersion() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(ENUM_WITH_CONSTANTS_AND_REGULAR_MEMBERS);

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
    void describeMembers_enumTypeParsed_describesRegularEnumMembers() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(ENUM_WITH_CONSTANTS_AND_REGULAR_MEMBERS);

        // Then
        MemberDescriptor valueFieldDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "value");
        assertThat(valueFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(valueFieldDescriptor.getDeclarationModifiers()).contains(FINAL);
        assertThat(valueFieldDescriptor.getMemberAccess()).contains(PRIVATE);
    }

    @Test
    void describeMembers_recordTypeParsed_doesNotReturnImplicitTypeMembers() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(RECORD_WITH_EXPLICIT_METHOD_AND_NO_IMPLICIT_MEMBERS);

        // Then
        boolean anyImplicitTypeMemberReturned =
                describedMembers.keySet().stream().anyMatch(CtElement::isImplicit);
        assertThat(anyImplicitTypeMemberReturned).isFalse();
    }

    @Test
    void describeMembers_recordTypeParsed_describesExplicitMethods() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(RECORD_WITH_EXPLICIT_METHOD_AND_NO_IMPLICIT_MEMBERS);

        // Then
        MemberDescriptor sumMethodDescriptor =
                SpoonTestCaseUtils.requireMemberDescriptorByName(describedMembers, "sum");
        assertThat(sumMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(sumMethodDescriptor.getMemberAccess()).contains(PUBLIC);
    }

    @Test
    void describeMembers_recordTypeParsed_doesNotClassifyImplicitBackingFieldsAsExplicitFields() {
        // When
        Map<CtTypeMember, MemberDescriptor> describedMembers =
                SpoonMemberDescriptorFactory.describeMembers(RECORD_WITH_EXPLICIT_METHOD_AND_NO_IMPLICIT_MEMBERS);

        // Then
        List<String> describedFieldNames = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == FIELD)
                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                .toList();
        assertThat(describedFieldNames).doesNotContain("alpha");
        assertThat(describedFieldNames).doesNotContain("beta");
    }

    @NonNull
    private static CtType<?> parseMainTypeFromResource(String fileName) {
        URL javaFixtureResource = TestCaseResourceUtils.resolveRelativeUrl(TEST_CASES_RESOURCE_ROOT_URL, fileName);
        return SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(javaFixtureResource);
    }
}
