// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberAccess;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDeclarationFlagsUtil;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MemberDeclarationFlagsUtilTest {

    @Test
    void containsAllRequiredDeclarationFlags_allRequiredBitsPresent_returnTrue() {
        // Given
        int featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD,
                MemberAccess.PRIVATE,
                EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL));
        int requiredDeclarationFlagsMask = deriveModifiersSegmentBitsMask(EnumSet.of(DeclarationModifier.STATIC));

        // When
        boolean containsAllRequiredDeclarationFlags = MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(
                featureMask, requiredDeclarationFlagsMask);

        // Then
        assertThat(containsAllRequiredDeclarationFlags).isTrue();
    }

    @Test
    void containsAllRequiredDeclarationFlags_anyRequiredBitMissing_returnFalse() {
        // Given
        int featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD, MemberAccess.PRIVATE, EnumSet.of(DeclarationModifier.FINAL));
        int requiredDeclarationFlagsMask = deriveModifiersSegmentBitsMask(EnumSet.of(DeclarationModifier.STATIC));

        // When
        boolean containsAllRequiredDeclarationFlags = MemberDeclarationFlagsUtil.containsAllRequiredDeclarationFlags(
                featureMask, requiredDeclarationFlagsMask);

        // Then
        assertThat(containsAllRequiredDeclarationFlags).isFalse();
    }

    @Test
    void encodeMemberDeclarationFlags_accessIsNull_affectOnlyAccessSegment() {
        // Given
        int withNullAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.CONSTRUCTOR, null, EnumSet.noneOf(DeclarationModifier.class));
        int withPackageAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.CONSTRUCTOR, MemberAccess.PACKAGE, EnumSet.noneOf(DeclarationModifier.class));
        int allAccessBitsMask = deriveAllMemberAccessBitsMask();
        int packageAccessBit = deriveAccessSegmentBit(MemberAccess.PACKAGE);

        // When
        int nullAccessSegment = withNullAccess & allAccessBitsMask;
        int accessOnlyDifferenceMask = (withNullAccess ^ withPackageAccess) & allAccessBitsMask;
        int nullAccessClearedMask = withNullAccess & ~allAccessBitsMask;
        int packageAccessClearedMask = withPackageAccess & ~allAccessBitsMask;

        // Then
        assertThat(nullAccessSegment).isZero();
        assertThat(withPackageAccess & packageAccessBit).isEqualTo(packageAccessBit);
        assertThat(accessOnlyDifferenceMask).isEqualTo(packageAccessBit);
        assertThat(nullAccessClearedMask).isEqualTo(packageAccessClearedMask);
        assertThat(withNullAccess).isNotZero();
    }

    @Test
    void encodeMemberDeclarationFlags_composedFromSegments_equalBitwiseOrOfSegments() {
        // Given
        MemberKind memberKind = MemberKind.METHOD;
        MemberAccess memberAccess = MemberAccess.PROTECTED;
        Set<DeclarationModifier> declarationModifiers =
                EnumSet.of(DeclarationModifier.ABSTRACT, DeclarationModifier.STRICTFP);
        int kindOnlyMask = encodeKindOnlyMask(memberKind);
        int accessOnlyMask = deriveAccessSegmentBit(memberAccess);
        int modifiersOnlyMask = deriveModifiersSegmentBitsMask(declarationModifiers);

        // When
        int compositeMask =
                MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(memberKind, memberAccess, declarationModifiers);

        // Then
        assertThat(compositeMask).isEqualTo(kindOnlyMask | accessOnlyMask | modifiersOnlyMask);
    }

    @Test
    void encodeMemberDeclarationFlags_memberAccessProvided_encodeOneHotAndZeroForNullAccess() {
        // Given
        int publicAccessBit = deriveAccessSegmentBit(MemberAccess.PUBLIC);
        int privateAccessBit = deriveAccessSegmentBit(MemberAccess.PRIVATE);
        int packageAccessBit = deriveAccessSegmentBit(MemberAccess.PACKAGE);
        int constructorKindOnlyMask = encodeKindOnlyMask(MemberKind.CONSTRUCTOR);
        int allAccessBitsMask = deriveAllMemberAccessBitsMask();

        // When
        int publicAndPrivateOverlap = publicAccessBit & privateAccessBit;
        int publicAndPackageOverlap = publicAccessBit & packageAccessBit;
        int privateAndPackageOverlap = privateAccessBit & packageAccessBit;
        int accessSegmentInKindOnlyMask = constructorKindOnlyMask & allAccessBitsMask;

        // Then
        assertThat(publicAccessBit).isNotZero();
        assertThat(privateAccessBit).isNotZero();
        assertThat(packageAccessBit).isNotZero();
        assertThat(publicAndPrivateOverlap).isZero();
        assertThat(publicAndPackageOverlap).isZero();
        assertThat(privateAndPackageOverlap).isZero();
        assertThat(accessSegmentInKindOnlyMask).isZero();
    }

    @Test
    void encodeMemberDeclarationFlags_memberKindsDiffer_encodeNonOverlappingBits() {
        // Given
        int methodKindMask = encodeKindOnlyMask(MemberKind.METHOD);
        int fieldKindMask = encodeKindOnlyMask(MemberKind.FIELD);
        int typeKindMask = encodeKindOnlyMask(MemberKind.TYPE_CLASS);

        // When
        int methodAndFieldOverlap = methodKindMask & fieldKindMask;
        int methodAndTypeOverlap = methodKindMask & typeKindMask;
        int fieldAndTypeOverlap = fieldKindMask & typeKindMask;
        int combinedMask = methodKindMask | fieldKindMask | typeKindMask;

        // Then
        assertThat(methodKindMask).isNotZero();
        assertThat(fieldKindMask).isNotZero();
        assertThat(typeKindMask).isNotZero();
        assertThat(methodAndFieldOverlap).isZero();
        assertThat(methodAndTypeOverlap).isZero();
        assertThat(fieldAndTypeOverlap).isZero();
        assertThat(combinedMask & methodKindMask).isEqualTo(methodKindMask);
        assertThat(combinedMask & fieldKindMask).isEqualTo(fieldKindMask);
        assertThat(combinedMask & typeKindMask).isEqualTo(typeKindMask);
    }

    @Test
    void encodeMemberDeclarationFlags_modifiersOrderDiffers_supportOrderIndependenceAndSubsetChecks() {
        // Given
        Set<DeclarationModifier> firstModifierSet = EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL);
        Set<DeclarationModifier> secondModifierSet = EnumSet.of(DeclarationModifier.FINAL, DeclarationModifier.STATIC);
        Set<DeclarationModifier> supersetModifierSet =
                EnumSet.of(DeclarationModifier.STATIC, DeclarationModifier.FINAL, DeclarationModifier.STRICTFP);

        // When
        int firstModifierMask = deriveModifiersSegmentBitsMask(firstModifierSet);
        int secondModifierMask = deriveModifiersSegmentBitsMask(secondModifierSet);
        int supersetModifierMask = deriveModifiersSegmentBitsMask(supersetModifierSet);
        int subsetIntersectionMask = supersetModifierMask & firstModifierMask;

        // Then
        assertThat(firstModifierMask).isEqualTo(secondModifierMask);
        assertThat(subsetIntersectionMask).isEqualTo(firstModifierMask);
    }

    @Test
    void encodeMemberDeclarationFlags_segmentsCompared_notOverlapAcrossKindAccessAndModifiers() {
        // Given
        int anyAccessBit = deriveAccessSegmentBit(MemberAccess.PUBLIC);
        int anyKindBit = encodeKindOnlyMask(MemberKind.TYPE_INTERFACE);
        int anyModifiersBit = deriveModifiersSegmentBitsMask(EnumSet.of(DeclarationModifier.STATIC));

        // When
        int accessAndKindOverlap = anyAccessBit & anyKindBit;
        int accessAndModifiersOverlap = anyAccessBit & anyModifiersBit;
        int kindAndModifiersOverlap = anyKindBit & anyModifiersBit;

        // Then
        assertThat(accessAndKindOverlap).as("access vs kind must not overlap").isZero();
        assertThat(accessAndModifiersOverlap)
                .as("access vs modifiers must not overlap")
                .isZero();
        assertThat(kindAndModifiersOverlap)
                .as("kind vs modifiers must not overlap")
                .isZero();
    }

    private static int deriveAccessSegmentBit(MemberAccess memberAccess) {
        int kindOnlyMask = encodeKindOnlyMask(MemberKind.FIELD);
        int encodedWithAccess = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                MemberKind.FIELD, memberAccess, EnumSet.noneOf(DeclarationModifier.class));
        return encodedWithAccess ^ kindOnlyMask;
    }

    private static int deriveAllMemberAccessBitsMask() {
        int combinedAccessBitsMask = 0;
        for (MemberAccess memberAccess : MemberAccess.values()) {
            combinedAccessBitsMask |= deriveAccessSegmentBit(memberAccess);
        }
        return combinedAccessBitsMask;
    }

    private static int deriveModifiersSegmentBitsMask(Set<DeclarationModifier> declarationModifiers) {
        int kindOnlyMask = encodeKindOnlyMask(MemberKind.FIELD);
        int encodedWithModifiers =
                MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(MemberKind.FIELD, null, declarationModifiers);
        return encodedWithModifiers ^ kindOnlyMask;
    }

    private static int encodeKindOnlyMask(MemberKind memberKind) {
        return MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                memberKind, null, EnumSet.noneOf(DeclarationModifier.class));
    }
}
