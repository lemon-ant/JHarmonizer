// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Shared helpers for unit-testing JHarmonizer Maven mojos.
 * Provides reflection-based field injection (matching Maven's own injection mechanism)
 * and classpath-resource copying utilities.
 */
@UtilityClass
class MojoTestUtils {

    /**
     * Injects a value into a declared field of the given object, searching up the class hierarchy.
     * Uses reflection to replicate Maven's own parameter injection mechanism.
     *
     * @param target    the object to inject into
     * @param fieldName the name of the field to set
     * @param value     the value to assign
     * @throws IllegalStateException if the field cannot be found or set
     */
    static void injectField(@NonNull Object target, @NonNull String fieldName, Object value) {
        Class<?> searchClass = target.getClass();
        while (searchClass != null) {
            try {
                Field declaredField = searchClass.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                declaredField.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                searchClass = searchClass.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot inject field '" + fieldName + "' in "
                                + target.getClass().getName(),
                        e);
            }
        }
        throw new IllegalStateException("Field '" + fieldName + "' not found in class hierarchy of "
                + target.getClass().getName());
    }

    /**
     * Copies all classpath resources from the given resource directory into a target directory,
     * preserving the file names.
     *
     * @param resourceDirectory the classpath directory path (must start with {@code /})
     * @param targetDirectory   the directory to copy files into
     * @throws UncheckedIOException if a resource cannot be read or written
     * @throws IllegalArgumentException if the resource directory cannot be found
     */
    static void copyResourceDirectory(@NonNull String resourceDirectory, @NonNull Path targetDirectory) {
        URL directoryUrl = MojoTestUtils.class.getResource(resourceDirectory);
        if (directoryUrl == null) {
            throw new IllegalArgumentException("Classpath resource directory not found: " + resourceDirectory);
        }
        try {
            Files.createDirectories(targetDirectory);
            Path directoryPath = Path.of(directoryUrl.toURI());
            try (Stream<Path> entries = Files.list(directoryPath)) {
                entries.filter(Files::isRegularFile).forEach(srcFile -> {
                    Path targetFile =
                            targetDirectory.resolve(srcFile.getFileName().toString());
                    copyFile(srcFile, targetFile);
                });
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot convert resource URL to path: " + directoryUrl, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy resource directory: " + resourceDirectory, e);
        }
    }

    /**
     * Reads the content of a classpath resource as a UTF-8 string.
     *
     * @param resourcePath the classpath path (must start with {@code /})
     * @return the resource content
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException     if reading fails
     */
    @NonNull
    static String readResourceAsString(@NonNull String resourcePath) {
        try (InputStream inputStream = MojoTestUtils.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read classpath resource: " + resourcePath, e);
        }
    }

    /**
     * Copies a single classpath resource to a target file path and returns the target path.
     *
     * @param resourcePath the classpath path (must start with {@code /})
     * @param targetFile   the destination file path
     * @return the target file path for chaining
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException     if the copy fails
     */
    @NonNull
    static Path extractResourceToTemp(@NonNull String resourcePath, @NonNull Path targetFile) {
        try (InputStream inputStream = MojoTestUtils.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
            }
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract resource " + resourcePath, e);
        }
    }

    /**
     * Converts a {@link Path} to a {@link File} for use as a mojo {@code baseDir} parameter value.
     *
     * @param path the path to convert
     * @return the corresponding file
     */
    @NonNull
    static File toFile(@NonNull Path path) {
        return path.toFile();
    }

    private static void copyFile(Path srcFile, Path targetFile) {
        try {
            Files.copy(srcFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy " + srcFile + " to " + targetFile, e);
        }
    }
}
