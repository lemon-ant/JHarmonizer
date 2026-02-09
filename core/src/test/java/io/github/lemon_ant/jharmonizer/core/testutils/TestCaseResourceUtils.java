package io.github.lemon_ant.jharmonizer.core.testutils;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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

    public static String readClasspathResourceAsString(String absoluteClasspathResourcePath) {
        requireNonNull(absoluteClasspathResourcePath, "absoluteClasspathResourcePath cannot be null");
        if (!absoluteClasspathResourcePath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Expected an absolute classpath resource path starting with '/', but got: "
                            + absoluteClasspathResourcePath);
        }

        try (InputStream inputStream = requireNonNull(
                TestCaseResourceUtils.class.getResourceAsStream(absoluteClasspathResourcePath),
                "Missing test resource: " + absoluteClasspathResourcePath)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                    "Failed to read test resource from classpath: " + absoluteClasspathResourcePath, ioException);
        }
    }
}
