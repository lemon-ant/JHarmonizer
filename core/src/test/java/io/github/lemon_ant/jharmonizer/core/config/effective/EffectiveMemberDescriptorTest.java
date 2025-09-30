package io.github.lemon_ant.jharmonizer.core.config.effective;

import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.*;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess.*;
import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind.*;
import static org.assertj.core.api.Assertions.*;

import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.TargetCategory;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class EffectiveMemberDescriptorTest {

    // ---------- small builders ----------

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder base(MemberKind kind, MemberAccess access, String name) {
        return EffectiveMemberDescriptor.builder().memberKind(kind).memberAccess(access).name(name);
    }

    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder fld(String name)    { return base(FIELD, PUBLIC, name); }
    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder mtd(String name)    { return base(METHOD, PUBLIC, name); }
    private EffectiveMemberDescriptor.EffectiveMemberDescriptorBuilder typ(MemberKind k, String name) {
        assertThat(k.isType()).isTrue();
        return base(k, PUBLIC, name);
    }

    // ---------- 1) Name invariants ----------

    static Stream<Arguments> nameMustBeNullKinds() {
        return Stream.of(
            Arguments.of(CONSTRUCTOR),
            Arguments.of(INIT_BLOCK_STATIC),
            Arguments.of(INIT_BLOCK_INSTANCE)
        );
    }

    @ParameterizedTest(name = "{0} with non-null name → IAE")
    @MethodSource("nameMustBeNullKinds")
    void build_kindsRequiringNullName_andProvidedName_throwsIAE(MemberKind kind) {
        assertThatThrownBy(() -> base(kind, kind.getTargetCategory().isAccessLevelApplicable() ? PUBLIC : null, "X").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null name");
    }

    static Stream<Arguments> nameMustBeNonBlankKinds() {
        return Stream.of(
            Arguments.of(FIELD),
            Arguments.of(METHOD),
            Arguments.of(ENUM_CONSTANT),
            Arguments.of(RECORD_COMPONENT),
            Arguments.of(TYPE_CLASS),
            Arguments.of(TYPE_INTERFACE),
            Arguments.of(TYPE_ENUM),
            Arguments.of(TYPE_RECORD),
            Arguments.of(TYPE_ANNOTATION)
        );
    }

    @ParameterizedTest(name = "{0} with blank name → IAE")
    @MethodSource("nameMustBeNonBlankKinds")
    void build_kindsRequiringName_andBlankName_throwsIAE(MemberKind kind) {
        MemberAccess access = kind.getTargetCategory().isAccessLevelApplicable() ? PUBLIC : null;
        assertThatThrownBy(() -> base(kind, access, "  ").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Non-initializer elements must have a non-blank name");
    }

    // ---------- 2) Access applicability ----------

    static Stream<MemberKind> accessApplicableKinds() {
        return Stream.of(FIELD, METHOD, CONSTRUCTOR, TYPE_CLASS, TYPE_INTERFACE, TYPE_ENUM, TYPE_RECORD, TYPE_ANNOTATION);
    }

    static Stream<MemberKind> accessNotApplicableKinds() {
        return Stream.of(INIT_BLOCK_STATIC, INIT_BLOCK_INSTANCE, ENUM_CONSTANT, RECORD_COMPONENT);
    }

    @ParameterizedTest(name = "{0} requires access → null access IAE")
    @MethodSource("accessApplicableKinds")
    void build_accessApplicable_andNullAccess_throwsIAE(MemberKind kind) {
        String name = kind.getTargetCategory() == TargetCategory.CONSTRUCTOR ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder().memberKind(kind).name(name).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access level must be provided for " + kind);
    }

    @ParameterizedTest(name = "{0} forbids access → provided access IAE")
    @MethodSource("accessNotApplicableKinds")
    void build_accessNotApplicable_andProvidedAccess_throwsIAE(MemberKind kind) {
        String name = kind.getTargetCategory() == TargetCategory.INIT_BLOCK ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
            .memberKind(kind).name(name).memberAccess(PUBLIC).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access level must be null for " + kind);
    }

    // ---------- 3) No-modifier categories (applicability-based rejection) ----------

    static Stream<Arguments> noModifierCategories() {
        return Stream.of(
            Arguments.of(INIT_BLOCK_STATIC, null, STATIC),
            Arguments.of(INIT_BLOCK_INSTANCE, null, FINAL),
            Arguments.of(ENUM_CONSTANT, null, STATIC),
            Arguments.of(RECORD_COMPONENT, null, FINAL)
        );
    }

    @ParameterizedTest(name = "{0} with modifier {2} → IAE (illegal modifier)")
    @MethodSource("noModifierCategories")
    void build_noModifierCategories_anyModifier_throwsIAE(MemberKind kind, MemberAccess access, DeclarationModifier mod) {
        String name = kind.getTargetCategory() == TargetCategory.INIT_BLOCK ? null : "Valid";
        assertThatThrownBy(() -> EffectiveMemberDescriptor.builder()
            .memberKind(kind).name(name).memberAccess(access).declarationModifier(mod).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Illegal modifier for " + kind);
    }

    // ---------- 4) Allowed modifiers (positive smoke) ----------

    @Test
    void build_field_withTransientAndVolatile_isOk() {
        assertThat(fld("x").declarationModifier(TRANSIENT).build().getDeclarationModifiers())
            .containsExactly(TRANSIENT);
        assertThat(fld("y").declarationModifier(VOLATILE).build().getDeclarationModifiers())
            .containsExactly(VOLATILE);
    }

    @Test
    void build_method_withDefaultOrStrictfp_isOk() {
        assertThat(mtd("m").declarationModifier(DEFAULT).build().getDeclarationModifiers())
            .containsExactly(DEFAULT);
        assertThat(mtd("n").declarationModifier(STRICTFP).build().getDeclarationModifiers())
            .containsExactly(STRICTFP);
    }

    @Test
    void build_type_withStrictfpOrSealed_isOk() {
        assertThat(typ(TYPE_CLASS, "T").declarationModifier(STRICTFP).build().getDeclarationModifiers())
            .containsExactly(STRICTFP);
        assertThat(typ(TYPE_CLASS, "T2").declarationModifier(SEALED).build().getDeclarationModifiers())
            .containsExactly(SEALED);
    }

    // ---------- 5) Method conflicts ----------

    static Stream<DeclarationModifier> abstractConflicts() {
        return Stream.of(FINAL, STATIC, NATIVE, SYNCHRONIZED);
    }

    @ParameterizedTest(name = "abstract + {0} on METHOD → IAE")
    @MethodSource("abstractConflicts")
    void build_method_abstractWithConflictingModifier_throwsIAE(DeclarationModifier conflicting) {
        assertThatThrownBy(() -> mtd("process").declarationModifier(ABSTRACT).declarationModifier(conflicting).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_method_abstractPlusPrivate_throwsIAE() {
        assertThatThrownBy(() -> base(METHOD, PRIVATE, "process").declarationModifier(ABSTRACT).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("abstract + private");
    }

    static Stream<DeclarationModifier> defaultConflicts() {
        return Stream.of(ABSTRACT, STATIC, FINAL, SYNCHRONIZED, NATIVE);
    }

    @ParameterizedTest(name = "default + {0} on METHOD → IAE")
    @MethodSource("defaultConflicts")
    void build_method_defaultWithConflictingModifier_throwsIAE(DeclarationModifier conflicting) {
        assertThatThrownBy(() -> mtd("process").declarationModifier(DEFAULT).declarationModifier(conflicting).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- 6) Type conflicts ----------

    @Test
    void build_type_abstractPlusFinal_throwsIAE() {
        assertThatThrownBy(() -> typ(TYPE_CLASS, "T").declarationModifier(ABSTRACT).declarationModifier(FINAL).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Illegal modifier combination");
    }

    @Test
    void build_type_sealedPlusNonSealed_throwsIAE() {
        assertThatThrownBy(() -> typ(TYPE_CLASS, "T").declarationModifier(SEALED).declarationModifier(NON_SEALED).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Illegal modifier combination");
    }

    // ---------- 7) Equality & basic flags ----------

    @Test
    void equalsHashCode_andFlags_areConsistent() {
        var a = fld("VALUE").declarationModifier(STATIC).declarationModifier(FINAL)
            .annotationQualifiedName("javax.annotation.Nullable").build();
        var b = fld("VALUE").declarationModifier(FINAL).declarationModifier(STATIC)
            .annotationQualifiedName("javax.annotation.Nullable").build();
        var c = fld("OTHER").declarationModifier(STATIC).declarationModifier(FINAL)
            .annotationQualifiedName("javax.annotation.Nullable").build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);

        assertThat(typ(TYPE_INTERFACE, "Api").build().isType()).isTrue();
        assertThat(base(INIT_BLOCK_STATIC, null, null).build().isInitializer()).isTrue();
        assertThat(mtd("x").build().isType()).isFalse();
        assertThat(mtd("x").build().isInitializer()).isFalse();
    }

    // ---------- 8) Constructor baseline ----------

    @Test
    void build_constructor_withNullNameAccessProvided_noModifiers_isOk() {
        var d = base(CONSTRUCTOR, PUBLIC, null).build();
        assertThat(d.getMemberAccess()).contains(PUBLIC);
        assertThat(d.getDeclarationModifiers()).isEmpty();
    }
}
