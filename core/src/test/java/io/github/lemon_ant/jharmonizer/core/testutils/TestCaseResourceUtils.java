package io.github.lemon_ant.jharmonizer.core.testutils;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;

/**
 * Test-only utilities for reading test-case resources from the classpath.
 *
 * <p>We intentionally avoid resolving resources to {@link java.nio.file.Path} because resources may be packed into a
 * JAR or provided by a non-standard class loader in some runners.
 */
@UtilityClass
public class TestCaseResourceUtils {

    public static URL resolveRelativeUrl(URL directoryResource, String relativePathSegment) {
        requireNonNull(directoryResource, "directoryResource cannot be null");
        requireNonNull(relativePathSegment, "relativePathSegment cannot be null");
        String directoryUrlString = directoryResource.toString();
        if (!directoryUrlString.endsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected a directory URL ending with '/', but got: " + directoryResource);
        }
        try {
            return new URL(directoryResource, relativePathSegment);
        } catch (MalformedURLException malformedURLException) {
            throw new IllegalArgumentException(
                    "Failed to resolve relative URL segment '%s' under directory URL: %s"
                            .formatted(relativePathSegment, directoryResource),
                    malformedURLException);
        }
    }

    public static String readClasspathResourceAsString(URL resourceUrl) {
        requireNonNull(resourceUrl, "resourceUrl cannot be null");
        try (InputStream inputStream = resourceUrl.openStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read resource URL: " + resourceUrl, ioException);
        }
    }
}
