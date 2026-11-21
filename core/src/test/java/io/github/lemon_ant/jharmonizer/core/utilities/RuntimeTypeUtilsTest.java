// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.findJavaLangTopLevelType;
import static io.github.lemon_ant.jharmonizer.core.utilities.RuntimeTypeUtils.findPublicMemberTypeInHierarchy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RuntimeTypeUtilsTest {

    @ParameterizedTest
    @ValueSource(
            strings = {"MissingRuntimeType", "java.lang.Deprecated", "Thread.State", "Thread$State", "StringLatin1"})
    void findJavaLangTopLevelType_notPublicTopLevelName_returnsEmpty(@NonNull String simpleTypeName) {
        assertThat(findJavaLangTopLevelType(simpleTypeName)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideJavaLangTopLevelTypes")
    void findJavaLangTopLevelType_publicTopLevelName_returnsRuntimeDeclaration(
            @NonNull String simpleTypeName, @NonNull Class<?> expectedType) {
        assertThat(findJavaLangTopLevelType(simpleTypeName)).contains(expectedType);
    }

    @ParameterizedTest
    @MethodSource("provideUnavailableMemberTypes")
    void findPublicMemberTypeInHierarchy_absentNonPublicOrAmbiguousMember_returnsEmpty(
            @NonNull Class<?> ownerType, @NonNull String memberTypeName) {
        assertThat(findPublicMemberTypeInHierarchy(ownerType, memberTypeName)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("providePublicMemberTypes")
    void findPublicMemberTypeInHierarchy_uniquePublicMember_returnsActualDeclaration(
            @NonNull Class<?> ownerType, @NonNull String memberTypeName, @NonNull Class<?> expectedType) {
        assertThat(findPublicMemberTypeInHierarchy(ownerType, memberTypeName)).contains(expectedType);
    }

    @NonNull
    private static Stream<Arguments> provideJavaLangTopLevelTypes() {
        return Stream.of(
                Arguments.of("Deprecated", Deprecated.class),
                Arguments.of("String", String.class),
                Arguments.of("Thread", Thread.class));
    }

    @NonNull
    private static Stream<Arguments> providePublicMemberTypes() {
        return Stream.of(
                Arguments.of(Thread.class, "State", Thread.State.class),
                Arguments.of(ProcessBuilder.Redirect.class, "Type", ProcessBuilder.Redirect.Type.class),
                Arguments.of(TreeMap.class, "SimpleEntry", AbstractMap.SimpleEntry.class),
                Arguments.of(DirectOwner.class, "Tag", DirectOwner.Tag.class),
                Arguments.of(InheritedOwner.class, "Tag", DirectOwner.Tag.class),
                Arguments.of(ChildMemberTypes.class, "Tag", ChildMemberTypes.Tag.class),
                Arguments.of(DiamondOwner.class, "Tag", ParentMemberTypes.Tag.class),
                Arguments.of(PrivateAndPublicOwner.class, "Tag", ParentMemberTypes.Tag.class),
                Arguments.of(Dollar$Owner.class, "Tag$Kind", Dollar$Owner.Tag$Kind.class));
    }

    @NonNull
    private static Stream<Arguments> provideUnavailableMemberTypes() {
        return Stream.of(
                Arguments.of(String.class, "MissingMemberType"),
                Arguments.of(PrivateOwner.class, "Tag"),
                Arguments.of(ProtectedOwner.class, "Tag"),
                Arguments.of(PackageOwner.class, "Tag"),
                Arguments.of(NonPublicAmbiguousOwner.class, "Tag"),
                Arguments.of(CrossBranchClassOwner.class, "Tag"),
                Arguments.of(CrossBranchInterfaceOwner.class, "Tag"));
    }

    public interface ChildMemberTypes extends ParentMemberTypes {

        @interface Tag {}
    }

    public static class CrossBranchClassOwner extends DirectOwner implements ParentMemberTypes {}

    public interface CrossBranchInterfaceOwner extends ChildMemberTypes, ParentMemberTypes {}

    public static class DiamondOwner implements LeftMemberTypes, RightMemberTypes {}

    public static class DirectOwner implements ParentMemberTypes {

        public @interface Tag {}
    }

    public static class Dollar$Owner {

        public @interface Tag$Kind {}
    }

    public static class InheritedOwner extends DirectOwner {}

    public interface LeftMemberTypes extends ParentMemberTypes {}

    public static class NonPublicAmbiguousOwner extends ProtectedOwner implements ParentMemberTypes {}

    public static class PackageOwner extends DirectOwner {

        @interface Tag {}
    }

    public interface ParentMemberTypes {

        @interface Tag {}
    }

    public static class PrivateAndPublicOwner extends PrivateOwner implements ParentMemberTypes {}

    public static class PrivateOwner extends DirectOwner {

        private @interface Tag {}
    }

    public static class ProtectedOwner extends DirectOwner {

        protected @interface Tag {}
    }

    public interface RightMemberTypes extends ParentMemberTypes {}
}
