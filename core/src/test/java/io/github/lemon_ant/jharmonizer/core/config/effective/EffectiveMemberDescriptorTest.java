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
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_ANNOTATION;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_CLASS;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_ENUM;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_INTERFACE;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.TYPE_RECORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.TargetCategory;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EffectiveMemberDescriptorTest {

    private static Stream<Arguments> kindsRequiringNullName() {
        return Stream.of(Arguments.of(CONSTRUCTOR), Arguments.of(INIT_BLOCK_STATIC), Arguments.of(INIT_BLOCK_INSTANCE));
    }

    private static Stream<Arguments> kindsRequiringNonBlankName() {
        return Stream.of(
                Arguments.of(FIELD),
                Arguments.of(METHOD),
                Arguments.of(ENUM_CONSTANT),
                Arguments.of(RECORD_COMPONENT),
                Arguments.of(TYPE_CLASS),
                Arguments.of(TYPE_INTERFACE),
                Arguments.of(TYPE_ENUM),
                Arguments.of(TYPE_RECORD),
                Arguments.of(TYPE_ANNOTATION));
    }

    private static Stream<MemberKind> accessApplicableKinds() {
        return Stream.of(
                FIELD, METHOD, CONSTRUCTOR, TYPE_CLASS, TYPE_INTERFACE, TYPE_ENUM, TYPE_RECORD, TYPE_ANNOTATION);
    }

    private static Stream<MemberKind> accessNotApplicableKinds() {
        return Stream.of(INIT_BLOCK_STATIC, INIT_BLOCK_INSTANCE, ENUM_CONSTANT, RECORD_COMPONENT);
    }

    private static Stream<Arguments> noModifierCases() {
        return Stream.of(
                Arguments.of(INIT_BLOCK_STATIC, null, STATIC),
                Arguments.of(INIT_BLOCK_INSTANCE, null, FINAL),
                Arguments.of(ENUM_CONSTANT, null, STATIC),
                Arguments.of(RECORD_COMPONENT, null, FINAL));
    }

    private static Stream<DeclarationModifier> abstractConflicts() {
        return Stream.of(FINAL, STATIC, NATIVE, SYNCHRONIZED);
    }

    private static Stream<DeclarationModifier> defaultConflicts() {
        return Stream.of(ABSTRACT, STATIC, FINAL, SYNCHRONIZED, NATIVE);
    }

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder base(MemberKind k, MemberAccess a, String n) {
        return EffectiveMemberDescriptor.builder().memberKind(k).memberAccess(a).name(n);
    }

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder fld(String n) {
        return base(FIELD, PUBLIC, n);
    }

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder mtd(String n) {
        return base(METHOD, PUBLIC, n);
    }

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder typ(MemberKind k, String n) {
        assertThat(k.isType()).isTrue();
        return base(k, PUBLIC, n);
    }

    @ParameterizedTest(name = "{0} with name → IAE")
    @MethodSource("kindsRequiringNullName")
    void builder_nullNameRequiredAndProvided_throwsIAE(MemberKind kind) {
        assertThatThrownBy(() -> base(kind, kind.getTargetCategory().isAccessLevelApplicable() ? PUBLIC : null, "X")
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null name");
    }

    @ParameterizedTest(name = "{0} with blank name → IAE")
    @MethodSource("kindsRequiringNonBlankName")
    void builder_nameRequiredAndBlank_throwsIAE(MemberKind kind) {
        MemberAccess access = kind.getTargetCategory().isAccessLevelApplicable() ? PUBLIC : null;
        assertThatThrownBy(() -> base(kind, access, "  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Non-initializer elements must have a non-blank name");
    }

    @ParameterizedTest(name = "{0} null access → IAE")
    @MethodSource("accessApplicableKinds")
    void builder_accessRequiredAndNull_throwsIAE(MemberKind kind) {
        String name = kind.getTargetCategory() == TargetCategory.CONSTRUCTOR ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(kind)
                        .name(name)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level must be provided for " + kind);
    }

    @ParameterizedTest(name = "{0} access provided → IAE")
    @MethodSource("accessNotApplicableKinds")
    void builder_accessForbiddenAndProvided_throwsIAE(MemberKind kind) {
        String name = kind.getTargetCategory() == TargetCategory.INIT_BLOCK ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(kind)
                        .name(name)
                        .memberAccess(PUBLIC)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level must be null for " + kind);
    }

    @ParameterizedTest(name = "{0} + {2} → IAE")
    @MethodSource("noModifierCases")
    void builder_modifierInapplicableAndProvided_throwsIAE(
            MemberKind kind, MemberAccess access, DeclarationModifier mod) {
        String name = kind.getTargetCategory() == TargetCategory.INIT_BLOCK ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
                        .memberKind(kind)
                        .name(name)
                        .memberAccess(access)
                        .declarationModifier(mod)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal modifier for " + kind);
    }

    @Test
    void builder_fieldWithTransient_returnsDescriptor() {
        assertThat(fld("x").declarationModifier(TRANSIENT).build().getDeclarationModifiers())
                .containsExactly(TRANSIENT);
    }

    @Test
    void builder_fieldWithVolatile_returnsDescriptor() {
        assertThat(fld("y").declarationModifier(VOLATILE).build().getDeclarationModifiers())
                .containsExactly(VOLATILE);
    }

    @Test
    void builder_methodWithDefault_returnsDescriptor() {
        assertThat(mtd("m").declarationModifier(DEFAULT).build().getDeclarationModifiers())
                .containsExactly(DEFAULT);
    }

    @Test
    void builder_methodWithStrictfp_returnsDescriptor() {
        assertThat(mtd("n").declarationModifier(STRICTFP).build().getDeclarationModifiers())
                .containsExactly(STRICTFP);
    }

    @Test
    void builder_typeWithStrictfp_returnsDescriptor() {
        assertThat(typ(TYPE_CLASS, "T").declarationModifier(STRICTFP).build().getDeclarationModifiers())
                .containsExactly(STRICTFP);
    }

    @Test
    void builder_typeWithSealed_returnsDescriptor() {
        assertThat(typ(TYPE_CLASS, "T2").declarationModifier(SEALED).build().getDeclarationModifiers())
                .containsExactly(SEALED);
    }

    @ParameterizedTest(name = "abstract + {0} → IAE")
    @MethodSource("abstractConflicts")
    void builder_methodAbstractWithConflict_throwsIAE(DeclarationModifier conflicting) {
        assertThatThrownBy(() -> mtd("process")
                        .declarationModifier(ABSTRACT)
                        .declarationModifier(conflicting)
                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builder_methodAbstractWithPrivate_throwsIAE() {
        assertThatThrownBy(() ->
                        base(METHOD, PRIVATE, "p").declarationModifier(ABSTRACT).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abstract + private");
    }

    @ParameterizedTest(name = "default + {0} → IAE")
    @MethodSource("defaultConflicts")
    void builder_methodDefaultWithConflict_throwsIAE(DeclarationModifier conflicting) {
        assertThatThrownBy(() -> mtd("q").declarationModifier(DEFAULT)
                        .declarationModifier(conflicting)
                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builder_typeAbstractPlusFinal_throwsIAE() {
        assertThatThrownBy(() -> typ(TYPE_CLASS, "T")
                        .declarationModifier(ABSTRACT)
                        .declarationModifier(FINAL)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal modifier combination");
    }

    @Test
    void builder_typeSealedPlusNonSealed_throwsIAE() {
        assertThatThrownBy(() -> typ(TYPE_CLASS, "T")
                        .declarationModifier(SEALED)
                        .declarationModifier(NON_SEALED)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal modifier combination");
    }

    @Test
    void equalsHashCode_sameContent_equalAndSameHash() {
        var a = fld("VALUE")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();
        var b = fld("VALUE")
                .declarationModifier(FINAL)
                .declarationModifier(STATIC)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equalsHashCode_differentName_notEqual() {
        var a = fld("VALUE")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();
        var c = fld("OTHER")
                .declarationModifier(STATIC)
                .declarationModifier(FINAL)
                .annotationQualifiedName("javax.annotation.Nullable")
                .build();
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void flags_typeAndInitializer_asExpected() {
        assertThat(typ(TYPE_INTERFACE, "Api").build().isType()).isTrue();
        assertThat(base(INIT_BLOCK_STATIC, null, null).build().isInitializer()).isTrue();
        assertThat(mtd("x").build().isType()).isFalse();
        assertThat(mtd("x").build().isInitializer()).isFalse();
    }

    @Test
    void builder_constructorNullNameWithAccess_returnsDescriptor() {
        var d = base(CONSTRUCTOR, PUBLIC, null).build();
        assertThat(d.getMemberAccess()).contains(PUBLIC);
        assertThat(d.getDeclarationModifiers()).isEmpty();
    }
}
