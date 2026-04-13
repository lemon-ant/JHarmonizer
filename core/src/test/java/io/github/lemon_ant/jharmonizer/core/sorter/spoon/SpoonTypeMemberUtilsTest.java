package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class SpoonTypeMemberUtilsTest {

    private static final URL MIXED_MEMBERS_URL = TestCaseResourceUtils.requireClasspathResourceUrl("/" + TEST_CASES_DIR
            + "/core/sorter/spoon/member-descriptor/valid/ClassWithFieldsMethodsInitBlocksAndNestedTypes.java");
    private static final URL RECORD_URL = TestCaseResourceUtils.requireClasspathResourceUrl("/" + TEST_CASES_DIR
            + "/core/sorter/spoon/member-descriptor/valid/RecordWithExplicitMethodAndNoImplicitMembers.java");
    private static final URL ENUM_URL = TestCaseResourceUtils.requireClasspathResourceUrl("/" + TEST_CASES_DIR
            + "/core/sorter/spoon/member-descriptor/valid/EnumWithConstantsAndRegularMembers.java");

    private static final CtType<?> MIXED_MEMBERS_TYPE =
            SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(MIXED_MEMBERS_URL);

    @Nested
    class DeriveVisibilityRank {

        @Test
        void deriveVisibilityRank_publicMember_returnsZero() {
            // Given
            CtTypeMember publicField = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "PUBLIC_STATIC_FINAL_FIELD");

            // When
            int rank = SpoonTypeMemberUtils.deriveVisibilityRank(publicField);

            // Then
            assertThat(rank).isEqualTo(0);
        }

        @Test
        void deriveVisibilityRank_protectedMember_returnsOne() {
            // Given
            CtTypeMember protectedField = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "protectedField");

            // When
            int rank = SpoonTypeMemberUtils.deriveVisibilityRank(protectedField);

            // Then
            assertThat(rank).isEqualTo(1);
        }

        @Test
        void deriveVisibilityRank_packagePrivateMember_returnsTwo() {
            // Given
            CtTypeMember packageField = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "packageField");

            // When
            int rank = SpoonTypeMemberUtils.deriveVisibilityRank(packageField);

            // Then
            assertThat(rank).isEqualTo(2);
        }

        @Test
        void deriveVisibilityRank_privateMember_returnsThree() {
            // Given
            CtTypeMember privateMethod = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "privateStaticMethod");

            // When
            int rank = SpoonTypeMemberUtils.deriveVisibilityRank(privateMethod);

            // Then
            assertThat(rank).isEqualTo(3);
        }

        @Test
        void deriveVisibilityRank_anonymousExecutable_returnsThree() {
            // Given
            CtAnonymousExecutable staticInitBlock = MIXED_MEMBERS_TYPE.getTypeMembers().stream()
                    .filter(member -> member instanceof CtAnonymousExecutable)
                    .map(member -> (CtAnonymousExecutable) member)
                    .findFirst()
                    .orElseThrow();

            // When
            int rank = SpoonTypeMemberUtils.deriveVisibilityRank(staticInitBlock);

            // Then
            assertThat(rank).isEqualTo(3);
        }
    }

    @Nested
    class DeriveAlphaSortingRank {

        @Test
        void deriveAlphaSortingRank_anonymousExecutable_returnsOne() {
            // Given
            CtAnonymousExecutable initBlock = MIXED_MEMBERS_TYPE.getTypeMembers().stream()
                    .filter(member -> member instanceof CtAnonymousExecutable)
                    .map(member -> (CtAnonymousExecutable) member)
                    .findFirst()
                    .orElseThrow();

            // When
            int rank = SpoonTypeMemberUtils.deriveAlphaSortingRank(initBlock);

            // Then
            assertThat(rank).isEqualTo(1);
        }

        @Test
        void deriveAlphaSortingRank_regularField_returnsZero() {
            // Given
            CtTypeMember field = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "packageField");

            // When
            int rank = SpoonTypeMemberUtils.deriveAlphaSortingRank(field);

            // Then
            assertThat(rank).isEqualTo(0);
        }
    }

    @Nested
    class DeriveAlphaKey {

        @Test
        void deriveAlphaKey_method_containsNameAndParameters() {
            // Given
            CtTypeMember method = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "privateStaticMethod");

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(method);

            // Then
            assertThat(alphaKey).contains("privateStaticMethod").contains("()");
        }

        @Test
        void deriveAlphaKey_field_containsNameAndType() {
            // Given
            CtTypeMember field = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "protectedField");

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(field);

            // Then
            assertThat(alphaKey).contains("protectedField");
        }

        @Test
        void deriveAlphaKey_staticInitBlock_returnsClinitKey() {
            // Given
            CtAnonymousExecutable staticInitBlock = MIXED_MEMBERS_TYPE.getTypeMembers().stream()
                    .filter(member -> member instanceof CtAnonymousExecutable)
                    .map(member -> (CtAnonymousExecutable) member)
                    .filter(member -> member.getModifiers().contains(spoon.reflect.declaration.ModifierKind.STATIC))
                    .findFirst()
                    .orElseThrow();

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(staticInitBlock);

            // Then
            assertThat(alphaKey).isEqualTo("<clinit>");
        }

        @Test
        void deriveAlphaKey_instanceInitBlock_returnsInitKey() {
            // Given
            CtAnonymousExecutable instanceInitBlock = MIXED_MEMBERS_TYPE.getTypeMembers().stream()
                    .filter(member -> member instanceof CtAnonymousExecutable)
                    .map(member -> (CtAnonymousExecutable) member)
                    .filter(member -> !member.getModifiers().contains(spoon.reflect.declaration.ModifierKind.STATIC))
                    .findFirst()
                    .orElseThrow();

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(instanceInitBlock);

            // Then
            assertThat(alphaKey).isEqualTo("<init>");
        }

        @Test
        void deriveAlphaKey_constructor_containsInitPrefix() {
            // Given
            CtConstructor<?> constructor = MIXED_MEMBERS_TYPE.getTypeMembers().stream()
                    .filter(member -> member instanceof CtConstructor)
                    .map(member -> (CtConstructor<?>) member)
                    .findFirst()
                    .orElseThrow();

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(constructor);

            // Then
            assertThat(alphaKey).startsWith("<init>");
        }

        @Test
        void deriveAlphaKey_recordComponent_containsComponentName() {
            // Given
            CtType<?> recordType = SpoonTestCaseUtils.parseMainTypeFromJavaFixtureResource(RECORD_URL);
            CtTypeMember nestedRecord = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "NestedRecord");
            CtType<?> nestedRecordType = (CtType<?>) nestedRecord;
            CtTypeMember recordComponent = nestedRecordType.getTypeMembers().stream()
                    .filter(member -> member instanceof spoon.reflect.declaration.CtRecordComponent)
                    .findFirst()
                    .orElseThrow();

            // When
            String alphaKey = SpoonTypeMemberUtils.deriveAlphaKey(recordComponent);

            // Then
            assertThat(alphaKey).contains("component");
        }
    }

    @Nested
    class ExtractSrcStart {

        @Test
        void extractSrcStart_validPosition_returnsSourceStart() {
            // Given
            CtTypeMember field = SpoonTestCaseUtils.requireTypeMemberBySimpleName(
                    MIXED_MEMBERS_TYPE.getTypeMembers(), "packageField");

            // When
            int srcStart = SpoonTypeMemberUtils.extractSrcStart(field);

            // Then
            assertThat(srcStart).isPositive();
        }
    }

    @Nested
    class StreamExplicitSrcTypeMembers {

        @Test
        void streamExplicitSrcTypeMembers_classWithMixedMembers_doesNotIncludeImplicitMembers() {
            // When
            List<CtTypeMember> explicitMembers = SpoonTypeMemberUtils.streamExplicitSrcTypeMembers(MIXED_MEMBERS_TYPE)
                    .toList();

            // Then
            assertThat(explicitMembers).isNotEmpty();
            assertThat(explicitMembers)
                    .allMatch(member -> member instanceof CtField
                            || member instanceof CtMethod
                            || member instanceof CtConstructor
                            || member instanceof CtAnonymousExecutable
                            || member instanceof CtType);
        }
    }
}
