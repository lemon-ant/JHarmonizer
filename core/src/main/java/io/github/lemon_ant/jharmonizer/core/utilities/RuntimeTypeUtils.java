// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Inspects actual runtime types without initializing them or consulting application source declarations. */
@UtilityClass
public class RuntimeTypeUtils {

    @NonNull
    private static final String CLASS_FILE_SUFFIX = ".class";

    @NonNull
    private static final String JAVA_LANG_RESOURCE_PREFIX = "java/lang/";

    @NonNull
    private static final Map<String, Class<?>> JAVA_LANG_TOP_LEVEL_TYPES_BY_SIMPLE_NAME =
            collectJavaLangTopLevelTypesBySimpleName();

    /**
     * Finds a public top-level type implicitly imported from the running JDK's {@code java.lang} package.
     * Builds the immutable platform index once, without initializing the indexed classes.
     * @param simpleTypeName one Java type identifier, including any literal '$' characters
     * @return the runtime type, or empty when the name does not identify a public top-level java.lang type
     */
    @NonNull
    public static Optional<Class<?>> findJavaLangTopLevelType(@NonNull String simpleTypeName) {
        return Optional.ofNullable(JAVA_LANG_TOP_LEVEL_TYPES_BY_SIMPLE_NAME.get(simpleTypeName));
    }

    /**
     * Finds a uniquely named public member type, including inherited members of classes and interfaces.
     * Direct declarations hide their ancestors even when the declarations themselves are not public.
     * @param ownerType runtime type whose members are searched
     * @param memberTypeName simple name of the member type
     * @return the unique public member declaration, or empty for absence, ambiguity, or a non-public selection
     */
    @NonNull
    public static Optional<Class<?>> findPublicMemberType(@NonNull Class<?> ownerType, @NonNull String memberTypeName) {
        Set<Class<?>> memberDeclarations = collectMemberTypeDeclarations(ownerType, memberTypeName);
        return memberDeclarations.size() == 1
                ? memberDeclarations.stream()
                        .filter(memberDeclaration -> Modifier.isPublic(memberDeclaration.getModifiers()))
                        .findFirst()
                : Optional.empty();
    }

    /** Enumerates only direct java.lang class resources, then validates their actual runtime nesting and access. */
    @NonNull
    private static Map<String, Class<?>> collectJavaLangTopLevelTypesBySimpleName() {
        Module javaBaseModule = Object.class.getModule();
        ModuleReference javaBaseModuleReference = ModuleLayer.boot()
                .configuration()
                .findModule(javaBaseModule.getName())
                .orElseThrow(() -> new IllegalStateException("The boot layer does not contain java.base"))
                .reference();
        try (ModuleReader moduleReader = javaBaseModuleReference.open();
                Stream<String> moduleResources = moduleReader.list()) {
            return moduleResources
                    .filter(RuntimeTypeUtils::isJavaLangClassResource)
                    .map(resourceName -> readModuleType(javaBaseModule, resourceName))
                    // '$' is a legal identifier character: inspect nesting instead of filtering resource names.
                    .filter(runtimeType ->
                            runtimeType.getEnclosingClass() == null && Modifier.isPublic(runtimeType.getModifiers()))
                    .collect(Collectors.toUnmodifiableMap(Class::getSimpleName, runtimeType -> runtimeType));
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot index public java.lang runtime types", ioException);
        }
    }

    /** Retains non-public inherited selections until ambiguity is checked by the caller. */
    @NonNull
    private static Set<Class<?>> collectMemberTypeDeclarations(Class<?> ownerType, String memberTypeName) {
        Optional<Class<?>> directDeclaration = Stream.of(ownerType.getDeclaredClasses())
                .filter(memberDeclaration -> memberTypeName.equals(memberDeclaration.getSimpleName()))
                .findFirst();
        if (directDeclaration.isPresent()) {
            // A private Owner.Tag also hides its ancestors; do not continue through Owner's parents.
            return Set.of(directDeclaration.orElseThrow());
        }
        return Stream.concat(Stream.ofNullable(ownerType.getSuperclass()), Stream.of(ownerType.getInterfaces()))
                .flatMap(parentType -> collectMemberTypeDeclarations(parentType, memberTypeName).stream())
                .filter(memberDeclaration -> isInheritedBy(ownerType, memberDeclaration))
                // The same declaration reached through both sides of a diamond is one member, not ambiguity.
                // Different declarations remain ambiguous even if their owners have a subtype relationship.
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Checks inheritance at every edge so package-private members cannot reappear after crossing a package. */
    private static boolean isInheritedBy(Class<?> inheritingType, Class<?> memberDeclaration) {
        int memberModifiers = memberDeclaration.getModifiers();
        if (Modifier.isPrivate(memberModifiers)) {
            return false;
        }
        Class<?> declaringType = memberDeclaration.getDeclaringClass();
        return Modifier.isPublic(memberModifiers)
                || Modifier.isProtected(memberModifiers)
                || declaringType.getModule().equals(inheritingType.getModule())
                        && declaringType.getPackageName().equals(inheritingType.getPackageName());
    }

    /** Excludes subpackages while retaining names such as java/lang/Dollar$Type.class. */
    private static boolean isJavaLangClassResource(String resourceName) {
        return resourceName.startsWith(JAVA_LANG_RESOURCE_PREFIX)
                && resourceName.endsWith(CLASS_FILE_SUFFIX)
                && resourceName.indexOf('/', JAVA_LANG_RESOURCE_PREFIX.length()) < 0;
    }

    /** Loads a listed java.base resource by its binary name without initializing the class. */
    @NonNull
    private static Class<?> readModuleType(Module runtimeModule, String resourceName) {
        String binaryTypeName = resourceName
                .substring(0, resourceName.length() - CLASS_FILE_SUFFIX.length())
                .replace('/', '.');
        Class<?> runtimeType = Class.forName(runtimeModule, binaryTypeName);
        if (runtimeType == null) {
            throw new IllegalStateException("No runtime type for module resource " + resourceName);
        }
        return runtimeType;
    }
}
