package io.github.lemon_ant.jharmonizer.core.testutils;

import static java.util.Objects.requireNonNull;

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
 * Test-only utilities for reading test-case resources from the classpath.
 *
 * <p>We intentionally avoid resolving resources to {@link java.nio.file.Path} because resources may be packed into a
 * JAR or provided by a non-standard class loader in some runners.
 */
@UtilityClass
public class TestCaseResourceUtils {

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
        requireNonNull(resourceUrl, "resourceUrl cannot be null");
        try (InputStream inputStream = resourceUrl.openStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read resource URL: " + resourceUrl, ioException);
        }
    }
}
