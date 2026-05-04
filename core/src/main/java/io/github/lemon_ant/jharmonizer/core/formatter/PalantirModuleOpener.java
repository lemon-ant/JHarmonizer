// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.formatter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Opens JDK compiler internal packages required by palantir-java-format programmatically.
 *
 * <p>palantir-java-format accesses {@code com.sun.tools.javac.*} internal classes via
 * reflection at runtime. When JHarmonizer runs as a Maven plugin (unnamed module), those
 * packages are not open by default, causing {@link IllegalAccessError}. This class opens
 * them programmatically via {@code sun.misc.Unsafe} — the same technique used by the
 * <a href="https://github.com/diffplug/spotless">Spotless</a> Maven plugin
 * ({@code com.diffplug.spotless.java.ModuleHelper}) — so users do not need to add
 * {@code --add-opens} flags to their JVM configuration.
 *
 * <p>Background: {@code java.lang} is <em>exported</em> by {@code java.base} but not
 * <em>opened</em>, so {@code setAccessible(true)} on {@code Module.implAddOpens} would
 * throw {@link java.lang.reflect.InaccessibleObjectException} on Java 17+. Obtaining
 * the trusted {@code IMPL_LOOKUP} via {@code sun.misc.Unsafe} (whose package
 * {@code sun.misc} is both exported and opened by {@code jdk.unsupported}) bypasses
 * that restriction and allows making {@code implAddOpens} effectively public.
 */
@Slf4j
@UtilityClass
class PalantirModuleOpener {

    private static final List<String> PACKAGES_TO_OPEN = List.of(
            "com.sun.tools.javac.api",
            "com.sun.tools.javac.parser",
            "com.sun.tools.javac.util",
            "com.sun.tools.javac.file",
            "com.sun.tools.javac.tree");

    private static final AtomicBoolean opened = new AtomicBoolean();

    /**
     * Programmatically opens all {@code jdk.compiler} internal packages required by
     * palantir-java-format to all unnamed modules. Idempotent and thread-safe.
     *
     * <p>On failure, logs a warning with the equivalent {@code --add-opens} flags
     * that can be added to {@code .mvn/jvm.config} as a manual fallback.
     */
    static void openRequiredJdkCompilerPackages() {
        if (opened.compareAndSet(false, true)) {
            applyPackageOpens();
        }
    }

    private static void applyPackageOpens() {
        try {
            Method implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class);
            makeMethodPublicViaTrustedLookup(implAddOpens);
            for (Module module : ModuleLayer.boot().modules()) {
                for (String packageName : PACKAGES_TO_OPEN) {
                    if (module.getPackages().contains(packageName)) {
                        implAddOpens.invoke(module, packageName);
                    }
                }
            }
        } catch (ReflectiveOperationException | IllegalStateException exception) {
            log.warn(
                    "Could not open jdk.compiler internals programmatically. "
                            + "If palantir-java-format fails with IllegalAccessError, "
                            + "add the following lines to .mvn/jvm.config in your project:\n"
                            + "--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED\n"
                            + "--add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED\n"
                            + "--add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED\n"
                            + "--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED\n"
                            + "--add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                    exception);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static void makeMethodPublicViaTrustedLookup(Method method) {
        try {
            // Access sun.misc.Unsafe via reflection to avoid a direct import of the internal API.
            // sun.misc is both exported and opened by jdk.unsupported, so setAccessible succeeds.
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            // Suppression is intentional: jdk.unsupported opens sun.misc, so this setAccessible
            // does not breach the module boundary — it is the documented way to obtain Unsafe.
            unsafeField.setAccessible(true); // NOPMD AvoidAccessibilityAlteration
            Object unsafe = unsafeField.get(null);

            // Obtain MethodHandles.Lookup.IMPL_LOOKUP via raw memory access using Unsafe,
            // bypassing the module-system access restrictions entirely.
            // IMPL_LOOKUP is a pre-existing trusted lookup inside the JDK itself.
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = unsafeClass.getMethod("staticFieldBase", Field.class).invoke(unsafe, implLookupField);
            Long offset = (Long)
                    unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, implLookupField);
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup)
                    unsafeClass.getMethod("getObject", Object.class, long.class).invoke(unsafe, base, offset);

            // Elevate the modifiers of the target method to PUBLIC so that plain
            // Method.invoke() succeeds without further access restrictions.
            MethodHandle modifiersSetter = trustedLookup.findSetter(Method.class, "modifiers", Integer.TYPE);
            modifiersSetter.invokeExact(method, Modifier.PUBLIC);
        } catch (Throwable throwable) {
            // MethodHandle.invokeExact() is declared throws Throwable; wrap it as unchecked
            // so the caller can use a narrower catch clause.
            throw new IllegalStateException(
                    "Could not obtain trusted MethodHandles.Lookup via sun.misc.Unsafe", throwable);
        }
    }
}
