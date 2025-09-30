package io.github.lemon_ant.jharmonizer.core.config.effective;

import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.ABSTRACT;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.DEFAULT;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.FINAL;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.NATIVE;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.NON_SEALED;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.SEALED;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.STATIC;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.STRICTFP;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.SYNCHRONIZED;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.TRANSIENT;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.VOLATILE;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess.PRIVATE;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess.PUBLIC;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.CONSTRUCTOR;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.ENUM_CONSTANT;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.FIELD;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_INSTANCE;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_STATIC;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.METHOD;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.RECORD_COMPONENT;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_CLASS;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_INTERFACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EffectiveMemberDescriptorTest {

    private EffectiveMemberDescriptorBuilder createBaseDescriptorBuilder(
            MemberKind kind, MemberAccess access, String name) {
        return EffectiveMemberDescriptor.builder()
                .memberKind(kind)
                .memberAccess(access)
                .name(name);
    }

    private EffectiveMemberDescriptorBuilder createFieldDescriptorBuilder(String name) {
        return createBaseDescriptorBuilder(FIELD, PUBLIC, name);
    }

    private EffectiveMemberDescriptorBuilder createMethodDescriptorBuilder(String name) {
        return createBaseDescriptorBuilder(METHOD, PUBLIC, name);
    }

    private EffectiveMemberDescriptorBuilder createTypeDescriptorBuilder(MemberKind typeKind, String name) {
        assertThat(typeKind.isType()).isTrue();
        return createBaseDescriptorBuilder(typeKind, PUBLIC, name);
    }

    // ---------- name invariants ----------

    @Test
    @DisplayName("Initializer must have null name")
    void build_initializerWithName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(INIT_BLOCK_STATIC, PUBLIC, "<clinit>")
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initializer elements must have null name");
    }

    @Test
    @DisplayName("Non-initializer must have non-blank name")
    void build_nonInitializerWithBlankName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createMethodDescriptorBuilder("   ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Non-initializer elements must have a non-blank name");
    }

    // ---------- kinds with no modifiers allowed ----------

    @Test
    @DisplayName("Constructor must not declare modifiers")
    void build_constructorWithAnyModifiers_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(CONSTRUCTOR, PUBLIC, "X")
                        .declarationModifier(FINAL)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONSTRUCTOR must not declare modifiers");
    }

    @Test
    @DisplayName("Enum constant must not declare modifiers")
    void build_enumConstantWithAnyModifiers_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(ENUM_CONSTANT, PUBLIC, "FOO")
                        .declarationModifier(STATIC)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("ENUM_CONSTANT", "Access", "null");
    }

    @Test
    @DisplayName("Record component must not declare modifiers")
    void build_recordComponentWithAnyModifiers_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(RECORD_COMPONENT, PUBLIC, "x")
                        .declarationModifier(FINAL)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("RECORD_COMPONENT", "Access", "null");
    }

    @Test
    @DisplayName("Initializer block must not declare modifiers")
    void build_initializerWithAnyModifiers_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(INIT_BLOCK_INSTANCE, PUBLIC, null)
                        .declarationModifier(STATIC)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("INIT_BLOCK_INSTANCE", "Access", "null");
    }

    // ---------- field rules ----------

    @Test
    @DisplayName("Field: method/type-only modifiers are forbidden")
    void build_fieldWithIllegalModifier_throwsIllegalArgumentException() {
        for (DeclarationModifier illegalModifier :
                Set.of(ABSTRACT, SYNCHRONIZED, NATIVE, DEFAULT, SEALED, NON_SEALED, STRICTFP)) {
            assertThatThrownBy(() -> createFieldDescriptorBuilder("value")
                            .declarationModifier(illegalModifier)
                            .build())
                    .as("modifier %s should be illegal for FIELD", illegalModifier)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Illegal modifier for FIELD");
        }
    }

    @Test
    @DisplayName("Field: static final is allowed")
    void build_fieldWithStaticFinal_returnsDescriptor() {
        EffectiveMemberDescriptor descriptor = createFieldDescriptorBuilder("VALUE")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .build();

        assertThat(descriptor.getDeclarationModifiers()).containsExactlyInAnyOrder(STATIC, FINAL);
        assertThat(descriptor.isType()).isFalse();
        assertThat(descriptor.isInitializer()).isFalse();
    }

    // ---------- method rules ----------

    @Test
    @DisplayName("Method: field/type-only modifiers are forbidden")
    void build_methodWithIllegalModifier_throwsIllegalArgumentException() {
        for (DeclarationModifier illegalModifier : Set.of(TRANSIENT, VOLATILE, SEALED, NON_SEALED)) {
            assertThatThrownBy(() -> createMethodDescriptorBuilder("process")
                            .declarationModifier(illegalModifier)
                            .build())
                    .as("modifier %s should be illegal for METHOD", illegalModifier)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Illegal modifier for METHOD");
        }
    }

    @Test
    @DisplayName("Method: abstract conflicts are rejected (final/static/native/synchronized)")
    void build_methodWithAbstractAndConflictingModifier_throwsIllegalArgumentException() {
        for (DeclarationModifier conflictingModifier : Set.of(FINAL, STATIC, NATIVE, SYNCHRONIZED)) {
            assertThatThrownBy(() -> createMethodDescriptorBuilder("process")
                            .declarationModifier(ABSTRACT)
                            .declarationModifier(conflictingModifier)
                            .build())
                    .as("abstract + %s should be illegal for METHOD", conflictingModifier)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Method: abstract + private is rejected")
    void build_methodWithAbstractAndPrivateAccess_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createBaseDescriptorBuilder(METHOD, PRIVATE, "process")
                        .declarationModifier(ABSTRACT)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abstract + private");
    }

    @Test
    @DisplayName("Method: default conflicts are rejected (abstract/static/final/synchronized/native)")
    void build_methodWithDefaultAndConflictingModifier_throwsIllegalArgumentException() {
        for (DeclarationModifier conflictingModifier : Set.of(ABSTRACT, STATIC, FINAL, SYNCHRONIZED, NATIVE)) {
            assertThatThrownBy(() -> createMethodDescriptorBuilder("process")
                            .declarationModifier(DEFAULT)
                            .declarationModifier(conflictingModifier)
                            .build())
                    .as("default + %s should be illegal for METHOD", conflictingModifier)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Method: plain public method without modifiers is allowed")
    void build_methodWithoutModifiers_returnsDescriptor() {
        EffectiveMemberDescriptor descriptor =
                createMethodDescriptorBuilder("process").build();
        assertThat(descriptor.getDeclarationModifiers()).isEmpty();
    }

    // ---------- type rules ----------

    @Test
    @DisplayName("Type: method/field-only modifiers are forbidden")
    void build_typeWithIllegalModifier_throwsIllegalArgumentException() {
        for (DeclarationModifier illegalModifier : Set.of(TRANSIENT, VOLATILE, SYNCHRONIZED, NATIVE, DEFAULT)) {
            assertThatThrownBy(() -> createTypeDescriptorBuilder(TYPE_CLASS, "Sample")
                            .declarationModifier(illegalModifier)
                            .build())
                    .as("modifier %s should be illegal for TYPE_*", illegalModifier)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Illegal modifier for TYPE_");
        }
    }

    @Test
    @DisplayName("Type: abstract + final is rejected")
    void build_typeWithAbstractAndFinal_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createTypeDescriptorBuilder(TYPE_CLASS, "Sample")
                        .declarationModifier(ABSTRACT)
                        .declarationModifier(FINAL)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal modifier combination");
    }

    @Test
    @DisplayName("Type: sealed + non-sealed is rejected")
    void build_typeWithSealedAndNonSealed_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> createTypeDescriptorBuilder(TYPE_CLASS, "Sample")
                        .declarationModifier(SEALED)
                        .declarationModifier(NON_SEALED)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal modifier combination");
    }

    @Test
    @DisplayName("Type: static final nested class is allowed")
    void build_typeWithStaticFinal_returnsDescriptor() {
        EffectiveMemberDescriptor descriptor = createTypeDescriptorBuilder(TYPE_CLASS, "Nested")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .build();

        assertThat(descriptor.isType()).isTrue();
        assertThat(descriptor.getDeclarationModifiers()).containsExactlyInAnyOrder(STATIC, FINAL);
    }

    // ---------- flags & equality ----------

    @Test
    @DisplayName("Flags isType()/isInitializer() work as expected")
    void isTypeAndIsInitializer_givenTypeMethodAndInitKinds_returnExpectedFlags() {
        assertThat(createTypeDescriptorBuilder(TYPE_INTERFACE, "Api").build().isType())
                .isTrue();
        assertThat(createBaseDescriptorBuilder(INIT_BLOCK_STATIC, null, null)
                        .build()
                        .isInitializer())
                .isTrue();
        assertThat(createMethodDescriptorBuilder("process").build().isType()).isFalse();
        assertThat(createMethodDescriptorBuilder("process").build().isInitializer())
                .isFalse();
    }

    @Test
    @DisplayName("equals/hashCode consider all relevant fields")
    void equalsAndHashCode_withDifferentFields_returnExpectedEquality() {
        EffectiveMemberDescriptor fieldConstantOne = createFieldDescriptorBuilder("VALUE")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();

        EffectiveMemberDescriptor fieldConstantTwo = createFieldDescriptorBuilder("VALUE")
                .declarationModifier(FINAL)
                .declarationModifier(STATIC) // different order, still equal
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();

        EffectiveMemberDescriptor fieldDifferentName = createFieldDescriptorBuilder("OTHER")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();

        assertThat(fieldConstantOne).isEqualTo(fieldConstantTwo).hasSameHashCodeAs(fieldConstantTwo);
        assertThat(fieldConstantOne).isNotEqualTo(fieldDifferentName);
    }

    @Test
    @DisplayName("build_kindWithApplicableAccessAndOmittedAccess_throwsIllegalArgumentException")
    void build_kindWithApplicableAccess_andNullAccess_throwsIllegalArgumentException() {
        // FIELD requires an explicit access level → null access must fail
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(EffectiveMemberDescriptor.MemberKind.FIELD)
                        .name("ValidName")
                        // .memberAccess(...) intentionally omitted
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level must be provided for FIELD");
    }

    @Test
    @DisplayName("build_kindWithoutApplicableAccess_andProvidedAccess_throwsIllegalArgumentException")
    void build_kindWithoutApplicableAccessAndProvidedAccess_throwsIllegalArgumentException() {
        // INIT_BLOCK_STATIC does not allow access level → any provided access must fail
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_STATIC)
                        .name(null) // initializer must have null name
                        .memberAccess(EffectiveMemberDescriptor.MemberAccess.PUBLIC)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level must be null for INIT_BLOCK_STATIC");
    }

    @Test
    @DisplayName("build_kindWithoutApplicableAccess_andNullAccess_returnsDescriptor")
    void build_kindWithoutApplicableAccessAndNullAccess_returnsDescriptor() {
        // INIT_BLOCK_INSTANCE with null access is valid
        EffectiveMemberDescriptor descriptor = EffectiveMemberDescriptor.builder()
                .memberKind(EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_INSTANCE)
                .name(null)
                // no memberAccess on purpose
                .build();

        assertThat(descriptor.getMemberAccess()).isEmpty();
        assertThat(descriptor.isInitializer()).isTrue();
    }

    @Test
    @DisplayName("build_kindWithApplicableAccessAndProvidedAccess_returnsDescriptor")
    void build_kindWithApplicableAccess_andProvidedAccess_returnsDescriptor() {
        EffectiveMemberDescriptor descriptor = EffectiveMemberDescriptor.builder()
                .memberKind(EffectiveMemberDescriptor.MemberKind.FIELD)
                .name("ValidName")
                .memberAccess(EffectiveMemberDescriptor.MemberAccess.PUBLIC)
                .build();

        assertThat(descriptor.getMemberAccess()).contains(EffectiveMemberDescriptor.MemberAccess.PUBLIC);
        assertThat(descriptor.isType()).isFalse();
    }

    @Test
    @DisplayName("build_methodWithNullAccess_throwsIllegalArgumentException")
    void build_methodWithNullAccess_throwsIllegalArgumentException() {
        // METHOD requires access (any of PUBLIC/PROTECTED/PACKAGE/PRIVATE), so null must fail
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(EffectiveMemberDescriptor.MemberKind.METHOD)
                        .name("ValidName")
                        // access omitted
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level must be provided for METHOD");
    }

    @Test
    @DisplayName("build_allKindsWithoutApplicableAccessAndProvidedAccess_throwsIllegalArgumentException")
    void build_allKindsWithoutApplicableAccess_andProvidedAccess_throwsIllegalArgumentException() {
        java.util.Set<EffectiveMemberDescriptor.MemberKind> kindsWithoutAccess = java.util.EnumSet.of(
                EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_STATIC,
                EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_INSTANCE,
                EffectiveMemberDescriptor.MemberKind.ENUM_CONSTANT,
                EffectiveMemberDescriptor.MemberKind.RECORD_COMPONENT);

        for (EffectiveMemberDescriptor.MemberKind memberKind : kindsWithoutAccess) {
            // Prepare a name according to invariants
            String name = (memberKind == EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_STATIC
                            || memberKind == EffectiveMemberDescriptor.MemberKind.INIT_BLOCK_INSTANCE)
                    ? null
                    : "ValidName";

            // effectively final for lambda
            assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                            .memberKind(memberKind)
                            .name(name)
                            .memberAccess(EffectiveMemberDescriptor.MemberAccess.PUBLIC)
                            .build())
                    .as("kind %s must reject any provided access level", memberKind)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Access level must be null for " + memberKind);
        }
    }

    @Test
    @DisplayName("build_allKindsWithApplicableAccessAndNullAccess_throwsIllegalArgumentException")
    void build_allKindsWithApplicableAccess_andNullAccess_throwsIllegalArgumentException() {
        java.util.Set<EffectiveMemberDescriptor.MemberKind> memberKindsWithAccess = java.util.EnumSet.of(
                EffectiveMemberDescriptor.MemberKind.FIELD,
                EffectiveMemberDescriptor.MemberKind.METHOD,
                EffectiveMemberDescriptor.MemberKind.CONSTRUCTOR,
                EffectiveMemberDescriptor.MemberKind.TYPE_CLASS,
                EffectiveMemberDescriptor.MemberKind.TYPE_INTERFACE,
                EffectiveMemberDescriptor.MemberKind.TYPE_ENUM,
                EffectiveMemberDescriptor.MemberKind.TYPE_RECORD,
                EffectiveMemberDescriptor.MemberKind.TYPE_ANNOTATION);

        for (EffectiveMemberDescriptor.MemberKind kind : memberKindsWithAccess) {
            // Constructors require a non-blank name; types/fields/methods too (per our invariants)
            String name = "Name";
            assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                            .memberKind(kind)
                            .name(name)
                            // no memberAccess
                            .build())
                    .as("kind %s must require a provided access level", kind)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Access level must be provided for " + kind);
        }
    }
}
