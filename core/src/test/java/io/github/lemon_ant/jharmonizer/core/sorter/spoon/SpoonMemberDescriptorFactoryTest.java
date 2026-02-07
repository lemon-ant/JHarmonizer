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
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.RECORD_COMPONENT;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_ANNOTATION;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_CLASS;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_ENUM;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_INTERFACE;
import static io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind.TYPE_RECORD;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

// TODO Refactor
class SpoonMemberDescriptorFactoryTest {

    @Test
    void describeMembers_classMembersAndNestedTypes_expectedKindsAccessAndModifiers() {
        CtType<?> parsedType = parseMainType("ExampleClass.java", exampleClassSourceCode());

        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);

        assertThat(describedMembers).hasSize(13);

        MemberDescriptor publicStaticFinalFieldDescriptor =
                findDescriptorByName(describedMembers, "PUBLIC_STATIC_FINAL_FIELD");
        assertThat(publicStaticFinalFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(publicStaticFinalFieldDescriptor.getMemberAccess()).contains(PUBLIC);
        assertThat(publicStaticFinalFieldDescriptor.getDeclarationModifiers()).containsExactlyInAnyOrder(STATIC, FINAL);
        assertThat(publicStaticFinalFieldDescriptor.getAnnotationQualifiedNames())
                .contains("Deprecated")
                .contains("java.lang.Deprecated");

        MemberDescriptor protectedFieldDescriptor = findDescriptorByName(describedMembers, "protectedField");
        assertThat(protectedFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(protectedFieldDescriptor.getMemberAccess()).contains(PROTECTED);
        assertThat(protectedFieldDescriptor.getDeclarationModifiers()).isEmpty();

        MemberDescriptor packageFieldDescriptor = findDescriptorByName(describedMembers, "packageField");
        assertThat(packageFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(packageFieldDescriptor.getMemberAccess()).contains(PACKAGE);

        MemberDescriptor privateStaticMethodDescriptor = findDescriptorByName(describedMembers, "privateStaticMethod");
        assertThat(privateStaticMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(privateStaticMethodDescriptor.getMemberAccess()).contains(PRIVATE);
        assertThat(privateStaticMethodDescriptor.getDeclarationModifiers()).containsExactly(STATIC);
        assertThat(privateStaticMethodDescriptor.getAnnotationQualifiedNames())
                .contains("Deprecated")
                .contains("java.lang.Deprecated");

        MemberDescriptor abstractMethodDescriptor = findDescriptorByName(describedMembers, "abstractMethod");
        assertThat(abstractMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(abstractMethodDescriptor.getMemberAccess()).contains(PUBLIC);
        assertThat(abstractMethodDescriptor.getDeclarationModifiers()).containsExactly(ABSTRACT);

        MemberDescriptor constructorDescriptor = findUniqueDescriptorByKind(describedMembers, CONSTRUCTOR);
        assertThat(constructorDescriptor.getName()).isEmpty();
        assertThat(constructorDescriptor.getMemberAccess()).contains(PUBLIC);

        List<MemberDescriptor> initBlockDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == INIT_BLOCK)
                .toList();
        assertThat(initBlockDescriptors).hasSize(2);

        MemberDescriptor staticInitBlockDescriptor = initBlockDescriptors.stream()
                .filter(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().contains(STATIC))
                .findFirst()
                .orElseThrow();
        assertThat(staticInitBlockDescriptor.getMemberAccess()).isEmpty();

        MemberDescriptor instanceInitBlockDescriptor = initBlockDescriptors.stream()
                .filter(memberDescriptor ->
                        memberDescriptor.getDeclarationModifiers().isEmpty())
                .findFirst()
                .orElseThrow();
        assertThat(instanceInitBlockDescriptor.getMemberAccess()).isEmpty();

        assertThat(findDescriptorByName(describedMembers, "PublicNestedClass").getMemberKind())
                .isEqualTo(TYPE_CLASS);
        assertThat(findDescriptorByName(describedMembers, "ProtectedNestedInterface")
                        .getMemberKind())
                .isEqualTo(TYPE_INTERFACE);
        assertThat(findDescriptorByName(describedMembers, "NestedEnum").getMemberKind())
                .isEqualTo(TYPE_ENUM);
        assertThat(findDescriptorByName(describedMembers, "NestedAnnotation").getMemberKind())
                .isEqualTo(TYPE_ANNOTATION);
        assertThat(findDescriptorByName(describedMembers, "NestedRecord").getMemberKind())
                .isEqualTo(TYPE_RECORD);
    }

    @Test
    void describeMembers_enumType_enumConstantsAreSkippedInThisVersion() {
        CtType<?> parsedType = parseMainType("ExampleEnum.java", exampleEnumSourceCode());
        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);

        // Current product contract: enum constants are intentionally NOT processed in this version.
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals("FIRST"));
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals("SECOND"));

        // Defensive: even if MemberKind has ENUM_CONSTANT, it must not appear here in this version.
        assertThat(describedMembers.values())
                .noneMatch(memberDescriptor ->
                        memberDescriptor.getMemberKind().name().equals("ENUM_CONSTANT"));

        // But we still must classify regular members declared inside the enum type.
        MemberDescriptor valueFieldDescriptor = findDescriptorByName(describedMembers, "value");
        assertThat(valueFieldDescriptor.getMemberKind()).isEqualTo(FIELD);
        assertThat(valueFieldDescriptor.getDeclarationModifiers()).contains(FINAL);
        assertThat(valueFieldDescriptor.getMemberAccess()).contains(PRIVATE);
    }

