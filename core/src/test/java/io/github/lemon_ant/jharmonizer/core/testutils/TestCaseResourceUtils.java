/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Test-only utilities for working with resources from the classpath.
 *
 * <p>We intentionally avoid resolving resources to {@link java.nio.file.Path} because resources may be packed into a
 * JAR or provided by a non-standard class loader in some runners.
 */
@UtilityClass
public class TestCaseResourceUtils {

    /** Root directory name for all test-case fixtures under {@code src/test/resources/}. */
    public static final String TEST_CASES_DIR = "test-cases";

    @NonNull
    public static URL requireClasspathResourceUrl(@NonNull String classpathAbsolutePath) {
        if (!classpathAbsolutePath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected an absolute classpath resource path starting with '/', but got: "
                            + classpathAbsolutePath);
        }

        URL resolvedUrl = TestCaseResourceUtils.class.getResource(classpathAbsolutePath);
        if (resolvedUrl == null) {
            throw new IllegalStateException("Classpath resource not found: " + classpathAbsolutePath);
        }

        return resolvedUrl;
    }

    @NonNull
    public static URL requireClasspathDirectoryUrl(@NonNull String classpathAbsoluteDirectoryPath) {
        if (!classpathAbsoluteDirectoryPath.endsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected an absolute classpath directory path ending with '/', but got: "
                            + classpathAbsoluteDirectoryPath);
        }

        URL resolvedUrl = requireClasspathResourceUrl(classpathAbsoluteDirectoryPath);
        if (!resolvedUrl.toExternalForm().endsWith("/")) {
            throw new IllegalStateException("Classpath directory URL must end with '/', but got: " + resolvedUrl);
        }

        return resolvedUrl;
    }

    @NonNull
    public static InputStream openClasspathResourceStream(@NonNull String classpathAbsolutePath) {
        URL resourceUrl = requireClasspathResourceUrl(classpathAbsolutePath);
        try {
            return resourceUrl.openStream();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to open resource URL: " + resourceUrl, ioException);
        }
    }

    @NonNull
    @Deprecated
    public static InputStream openClasspathResourceStream(
            @NonNull Class<?> anchorClass, @NonNull String classpathAbsolutePath) {
        if (!classpathAbsolutePath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected an absolute classpath resource path starting with '/', but got: "
                            + classpathAbsolutePath);
        }

        InputStream inputStream = anchorClass.getResourceAsStream(classpathAbsolutePath);
        if (inputStream == null) {
            throw new IllegalStateException("Classpath resource not found: " + classpathAbsolutePath);
        }

        return inputStream;
    }

    @NonNull
    public static URL resolveRelativeUrl(@NonNull URL directoryResourceUrl, @NonNull String relativePathSegment) {
        String directoryExternalForm = directoryResourceUrl.toExternalForm();
        if (!directoryExternalForm.endsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected a directory URL ending with '/', but got: " + directoryResourceUrl);
        }

        if (relativePathSegment.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected a relative path segment (must not start with '/'), but got: " + relativePathSegment);
        }

        try {
            return directoryResourceUrl.toURI().resolve(relativePathSegment).toURL();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL cannot be converted to URI: " + directoryResourceUrl, exception);
        } catch (IllegalArgumentException exception) {
            // Thrown by URI.resolve(...) if relativePathSegment is not a valid URI reference.
            throw new IllegalArgumentException(
                    "Invalid relative path segment '%s' for directory URL: %s"
                            .formatted(relativePathSegment, directoryResourceUrl),
                    exception);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(
                    "Failed to convert resolved URI to URL for segment '%s' under directory URL: %s"
                            .formatted(relativePathSegment, directoryResourceUrl),
                    exception);
        }
    }

    @NonNull
    public static String readClasspathResourceAsString(@NonNull URL resourceUrl) {
        try (InputStream inputStream = resourceUrl.openStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read resource URL: " + resourceUrl, ioException);
        }
    }

    @NonNull
    public static String readClasspathResourceAsString(@NonNull String classpathAbsolutePath) {
        URL resourceUrl = requireClasspathResourceUrl(classpathAbsolutePath);
        return readClasspathResourceAsString(resourceUrl);
    }
}
