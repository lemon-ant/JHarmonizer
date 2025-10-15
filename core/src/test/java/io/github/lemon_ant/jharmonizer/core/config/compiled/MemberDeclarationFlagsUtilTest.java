package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberDeclarationFlagsUtil (public API only)")
class MemberDeclarationFlagsUtilTest {

    // ---- helpers (test-only), построены ТОЛЬКО на public API ----

    private static int mask(MemberKind kind, MemberAccess access, Set<DeclarationModifier> mods) {
        return MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(kind, access, mods);
    }

    private static int mask(MemberKind kind) {
        return MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                kind, null, EnumSet.noneOf(DeclarationModifier.class));
    }

    private static int accessBit(MemberAccess access) {
        // фиксируем kind и пустые модификаторы → разница масок = чистый access-бит
        int base = mask(MemberKind.FIELD); // kind-only
        int withAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD, access, EnumSet.noneOf(DeclarationModifier.class));
        return withAccess ^ base;
    }

    private static int allAccessBits() {
        int sum = 0;
        for (MemberAccess a : MemberAccess.values()) {
            sum |= accessBit(a);
        }
        return sum;
    }

    private static int modifiersBits(Set<DeclarationModifier> modifiers) {
        // фиксируем kind и null-access; разница = чистые modifier-биты
        int base = mask(MemberKind.FIELD);
        int withMods = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(MemberKind.FIELD, null, modifiers);
        return withMods ^ base;
    }

    // -------------------- tests --------------------

    @Test
    @DisplayName("MemberKind encodes to unique one-hot bits (no overlap)")
    void memberKind_oneHot_noOverlap() {
        // given
        int methodBit = mask(MemberKind.METHOD);
        int fieldBit = mask(MemberKind.FIELD);
        int typeBit = mask(MemberKind.TYPE_CLASS);

        // then (one-hot uniqueness)
        assertThat(methodBit).isNotZero();
        assertThat(fieldBit).isNotZero();
        assertThat(typeBit).isNotZero();

        assertThat(methodBit & fieldBit).isZero();
        assertThat(methodBit & typeBit).isZero();
        assertThat(fieldBit & typeBit).isZero();

        int combined = methodBit | fieldBit | typeBit;
        assertThat(combined & methodBit).isEqualTo(methodBit);
        assertThat(combined & fieldBit).isEqualTo(fieldBit);
        assertThat(combined & typeBit).isEqualTo(typeBit);
    }

    @Test
    @DisplayName("MemberAccess encodes to unique one-hot bits; null yields zero access segment")
    void memberAccess_oneHot_and_null_is_zero() {
        // given
        int publicBit = accessBit(MemberAccess.PUBLIC);
        int privateBit = accessBit(MemberAccess.PRIVATE);
        int packageBit = accessBit(MemberAccess.PACKAGE);
        int baseKindOnly = mask(MemberKind.CONSTRUCTOR); // access=null, no mods

        // then
        assertThat(publicBit).isNotZero();
        assertThat(privateBit).isNotZero();
        assertThat(packageBit).isNotZero();

        assertThat(publicBit & privateBit).isZero();
        assertThat(publicBit & packageBit).isZero();
        assertThat(privateBit & packageBit).isZero();

        // null access → access segment = 0
        int allAcc = allAccessBits();
        assertThat(baseKindOnly & allAcc).isZero();
    }

    @Test
    @DisplayName("DeclarationModifier encodes to subset bits; order independent")
    void modifiers_subset_and_order_independent() {
        // given
        Set<DeclarationModifier> a = EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL);
        Set<DeclarationModifier> b = EnumSet.of(DeclarationModifier.FINAL, DeclarationModifier.STATIC);
        Set<DeclarationModifier> superset =
                EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL, DeclarationModifier.STRICTFP);

        // when
        int aBits = modifiersBits(a);
        int bBits = modifiersBits(b);
        int supBits = modifiersBits(superset);

        // then
        assertThat(aBits).isEqualTo(bBits); // порядок не важен
        assertThat((supBits & aBits)).isEqualTo(aBits); // A — подмножество супермаски
    }

    @Test
    @DisplayName("buildFeatureMask equals OR of (kind ⊕ access ⊕ modifiers) parts")
    void encodeMemberDeclarationFlags_equals_or_of_parts() {
        // given
        MemberKind kind = MemberKind.METHOD;
        MemberAccess access = MemberAccess.PROTECTED;
        Set<DeclarationModifier> mods = EnumSet.of(DeclarationModifier.ABSTRACT, DeclarationModifier.STRICTFP);

        int kindOnly = mask(kind);
        int accOnly = accessBit(access);
        int modsOnly = modifiersBits(mods);

        // when
        int composite = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(kind, access, mods);

        // then
        assertThat(composite).isEqualTo(kindOnly | accOnly | modsOnly);
    }

    @Test
    @DisplayName("featureMaskContainsAllRequiredDeclarationModifiers: true when all required bits present")
    void containsAllRequiredMods_true() {
        // given
        int featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD,
                MemberAccess.PRIVATE,
                EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL));

        int required = modifiersBits(EnumSet.of(DeclarationModifier.STATIC));

        // when
        boolean ok = MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(featureMask, required);

        // then
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("featureMaskContainsAllRequiredDeclarationModifiers: false when any required bit is missing")
    void containsAllRequiredMods_false() {
        // given
        int featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD, MemberAccess.PRIVATE, EnumSet.of(DeclarationModifier.FINAL));

        int required = modifiersBits(EnumSet.of(DeclarationModifier.STATIC));

        // when
        boolean ok = MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(featureMask, required);

        // then
        assertThat(ok).isFalse();
    }

    @Test
    @DisplayName("Segments do not overlap: access vs kind vs modifiers")
    void segments_do_not_overlap() {
        // given
        int anyAccess = accessBit(MemberAccess.PUBLIC);
        int anyKind = mask(MemberKind.TYPE_INTERFACE);
        int anyMods = modifiersBits(EnumSet.of(DeclarationModifier.STATIC));

        // then
        assertThat(anyAccess & anyKind).as("access vs kind must not overlap").isZero();
        assertThat(anyAccess & anyMods)
                .as("access vs modifiers must not overlap")
                .isZero();
        assertThat(anyKind & anyMods).as("kind vs modifiers must not overlap").isZero();
    }

    @Test
    @DisplayName("Null access affects only access segment; clearing access makes masks equal")
    void null_access_affects_only_access_segment() {
        // given
        int withNullAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.CONSTRUCTOR, null, EnumSet.noneOf(DeclarationModifier.class));

        int withPackageAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.CONSTRUCTOR, MemberAccess.PACKAGE, EnumSet.noneOf(DeclarationModifier.class));

        int allAccess = allAccessBits();

        // then
        // 1) Null access не выставляет ни одного access-бита
        assertThat(withNullAccess & allAccess).isZero();

        // 2) PACKAGE выставляет ровно свой бит
        int packageBit = accessBit(MemberAccess.PACKAGE);
        assertThat(withPackageAccess & packageBit).isEqualTo(packageBit);

        // 3) Различие между масками — только в access-сегменте
        int onlyAccessDiff = (withNullAccess ^ withPackageAccess) & allAccess;
        assertThat(onlyAccessDiff).isEqualTo(packageBit);

        // 4) Если «обнулить» access-сегмент — маски идентичны (kind одинаковый, модификаторов нет)
        int nullCleared = withNullAccess & ~allAccess;
        int packageCleared = withPackageAccess & ~allAccess;
        assertThat(nullCleared).isEqualTo(packageCleared);

        // 5) Маска с null-access не нулевая — потому что kind-сегмент установлен
        assertThat(withNullAccess).isNotZero();
    }
}