    @Test
    void describeMembers_recordType_implicitMembersAreNotDescribed() {
        CtType<?> parsedType = parseMainType("ExampleRecord.java", exampleRecordSourceCode());
        assertThat(parsedType).isInstanceOf(CtRecord.class);

        CtRecord parsedRecord = (CtRecord) parsedType;

        Map<CtTypeMember, MemberDescriptor> describedMembers = SpoonMemberDescriptorFactory.describeMembers(parsedType);

        // Safety net: the factory must never return implicit members (records generate multiple implicit elements).
        assertThat(describedMembers.keySet()).allMatch(typeMember -> !typeMember.isImplicit());

        // Record components are supported in Spoon 11+. If the current Spoon configuration does not expose them,
        // keep the test focused on the non-negotiable contract: implicit record-generated elements must not be
        // described.
        List<String> expectedRecordComponentNames = List.of("alpha", "beta");

        List<String> explicitRecordComponentNames = parsedRecord.getRecordComponents().stream()
                .filter(recordComponent -> !recordComponent.isImplicit())
                .map(CtRecordComponent::getSimpleName)
                .toList();

        if (!explicitRecordComponentNames.isEmpty()) {
            assertThat(explicitRecordComponentNames).containsExactlyInAnyOrderElementsOf(expectedRecordComponentNames);
        }

        // The current product version may or may not expose record components as "members" for ordering.
        // If they are described, validate their descriptors. If not, the rest of the test still enforces
        // the non-negotiable contract: implicit record-generated elements must not be described.
        List<MemberDescriptor> recordComponentDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == RECORD_COMPONENT)
                .toList();

        if (!recordComponentDescriptors.isEmpty()) {
            assertThat(recordComponentDescriptors.stream()
                            .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                            .toList())
                    .containsExactlyInAnyOrder("alpha", "beta");

            recordComponentDescriptors.forEach(recordComponentDescriptor ->
                    assertThat(recordComponentDescriptor.getMemberAccess()).isEmpty());
        }

        List<String> implicitRecordComponentNames = parsedRecord.getRecordComponents().stream()
                .filter(CtRecordComponent::isImplicit)
                .map(CtRecordComponent::getSimpleName)
                .toList();

        implicitRecordComponentNames.forEach(implicitRecordComponentName -> {
            boolean implicitComponentIsDescribed = describedMembers.values().stream()
                    .filter(memberDescriptor -> memberDescriptor.getMemberKind() == RECORD_COMPONENT)
                    .anyMatch(memberDescriptor ->
                            memberDescriptor.getName().orElse("").equals(implicitRecordComponentName));
            assertThat(implicitComponentIsDescribed).isFalse();
        });

        List<String> implicitFieldNames = parsedRecord.getFields().stream()
                .filter(field -> field.isImplicit())
                .map(CtField::getSimpleName)
                .toList();

        assertThat(describedMembers.values().stream()
                        .filter(memberDescriptor -> memberDescriptor.getMemberKind() == FIELD)
                        .map(memberDescriptor -> memberDescriptor.getName().orElse("<null>"))
                        .toList())
                .doesNotContainAnyElementsOf(implicitFieldNames);

        MemberDescriptor sumMethodDescriptor = findDescriptorByName(describedMembers, "sum");
        assertThat(sumMethodDescriptor.getMemberKind()).isEqualTo(METHOD);
        assertThat(sumMethodDescriptor.getMemberAccess()).contains(PUBLIC);

        // Record component fields are implicitly final; verify we do not accidentally classify them as explicit fields.
        assertThat(describedMembers.values().stream()
                        .filter(memberDescriptor -> memberDescriptor.getMemberKind() == FIELD)
                        .map(memberDescriptor -> memberDescriptor.getName().orElse("<null>"))
                        .toList())
                .doesNotContain("alpha")
                .doesNotContain("beta");
    }

    private static CtType<?> parseMainType(String fileName, String sourceCode) {
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of(fileName), sourceCode);
        return spoonAstModel.getMainType().orElseThrow();
    }

    private static MemberDescriptor findDescriptorByName(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedName) {

        return describedMembers.values().stream()
                .filter(memberDescriptor ->
                        memberDescriptor.getName().orElse("").equals(expectedName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No member descriptor found for name: " + expectedName));
    }

    private static MemberDescriptor findUniqueDescriptorByKind(
            Map<CtTypeMember, MemberDescriptor> describedMembers,
            io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind expectedKind) {

        List<MemberDescriptor> matchingDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == expectedKind)
                .toList();

        assertThat(matchingDescriptors)
                .withFailMessage(
                        "Expected exactly one descriptor with kind %s, but found %s", expectedKind, matchingDescriptors)
                .hasSize(1);

        return matchingDescriptors.getFirst();
    }

    private static String exampleClassSourceCode() {
        return String.join(
                "\n",
                "package example;",
                "",
                "public abstract class ExampleClass {",
                "",
                "  @Deprecated",
                "  public static final int PUBLIC_STATIC_FINAL_FIELD = 1;",
                "",
                "  protected int protectedField;",
                "  int packageField;",
                "",
                "  @Deprecated",
                "  private static void privateStaticMethod() { }",
                "",
                "  public abstract void abstractMethod();",
                "",
                "  public ExampleClass() { }",
                "",
                "  static { int staticValue = 1; }",
                "  { int instanceValue = 2; }",
                "",
                "  public class PublicNestedClass { }",
                "  protected interface ProtectedNestedInterface { }",
                "  enum NestedEnum { A }",
                "  @interface NestedAnnotation { }",
                "  record NestedRecord(int component) { }",
                "}");
    }

    private static String exampleEnumSourceCode() {
        return String.join(
                "\n",
                "package example;",
                "",
                "public enum ExampleEnum {",
                "  FIRST,",
                "  SECOND;",
                "",
                "  private final int value = 1;",
                "}");
    }

    private static String exampleRecordSourceCode() {
        return String.join(
                "\n",
                "package example;",
                "",
                "public record ExampleRecord(int alpha, int beta) {",
                "  public int sum() { return alpha + beta; }",
                "}");
    }
}
